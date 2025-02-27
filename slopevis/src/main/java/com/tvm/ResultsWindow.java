package com.tvm;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Created by horse on 6/10/2016.
 */
public class ResultsWindow {
    private static final Logger logger = LogManager.getLogger(ResultsWindow.class);
    private ResultTableModel resultTableModel;

    JPanel panel;
    private JTable resultsTable;
    private JButton debugButton;
    private JProgressBar progressBar;
    private JButton exportButton;

    private int totalTasks;
    private int doneTasks;

    private Map<String, StringBuilder> debugLog;

    public ResultsWindow() {
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

        exportButton.addActionListener(l -> {
            File f = null;
            try {
                f = File.createTempFile("SlopeVisTemp", ".csv");

                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(resultTableModel.header());
                bw.write("\n");
                for (Result r : resultTableModel.getResults()) {
                    bw.write(r.toString());
                    bw.write("\n");
                }
                bw.flush();
                bw.close();

                f.deleteOnExit();

                Desktop.getDesktop().open(f);
            } catch (IOException e) {
                logger.error("Could not write file", e);
                e.printStackTrace();
            }

        });
    }

    public void runCalculation(ConfigBean bean) {
        logger.info("Starting Calculation");
        Map<String, Path> files = FileFinder.getFiles();

        debugButton.setEnabled(bean.debug);

        debugLog = new HashMap<>();

        resultTableModel = new ResultTableModel();
        resultsTable.setModel(resultTableModel);
        doneTasks = 0;
        totalTasks = 0;
        progressBar.setValue(0);
        progressBar.setVisible(true);

        for(String key : files.keySet()) {
            Path p = files.get(key);
            if(p != null) {
                File f = p.toFile();
                CalculationTask task = new CalculationTask(f, bean);
                task.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("progress".equals(evt.getPropertyName())) {
                            progressBar.setValue((Integer)evt.getNewValue());
                        }
                    }
                });
                task.execute();
                totalTasks ++;
            }
        }
    }

    private class CalculationTask extends SwingWorker<List<Result>, Result> {

        private final File file;
        private final String symbol;
        private ConfigBean bean;

        public CalculationTask(File file, ConfigBean bean) {
            this.file = file;
            this.bean = bean;
            symbol = file.getName().replace(".csv", "");
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

                Data data = new FileDatabase().loadData(file, bean);

                if(data == null)
                    return null;

                double[] c = data.close;

                int maxP = Collections.max(bean.pointDistances) - 1;

                //int idx = maxP - bean.daysDolVol;
                int idx = 0;

                // maxP days is how many we need for the slope calc and dollar volume.
                // if we trigger with less than the hold time left then we can still report it.
                while(idx < data.close.length &&
                        idx + maxP < data.close.length) {
                    double p[] = new double[bean.pointDistances.size()];
                    SimpleRegression simpleRegression = new SimpleRegression();

                    {
                        int i = 0;
                        for (int d : bean.pointDistances) {
                            p[i++] = change(c[idx], c[idx + d - 1]);
                            debug("date=,%d,d=,%d,close=,%.2f,close+d=,%.2f,x=,%d,y=,%.2f", data.date[idx], d, c[idx], c[idx + d - 1], i, p[i - 1]);
                            simpleRegression.addData(i, p[i - 1]);
                        }
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

                                BigDecimal dvRounded = BigDecimal.valueOf(dollarVolume).setScale(2, BigDecimal.ROUND_HALF_UP);
                                r.dollarVolume = dvRounded.doubleValue();

                                // calculate liquidity
                                try {
                                    OptionalDouble liqDbl = Arrays.stream(data.volume, (idx + maxP) - bean.daysLiqVol, idx + maxP).average();
                                    double liqAvgVolume = liqDbl.getAsDouble();
                                    double liqAvgClose = new Mean().evaluate(data.close, (idx + maxP) - bean.daysLiqVol, bean.daysLiqVol);
                                    double liquidity = liqAvgClose * liqAvgVolume;
                                    debug("liquidity,avgVol=,%.2f,avgClose=,%.2f,dolVol=,%.2f", liqAvgVolume, liqAvgClose, liquidity);
                                    BigDecimal liqRounded = BigDecimal.valueOf(liquidity).setScale(2, BigDecimal.ROUND_HALF_UP);
                                    r.liquidity = liqRounded.doubleValue();
                                } catch(ArrayIndexOutOfBoundsException e) {
                                    debug("liquidity,not enough data");
                                }

                                if(idx+bean.tradeStartDays < data.open.length) {
                                    r.entryDate = data.date[idx + bean.tradeStartDays];
                                    r.entryPrevClose = data.close[idx + bean.tradeStartDays - 1];
                                    r.entryOpen = data.open[idx + bean.tradeStartDays];
                                    r.entryLow = data.low[idx + bean.tradeStartDays];

                                    BigDecimal targetPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.targetPc))).setScale(2, BigDecimal.ROUND_HALF_UP);
                                    BigDecimal stopPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.stopPc))).setScale(2, BigDecimal.ROUND_HALF_UP);

                                    r.target = targetPrice.toString();

                                    debug("entryDate=,%d,entryOpen=,%.2f,targetPrice=,%.2f,stopPrice=,%.2f", r.entryDate, r.entryOpen, targetPrice.doubleValue(), stopPrice.doubleValue());

                                    int i = 0;
                                    for(i = bean.tradeStartDays + 1; i <= bean.tradeStartDays+bean.maxHoldDays; i++) {
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
                                                r.profitPc = ((r.exitOpen-r.entryOpen)/r.entryOpen)*100;
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.close[idx + i];
                                                r.exitReason = "TARGET (No Next Day Data - showing close of target date)";
                                                debug("find target,TARGET FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                                r.profitPc = ((r.exitOpen-r.entryOpen)/r.entryOpen)*100;
                                            }
                                            break;
                                        }
                                        if(close.compareTo(stopPrice) <= 0) {
                                            if(idx+i+1 < data.close.length) {
                                                r.exitDate = data.date[idx + i + 1];
                                                r.exitOpen = data.open[idx + i + 1];
                                                r.exitReason = "STOP";
                                                debug("find target,STOP FOUND,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                                r.profitPc = ((r.exitOpen-r.entryOpen)/r.entryOpen)*100;
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.open[idx + i];
                                                r.exitReason = "STOP (No Next Day Data - showing close)";
                                                debug("find target,STOP FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                                r.profitPc = ((r.exitOpen-r.entryOpen)/r.entryOpen)*100;
                                            }
                                            break;
                                        }
                                    }

                                    // edge case: data is exactly as long as idx + tradeStartDays + maxHoldDays
                                    if(idx + i == data.date.length) {
                                        r.exitReason = "END DATA";
                                    }

                                    // we exceeded the max hold period
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
                logger.info("Calculation completed: {} symbols processed", doneTasks);
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
        public String symbol;
        public int entryDate;
        public double entryPrevClose;
        public double entryOpen;
        public double entryLow;

        public String target;
        public int exitDate;
        public double exitOpen;
        public String exitReason;

        public double profitPc;

        public double slope;
        public double dollarVolume;

        public double liquidity;


        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(symbol);
            sb.append(", ").append(entryDate);
            sb.append(",").append(entryPrevClose);
            sb.append(",").append(entryOpen);
            sb.append(",").append(entryLow);
            sb.append(",").append(target);

            sb.append(",").append(exitDate);
            sb.append(",").append(exitOpen);
            sb.append(",").append(exitReason);
            sb.append(",").append(profitPc);

            sb.append(",").append(slope);
            sb.append(",").append(dollarVolume);

            sb.append(",").append(liquidity);

            return sb.toString();
        }
    }

    public class ResultTableModel extends AbstractTableModel {

        final ColumnDef[] columnDefs = new ColumnDef[] {
                new ColumnDef("Symbol", String.class),
                new ColumnDef("Entry Date", Integer.class),
                new ColumnDef("Prev Entry Close", Double.class),
                new ColumnDef("Entry Open", Double.class),
                new ColumnDef("Entry Low", Double.class),

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
                case 2:                return data.get(rowIndex).entryPrevClose;
                case 3:                return data.get(rowIndex).entryOpen;
                case 4:                return data.get(rowIndex).entryLow;
                case 5:                return data.get(rowIndex).target;
                case 6:                return data.get(rowIndex).exitDate;
                case 7:                return data.get(rowIndex).exitOpen;
                case 8:                return data.get(rowIndex).exitReason;
                case 9:                return calcProfit(rowIndex);
                case 10:                return data.get(rowIndex).slope;
                case 11:                return data.get(rowIndex).dollarVolume;
                case 12:               return data.get(rowIndex).liquidity;

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

        public String header() {
            StringBuilder builder = new StringBuilder();
            for(ColumnDef c : columnDefs) {
                builder.append(c.name);
                builder.append(",");
            }
            return builder.toString();
        }

        public List<Result> getResults() {
            return data;
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

        File f = null;
        try {
            f = File.createTempFile("SlopeVis", ".csv");

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
