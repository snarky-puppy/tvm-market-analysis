package com.tvm;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Created by horse on 6/10/2016.
 */
public class ResultsWindow {
    private static final Logger logger = LogManager.getLogger(ResultsWindow.class);
    private ResultTableModel resultTableModel;

    JPanel panel;
    private JTable resultsTable;
    private JButton button1;
    private JProgressBar progressBar;

    private int totalTasks;
    private int doneTasks;

    public ResultsWindow() {
        resultTableModel = new ResultTableModel();
        resultsTable.setModel(resultTableModel);
        resultsTable.setAutoCreateRowSorter(true);

        progressBar.setVisible(false);
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
    }

    public void runCalculation(ConfigBean bean) {
        logger.info("Starting Calculation");
        Map<String, Path> files = FileFinder.getFiles();

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
        private ConfigBean bean;

        public CalculationTask(File file, ConfigBean bean) {
            this.file = file;
            this.bean = bean;
        }

        private double change(double start, double end) {
            return ((end - start)/start);
        }

        @Override
        protected List<Result> doInBackground() throws Exception {
            String symbol = file.getName().replace(".csv", "");
            List<Result> rv = new ArrayList<>();
            try {
                //logger.info("processing "+file.getName());

                Data data = new FileDatabase().loadData(file);

                if(data == null)
                    return null;

                double[] c = data.close;

                int maxP = Collections.max(bean.pointDistances);

                int idx = 0;
                // maxP days is how many we need for the slope calc and dollar volume.
                // if we trigger with less than the hold time left then we can still report it.
                while(idx + maxP - 1 < data.close.length && idx + bean.daysDolVol - 1 < data.close.length) {
                    double p[] = new double[bean.pointDistances.size()];
                    SimpleRegression simpleRegression = new SimpleRegression();

                    int i = 0;
                    for(int d : bean.pointDistances) {
                        p[i++] = change(c[idx], c[idx + d-1]);
                        simpleRegression.addData(i, p[i-1]);
                    }

                    double slope = simpleRegression.getSlope();

                    if(slope <= bean.slopeCutoff) {
                        //double avgVolume = new Mean().evaluate(data.volume, idx, 21);
                        OptionalDouble optionalDouble = Arrays.stream(data.volume, idx, idx + bean.daysDolVol).average();
                        if(optionalDouble.isPresent()) {
                            double avgVolume = optionalDouble.getAsDouble();
                            double avgClose = new Mean().evaluate(data.close, idx, bean.daysDolVol);
                            double dollarVolume = avgClose * avgVolume;
                            if (dollarVolume >= bean.minDolVol) {
                                Result r = new Result();
                                r.symbol = symbol;
                                r.slope = slope;
                                r.dollarVolume = dollarVolume;
                                if(idx+1 < data.open.length) {
                                    r.entryDate = data.date[idx + 1];
                                    r.entryOpen = data.open[idx + 1];

                                    BigDecimal targetPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.targetPc))).setScale(4, BigDecimal.ROUND_HALF_UP);
                                    BigDecimal stopPrice = BigDecimal.valueOf(r.entryOpen + ((r.entryOpen/100)*(bean.stopPc))).setScale(4, BigDecimal.ROUND_HALF_UP);;

                                    for(i = 2; i < bean.maxHoldDays+2; i++) {
                                        if(idx+i >= data.close.length) {
                                            r.exitReason = "END DATA";
                                            break;
                                        }
                                        if(targetPrice.compareTo(BigDecimal.valueOf(data.close[idx+i])) >= 0) {
                                            if(idx+i+1 < data.close.length) {
                                                r.exitDate = data.date[idx + i + 1];
                                                r.exitOpen = data.open[idx + i + 1];
                                                r.exitReason = "TARGET";
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.open[idx + i];
                                                r.exitReason = "TARGET (EOD)";
                                            }
                                            break;
                                        }
                                        if(stopPrice.compareTo(BigDecimal.valueOf(data.close[idx+i])) <= 0) {
                                            if(idx+i+1 < data.close.length) {
                                                r.exitDate = data.date[idx + i + 1];
                                                r.exitOpen = data.open[idx + i + 1];
                                                r.exitReason = "STOP";
                                            } else {
                                                r.exitDate = data.date[idx + i];
                                                r.exitOpen = data.open[idx + i];
                                                r.exitReason = "STOP (EOD)";
                                            }
                                            break;
                                        }
                                    }

                                    if(r.exitReason == null) {
                                        r.exitDate = data.date[idx + i];
                                        r.exitOpen = data.open[idx + i];
                                        r.exitReason = "MAX HOLD";
                                    }

                                } else {
                                    r.exitReason = "NO ENTRY DATA";
                                }
                                publish(r);
                                rv.add(r);
                            }
                        }
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
        public double entryOpen;
        public int exitDate;
        public double exitOpen;
        public String exitReason;

        public double slope;
        public double dollarVolume;
    }

    public class ResultTableModel extends AbstractTableModel {

        final ColumnDef[] columnDefs = new ColumnDef[] {
                new ColumnDef("Symbol", String.class),
                new ColumnDef("Entry Date", Integer.class),
                new ColumnDef("Entry Open", Double.class),

                new ColumnDef("Exit Date", Integer.class),
                new ColumnDef("Exit Open", Double.class),
                new ColumnDef("Exit Reason", String.class),
                new ColumnDef("Profit %", Double.class),

                new ColumnDef("Slope", Double.class),
                new ColumnDef("Dollar Volume", Double.class),
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
                case 3:                return data.get(rowIndex).exitDate;
                case 4:                return data.get(rowIndex).exitOpen;
                case 5:                return data.get(rowIndex).exitReason;
                case 6:                return calcProfit(rowIndex);
                case 7:                return data.get(rowIndex).slope;
                case 8:                return data.get(rowIndex).dollarVolume;

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

}
