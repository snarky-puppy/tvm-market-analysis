package com.tvm.stg;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by matt on 10/02/17.
 */
public class DataCruncher {
    private static final Logger logger = LogManager.getLogger(DataCruncher.class);
    private final DataCruncherMonitor monitor;
    ExecutorService executorService = Executors.newFixedThreadPool(8);
    List<SimulationBean> simulationBeans = new ArrayList<>();
    private final ArrayBlockingQueue<ResultSet> queue = new ArrayBlockingQueue<ResultSet>(1024);
    private boolean finalised = false;
    private Thread writerThread = new Thread(this::resultWriter);


    static class SimulationBean {
        public double minDolVol;
        public int daysDolVol;
        public double slopeCutoff;
        public double maxHoldDays;

        public int daysLiqVol;

        public double stopPc;
        public double targetPc;
        public int tradeStartDays;

        public List<Integer> pointDistances = new ArrayList<>();
        public double investPc;
        public int investSpread;
    }

    class Result {
        public String symbol;
        public int entryDate;
        public double entryOpen;
        public int exitDate;
        public double exitOpen;
        public ExitReason exitReason;

        public double slope;
        public double dollarVolume;
        public String target;
        public double liquidity;
    }

    class ResultSet {
        private SimulationBean simulationBean;
        private List<Result> results = new ArrayList<>();

        public ResultSet(SimulationBean simulationBean) {
            this.simulationBean = simulationBean;
        }

        public SimulationBean getSimulationBean() {
            return simulationBean;
        }

        public List<Result> getResults() {
            return results;
        }
    }

    public DataCruncher(ConfigBean bean, DataCruncherMonitor monitor) {
        this.monitor = monitor;

        logger.info("Calculating slope params");

        List<List<Integer>> points = bean.getPointRanges();
        logger.info("{} point combinations (out of {} inputs)", points.size(), bean.pointDistances.size());

        for (List<Integer> pointSet : points) {
            for (double targetPc : bean.targetPc.permute()) {
                for (double stopPc : bean.stopPc.permute()) {
                    for (int maxHoldDays : bean.maxHoldDays.permute()) {
                        for (double slopeCutoff : bean.slopeCutoff.permute()) {
                            for (int daysDolVol : bean.daysDolVol.permute()) {
                                for (double minDolVol : bean.minDolVol.permute()) {
                                    for (int tradeStartDays : bean.tradeStartDays.permute()) {
                                        for (int daysLiqVol : bean.daysLiqVol.permute()) {
                                            // compounder params
                                            for (double investPc : bean.investPercent.permute()) {
                                                for (int investSpread : bean.investSpread.permute()) {
                                                    SimulationBean simulationBean = new SimulationBean();
                                                    simulationBean.stopPc = stopPc;
                                                    simulationBean.pointDistances = pointSet;
                                                    simulationBean.targetPc = targetPc;
                                                    simulationBean.maxHoldDays = maxHoldDays;
                                                    simulationBean.slopeCutoff = slopeCutoff;
                                                    simulationBean.daysDolVol = daysDolVol;
                                                    simulationBean.minDolVol = minDolVol;
                                                    simulationBean.tradeStartDays = tradeStartDays;
                                                    simulationBean.daysLiqVol = daysLiqVol;
                                                    simulationBean.investPc = investPc;
                                                    simulationBean.investSpread = investSpread;
                                                    simulationBeans.add(simulationBean);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        logger.info("{} parameter combinations calculated", simulationBeans.size());

        Map<String, Path> files = FileFinder.getFiles();

        monitor.setNumCrunchJobs(files.size() * simulationBeans.size());

        writerThread.start();

        for (String key : files.keySet()) {
            Path p = files.get(key);

            Data data = new FileDatabase().loadData(p.toFile());

            if (data == null)
                continue;

            for (SimulationBean simulationBean : simulationBeans) {
                executorService.execute(() -> runCrunch(simulationBean, data));
            }
        }
    }



    private void runCrunch(SimulationBean simulationBean, Data data) {
        if(finalised)
            return;
        ResultSet resultSet = new ResultSet(simulationBean);
        crunchSlope(simulationBean, data, resultSet.results);
        if(finalised)
            return;
        crunchCompound(resultSet.results);
        if(finalised)
            return;
        enqueueResultSet(resultSet);
    }

    private void crunchCompound(List<Result> results) {

    }

    private void debug(String fmt, Object ...args) { /* debugSymbol(symbol, new Formatter().format(fmt, args).toString()); */ }

    private double change(double start, double end) {
        return ((end - start)/start);
    }

    private void crunchSlope(SimulationBean bean, Data data, List<Result> results) {
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
                            r.symbol = data.symbol;
                            r.slope = slope;
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
                                        r.exitReason = ExitReason.END_DATA;
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
                                            r.exitReason = ExitReason.STOP_TARGET;
                                            debug("find target,TARGET FOUND,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                        } else {
                                            r.exitDate = data.date[idx + i];
                                            r.exitOpen = data.close[idx + i];
                                            r.exitReason = ExitReason.STOP_TARGET; //"TARGET (No Next Day Data - showing close of target date)";
                                            debug("find target,TARGET FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                        }
                                        break;
                                    }
                                    if(close.compareTo(stopPrice) <= 0) {
                                        if(idx+i+1 < data.close.length) {
                                            r.exitDate = data.date[idx + i + 1];
                                            r.exitOpen = data.open[idx + i + 1];
                                            r.exitReason = ExitReason.STOP_PRICE; // "STOP";
                                            debug("find target,STOP FOUND,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                        } else {
                                            r.exitDate = data.date[idx + i];
                                            r.exitOpen = data.open[idx + i];
                                            r.exitReason = ExitReason.STOP_PRICE;// "STOP (No Next Day Data - showing close)";
                                            debug("find target,STOP FOUND but next day no data so showing found date,exitDate=,%d,exitOpen=,%.2f", r.exitDate, r.exitOpen);
                                        }
                                        break;
                                    }
                                }

                                if(r.exitReason == null) {
                                    r.exitDate = data.date[idx + i];
                                    r.exitOpen = data.open[idx + i];
                                    r.exitReason = ExitReason.STOP_MAX_HOLD;
                                    debug("exitReason=,MAX HOLD");
                                }

                            } else {
                                r.exitReason = ExitReason.OUT_OF_DATA; //"NO TRADE DAY DATA";
                                debug("exitReason=,NO TRADE DAY DATA (trade day starts after our data ends),max=,%d", data.date[data.date.length-1]);
                            }
                            results.add(r);
                        }
                    } else
                        debug("volume, not enough data");
                }
                idx++;
            }
        } catch(Exception ex) {
            logger.error("Uncaught exception", ex);
        } finally {
            monitor.jobFinished();
        }
    }

    protected void enqueueResultSet(ResultSet resultSet) {
        boolean accept = false;
        do {
            accept = queue.offer(resultSet);
            try {
                if(!accept)
                    Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!accept);
    }

    public void finalise() {
        finalised = true;
        try {
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void resultWriter() {
        try {
            ResultSet r;
            do {
                r = queue.poll(1000, TimeUnit.MILLISECONDS);
                if (r != null) {
                    try {
                        writeResults(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e);
                        System.exit(1);
                    }
                }
            } while (r != null || !finalised);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeResults(ResultSet r) throws IOException {

    }

}
