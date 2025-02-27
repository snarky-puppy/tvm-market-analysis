package com.tvm.stg;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Created by matt on 2/02/17.
 */
public class SlopeResultsWindow {

    private static final Logger logger = LogManager.getLogger(SlopeResultsWindow.class);
    private ResultTableModel resultTableModel;


    JPanel panel;
    private JTable resultsTable;
    private JButton debugButton;
    private JProgressBar progressBar;

    private int totalTasks;
    private int doneTasks;
    List<CalculationTask> taskList = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();

    private Map<String, StringBuilder> debugLog;

    public SlopeResultsWindow() {
        resultTableModel = new ResultTableModel();
        resultsTable.setModel(resultTableModel);
        resultsTable.setAutoCreateRowSorter(true);

        progressBar.setVisible(false);
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);

        debugButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == debugButton) {
                    openResults();
                }
            }
        });
    }

    static class SlopeBean {
        public double minDolVol;
        public int daysDolVol;
        public double slopeCutoff;
        public double maxHoldDays;

        public int daysLiqVol;

        public double stopPc;
        public double targetPc;
        public int tradeStartDays;

        public List<Integer> pointDistances = new ArrayList<>();
    }

    public void runCalculation(ConfigBean bean) {

        if(taskList.size() > 0) {
            logger.info("Cancelling previous calculation");
            for(CalculationTask task : taskList) {
                task.cancel(true);
            }
            taskList.clear();
        }

        if(stopWatch.isStarted()) {
            stopWatch.stop();
            stopWatch.reset();
        }

        logger.info("Starting Calculation");
        Map<String, Path> files = FileFinder.getFiles();

        //debugButton.setEnabled(bean.debug);

        debugLog = new HashMap<>();

        resultTableModel = new ResultTableModel();
        resultsTable.setModel(resultTableModel);
        doneTasks = 0;
        totalTasks = 0;
        progressBar.setValue(0);
        progressBar.setVisible(true);

        logger.info("Calculating slope params");

        List<SlopeBean> slopeBeans = new ArrayList<>();
        List<List<Integer>> points = bean.getPointRanges();
        logger.info("{} point combinations (out of {} inputs)", points.size(), bean.pointDistances.size());


        for(List<Integer> pointSet : points) {
            for(double targetPc : bean.targetPc.permute()) {
                for(double stopPc : bean.stopPc.permute()) {
                    for(int maxHoldDays : bean.maxHoldDays.permute()) {
                        for(double slopeCutoff : bean.slopeCutoff.permute()) {
                            for(int daysDolVol : bean.daysDolVol.permute()) {
                                for(double minDolVol : bean.minDolVol.permute()) {
                                    for(int tradeStartDays : bean.tradeStartDays.permute()) {
                                        for(int daysLiqVol : bean.daysLiqVol.permute()) {
                                            SlopeBean slopeBean = new SlopeBean();
                                            slopeBean.stopPc = stopPc;
                                            slopeBean.pointDistances = pointSet;
                                            slopeBean.targetPc = targetPc;
                                            slopeBean.maxHoldDays = maxHoldDays;
                                            slopeBean.slopeCutoff = slopeCutoff;
                                            slopeBean.daysDolVol = daysDolVol;
                                            slopeBean.minDolVol = minDolVol;
                                            slopeBean.tradeStartDays = tradeStartDays;
                                            slopeBean.daysLiqVol = daysLiqVol;
                                            slopeBeans.add(slopeBean);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        logger.info("{} slope params calculated", slopeBeans.size());



        for(String key : files.keySet()) {
            Path p = files.get(key);

            Data data = new FileDatabase().loadData(p.toFile());

            if(data == null)
                continue;

            for(SlopeBean slopeBean : slopeBeans) {
                CalculationTask task = new CalculationTask(p.toFile().getName().replace(".csv", ""), data, slopeBean);
                task.addPropertyChangeListener(evt -> {
                    if ("progress".equals(evt.getPropertyName())) {
                        progressBar.setValue((Integer) evt.getNewValue());
                    }
                });
                taskList.add(task);
                totalTasks ++;
            }
        }

        logger.info("Ready with {} tasks", totalTasks);
        stopWatch.start();
        for(CalculationTask task : taskList) {
            task.execute();
        }

    }

    private class CalculationTask extends SwingWorker<List<Result>, Result> {

        private final String symbol;
        private final Data data;
        private SlopeBean bean;

        public CalculationTask(String symbol, Data data, SlopeBean bean) {
            this.symbol = symbol;

            this.data = data;
            this.bean = bean;
        }

        private void debug(String fmt, Object ...args) { debugSymbol(symbol, new Formatter().format(fmt, args).toString()); }

        private double change(double start, double end) {
            return ((end - start)/start);
        }

        @Override
        protected List<Result> doInBackground() throws Exception {
            List<Result> rv = new ArrayList<>();
            try {
                //logger.info("Processing "+file.getName());

                double[] c = data.close;

                int maxP = Collections.max(bean.pointDistances);

                //int idx = maxP - bean.daysDolVol;
                int idx = 0;

                // maxP days is how many we need for the slope calc and dollar volume.
                // if we trigger with less than the hold time left then we can still report it.
                while(idx < data.close.length &&
                        idx + maxP < data.close.length) {
                    double p[] = new double[bean.pointDistances.size()];
                    SimpleRegression simpleRegression = new SimpleRegression();

                    int i = 0;
                    for(int d : bean.pointDistances) {
                        p[i++] = change(c[idx], c[idx + d-1]);
                        debug("date=,%d,d=,%d,close=,%.2f,close+d=,%.2f,x=,%d,y=,%.2f", data.date[idx], d, c[idx], c[idx+d-1], i, p[i-1]);
                        simpleRegression.addData(i, p[i-1]);
                    }

                    double slope = simpleRegression.getSlope();
                    debug("slope,%.2f,%s", slope, slope <= bean.slopeCutoff ? "yes" : "no");

                    if(slope <= bean.slopeCutoff) {
                        //double avgVolume = new Mean().evaluate(data.volume, idx, 21);
                        OptionalDouble optionalDouble = OptionalDouble.empty();
                        try {
                            optionalDouble = Arrays.stream(data.volume, (idx + maxP) - bean.daysDolVol, idx + maxP).average();
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }

                        if(optionalDouble.isPresent()) {
                            double avgVolume = optionalDouble.getAsDouble();
                            double avgClose = new Mean().evaluate(data.close, (idx + maxP) - bean.daysDolVol, bean.daysDolVol);
                            double dollarVolume = avgClose * avgVolume;
                            debug("volume,avgVol=,%.2f,avgClose=,%.2f,dolVol=,%.2f,%s", avgVolume, avgClose, dollarVolume, dollarVolume >= bean.minDolVol ? "yes": "no");
                            if (dollarVolume >= bean.minDolVol) {
                                Result r = new Result();
                                r.symbol = symbol;
                                r.slope = slope;
                                r.slopeBean = bean;
                                r.dollarVolume = dollarVolume;

                                // calculate liquidity
                                try {
                                    OptionalDouble liqDbl = Arrays.stream(data.volume, (idx + maxP) - bean.daysLiqVol, idx + maxP).average();
                                    double liqAvgVolume = liqDbl.getAsDouble();
                                    double liqAvgClose = new Mean().evaluate(data.close, (idx + maxP) - bean.daysLiqVol, bean.daysLiqVol);
                                    double liquidity = liqAvgClose * liqAvgVolume;
                                    debug("liquidity,avgVol=,%.2f,avgClose=,%.2f,dolVol=,%.2f", liqAvgVolume, liqAvgClose, liquidity);
                                    r.liquidity = liquidity;
                                } catch(ArrayIndexOutOfBoundsException e) {
                                    debug("liquidity,not enough data");
                                }

                                if(idx+bean.tradeStartDays < data.open.length) {
                                    r.entryDate = data.date[idx + bean.tradeStartDays];
                                    r.entryOpen = data.open[idx + bean.tradeStartDays];

                                    BigDecimal targetPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.targetPc))).setScale(4, BigDecimal.ROUND_HALF_UP);
                                    BigDecimal stopPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.stopPc))).setScale(4, BigDecimal.ROUND_HALF_UP);;

                                    r.target = targetPrice.toString();

                                    debug("entryDate=,%d,entryOpen=,%.2f,targetPrice=,%.2f,stopPrice=,%.2f", r.entryDate, r.entryOpen, targetPrice.doubleValue(), stopPrice.doubleValue());

                                    for(i = bean.tradeStartDays + 1; i < bean.tradeStartDays+bean.maxHoldDays+1; i++) {
                                        if(idx+i >= data.close.length) {
                                            r.exitReason = "END DATA";
                                            debug("exitReason=,END DATA (run out of data before found target/stop/maxHold),max=,%d",
                                                    data.date[data.date.length-1]);
                                            break;
                                        }

                                        BigDecimal close = BigDecimal.valueOf(data.close[idx+i]).setScale(4, BigDecimal.ROUND_HALF_UP);

                                        debug("find target,date=,%d,close=,%.2f,isTarget=%s,isStop=%s", data.date[idx+i], data.close[idx+1],
                                                close.compareTo(targetPrice) >= 0,
                                                close.compareTo(stopPrice) <= 0);

                                        if(close.compareTo(targetPrice) >= 0) {
                                            if(idx+i+1 < data.close.length) {
                                                r.exitDate = data.date[idx + i + 1];
                                                r.exitOpen = data.open[idx + i + 1];
                                                r.exitReason = "TARGET";
                                                debug("find target,TARGET FOUND,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.close[idx + i];
                                                r.exitReason = "TARGET (No Next Day Data - showing close of target date)";
                                                debug("find target,TARGET FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                            }
                                            break;
                                        }
                                        if(close.compareTo(stopPrice) <= 0) {
                                            if(idx+i+1 < data.close.length) {
                                                r.exitDate = data.date[idx + i + 1];
                                                r.exitOpen = data.open[idx + i + 1];
                                                r.exitReason = "STOP";
                                                debug("find target,STOP FOUND,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.open[idx + i];
                                                r.exitReason = "STOP (No Next Day Data - showing close)";
                                                debug("find target,STOP FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                            }
                                            break;
                                        }
                                    }

                                    if(r.exitReason == null) {
                                        r.exitDate = data.date[idx + i];
                                        r.exitOpen = data.open[idx + i];
                                        r.exitReason = "MAX HOLD";
                                        debug("exitReason=,MAX HOLD");
                                    }

                                } else {
                                    r.exitReason = "NO TRADE DAY DATA";
                                    debug("exitReason=,NO TRADE DAY DATA (trade day starts after our data ends),max=,%d", data.date[data.date.length-1]);
                                }
                                publish(r);
                                rv.add(r);
                            }
                        } else
                            debug("volume, not enough data");
                    }
                    idx++;
                }
            } catch(Exception ex) {
                logger.error("Uncaught exception", ex);
            }
            return rv;
        }

        @Override
        protected void done() {
            doneTasks++;
            if(doneTasks == totalTasks) {
                stopWatch.stop();
                logger.info("Calculation completed: {} simulations processed in {}", doneTasks, stopWatch.toString());
                stopWatch.reset();
                progressBar.setVisible(false);
                resultTableModel.fireTableDataChanged();
            } else {
                Double d = ((float)doneTasks / totalTasks) * 100.00;
                int progress = d.intValue();
                progressBar.setValue(progress);
            }
            super.done();
        }


        @Override
        protected void process(List<Result> chunks) {
            for (Result r : chunks) {
                resultTableModel.addResult(r);
            }
        }
    }

    class ColumnDef {
        public String name;
        public Class<?> type;

        public ColumnDef(String symbol, Class<?> klass) {
            name = symbol;
            type = klass;
        }
    }

    class Result {
        public SlopeBean slopeBean;

        public String symbol;
        public int entryDate;
        public double entryOpen;
        public int exitDate;
        public double exitOpen;
        public String exitReason;

        public double slope;
        public double dollarVolume;
        public String target;
        public double liquidity;
    }

    public class ResultTableModel extends AbstractTableModel {

        final ColumnDef[] columnDefs = new ColumnDef[] {
                new ColumnDef("Symbol", String.class),
                new ColumnDef("Entry Date", Integer.class),
                new ColumnDef("Entry Open", Double.class),

                new ColumnDef("Target", String.class),
                new ColumnDef("Exit Date", Integer.class),
                new ColumnDef("Exit Open", Double.class),
                new ColumnDef("Exit Reason", String.class),
                new ColumnDef("Profit %", Double.class),

                new ColumnDef("Slope", Double.class),
                new ColumnDef("Dollar Volume", Double.class),
                new ColumnDef("Liquidity", Double.class),
        };

        private ArrayList<Result> data = new ArrayList<>();

        public void clearResults() {
            data.clear();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnDefs.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnDefs[columnIndex].name;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnDefs[columnIndex].type;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(rowIndex > data.size() - 1)
                return null;
            switch(columnIndex) {
                case 0:                return data.get(rowIndex).symbol;
                case 1:                return data.get(rowIndex).entryDate;
                case 2:                return data.get(rowIndex).entryOpen;
                case 3:                return data.get(rowIndex).target;
                case 4:                return data.get(rowIndex).exitDate;
                case 5:                return data.get(rowIndex).exitOpen;
                case 6:                return data.get(rowIndex).exitReason;
                case 7:                return calcProfit(rowIndex);
                case 8:                return data.get(rowIndex).slope;
                case 9:                return data.get(rowIndex).dollarVolume;
                case 10:               return data.get(rowIndex).liquidity;

                default:
                    logger.error(String.format("Invalid column index: r=%d c=%d", rowIndex, columnIndex));
                    break;
            }
            return null;
        }

        private double calcProfit(int rowIndex) {
            double entry = data.get(rowIndex).entryOpen;
            double exit = data.get(rowIndex).exitOpen;
            if(entry == 0 || exit == 0)
                return 0.0;
            return ((exit-entry)/entry)*100;
        }

        public void addResult(Result r) {
            if(r.entryDate != 0)
                data.add(r);
        }
    }

    void debugSymbol(String symbol, String line) {
        if(debugButton.isEnabled()) {
            if(!debugLog.containsKey(symbol)) {
                debugLog.put(symbol, new StringBuilder());
            }
            StringBuilder sb = debugLog.get(symbol);
            sb.append(symbol);
            sb.append(',');
            sb.append(line);
            sb.append('\n');
        }
    }

    public void openResults() {

        File f;
        try {
            f = File.createTempFile("SlopeDebug", ".csv");

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));

            for(String key : debugLog.keySet()) {
                StringBuilder sb = debugLog.get(key);
                bw.write(sb.toString());
                bw.write("------\n");
            }
            bw.flush();
            bw.close();

            f.deleteOnExit();

            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            logger.error("Could not write file", e);
            e.printStackTrace();
        }


    }

}
