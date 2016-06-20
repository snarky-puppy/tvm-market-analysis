package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.EMAException;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Design9.1 - news
 * Created by horse on 16/05/2016.
 */
public class Strategy4 {

    private static final Logger logger = LogManager.getLogger(Strategy4.class);

    private static Random random = new Random();

    private final static Map<String, Database> exchangeDB = new HashMap<>();
    private final static Map<String, CloseData> symbolData = new HashMap<>();

    // k=category
    private static final Map<String, ArrayBlockingQueue<Result>> queues = new HashMap<>();
    private static final List<ResultWriter> writerThreads = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(4); // only 4 markets

            final boolean isTest = false;

            if(isTest) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        Strategy4 strategy4 = new Strategy4();
                        strategy4.run("test");
                    }
                });

            } else {
                for (final String market : Conf.listAllMarkets()) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            Strategy4 strategy4 = new Strategy4();
                            strategy4.run(market);
                        }
                    });
                }
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            stopAllWriterThreads();
        }
    }

    class NewsRow {
        public int date;
        public String symbol;
        public String news;
    }

    class Result {
        public String symbol;
        public String exchange;
        // dates
        public int d0;
        public int d7;
        public int d14;
        public int d21;
        // profit/loss
        public double pl7;
        public double pl14;
        public double pl21;
        public double slope;
        public double dollarVolume;
        
        public int holdPl28Date;
        public double holdPl28Price;
        public double holdPl28Pc;

        public int holdPl35Date;
        public double holdPl35Price;
        public double holdPl35Pc;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(symbol);
            sb.append(",").append(exchange);
            sb.append(",").append(d0);
            sb.append(",").append(d7);
            sb.append(",").append(d14);
            sb.append(",").append(d21);
            sb.append(",").append(pl7);
            sb.append(",").append(pl14);
            sb.append(",").append(pl21);
            sb.append(",").append(slope);
            sb.append(",").append(String.format("%.8f", dollarVolume));
            sb.append(",").append(holdPl28Date);
            sb.append(",").append(holdPl28Price);
            sb.append(",").append(holdPl28Pc);
            sb.append(",").append(holdPl35Date);
            sb.append(",").append(holdPl35Price);
            sb.append(",").append(holdPl35Pc);
            sb.append('\n');
            return sb.toString();
        }
    }

    static private class ResultWriter extends Thread {

        private final BlockingQueue<Result> blockingQueue;
        private boolean finalised = false;
        private BufferedWriter writer;
        private boolean header = false;


        ResultWriter(String category, BlockingQueue<Result> blockingQueue) {
            try {
                this.blockingQueue = blockingQueue;
                writer = new BufferedWriter(new FileWriter("strategy4-"+category+".csv"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        
        public void finalise() {
            finalised = true;
        }

        @Override
        public void run() {
            try {
                Result r = null;
                do {
                    r = blockingQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (r != null) {
                        try {
                            writeResults(r);
                        } catch (IOException e) {
                            logger.error(e);
                            finalised = true;
                        }
                    } else
                        logger.error("Nothing in queue");

                } while (r != null || finalised == false);

            } catch (InterruptedException e) {

            }
        }

        private void writeResults(Result r) throws IOException {
            if(!header) {
                writeHeader();
                header = true;
            }
            writer.write(r.toString());
        }

        private void writeHeader() throws IOException {
            StringBuilder bw = new StringBuilder();
            bw.append("Symbol,");
            bw.append("Exchange,");
            bw.append("Date0,");
            bw.append("Date7,");
            bw.append("Date14,");
            bw.append("Date21,");

            bw.append("P1,");
            bw.append("P2,");
            bw.append("P3,");
            bw.append("Slope,");
            bw.append("Dollar Volume,");

            
            bw.append("Hold 28 Date,");
            bw.append("Hold 28 Price,");
            bw.append("Hold 28 Pc,");

            bw.append("Hold 35 Date,");
            bw.append("Hold 35 Price,");
            bw.append("Hold 35 Pc,");


            bw.append('\n');

            writer.write(bw.toString());
        }

        public void close() {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private void run(String market) {

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        try {
            ArrayBlockingQueue<Result> queue = getQueue(market);
            Database db = DatabaseFactory.createDatabase(market);
            for (String symbol : db.listSymbols()) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        processSymbol(db, queue, symbol);
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private void processSymbol(Database db, ArrayBlockingQueue<Result> queue, String symbol) {
        try {
            CloseData data = db.loadData(symbol);
            double[] c = data.close;

            // gather high from previous 99 days
            int idx = 0;
            // 21 days is how many we need for the slope calc and dollar volume.
            // if we trigger with less than the hold time left then we can still report it.
            while(idx + 21 - 1 < data.close.length) {
                double p1, p2, p3;

                p1 = ((c[idx] - c[idx + 7-1])/c[idx+7-1])*100;
                p2 = ((c[idx] - c[idx + 14-1])/c[idx+14-1])*100;
                p3 = ((c[idx] - c[idx + 21-1])/c[idx+21-1])*100;

                SimpleRegression simpleRegression = new SimpleRegression();

                simpleRegression.addData(1, p1);
                simpleRegression.addData(2, p2);
                simpleRegression.addData(3, p3);

                double slope = simpleRegression.getSlope();

                if(slope <= -0.2) {
                    double avgVolume = new Mean().evaluate(data.volume, idx, 21);
                    double avgClose = new Mean().evaluate(data.close, idx, 21);
                    if(avgClose * avgVolume >= 10000000) {
                        Result r = new Result();
                        r.symbol = symbol;
                        r.exchange = db.getMarket();

                        r.d0 = data.date[idx];
                        r.d7 = data.date[idx+7-1];
                        r.d14 = data.date[idx+14-1];
                        r.d21 = data.date[idx+21-1];

                        r.pl7 = p1;
                        r.pl14 = p2;
                        r.pl21 = p3;
                        r.slope = slope;
                        r.dollarVolume = avgVolume * avgClose;


                        int i = idx + 21  + 28 - 1;
                        if(i < data.open.length) {
                            r.holdPl28Date = data.date[i];
                            r.holdPl28Pc = ((data.open[idx + 21] - data.open[i])/data.open[i])*100;
                            r.holdPl28Price = data.open[i];
                        }

                        i = idx + 21 + 35 - 1;
                        if(i < data.open.length) {
                            r.holdPl35Date = data.date[i];
                            r.holdPl35Pc = ((data.open[idx + 21] - data.open[i])/data.open[i])*100;
                            r.holdPl35Price = data.open[i];
                        }
                        enqueueResult(queue, r);
                    }
                }
                idx++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public double ema(double[] close, int startIdx, int days) throws EMAException {
        if(days <= 1) {
            //throw new EMAException("not enough days for meaningful EMA: "+days);
            return -1;
        }
        if(days > startIdx + 1) {
            //throw new EMAException("not enough data to calculate "+days+"day EMA");
            return -1;
        }

        double ema = 0.0;

        // start with SMA
        double prevDaysEMA = simpleMovingAverage(close, startIdx, days);

        // multiplier
        double k = (2 / (days + 1));

        for(int idx = startIdx - days + 1; idx <= startIdx; idx++) {
            ema = close[idx] * k + prevDaysEMA * (1 - k);
            prevDaysEMA = ema;
        }

        return ema;
    }

    private double simpleMovingAverage(double[] data, int startIdx, int days) {
        double sum = 0.0;
        for (int i = startIdx - days + 1; i <= startIdx ; i++) {
            sum += data[i];
        }
        return sum / days;
    }


    private void enqueueResult(ArrayBlockingQueue<Result> queue, Result result) {
        boolean accept = false;
        do {
            accept = queue.offer(result);
            try {
                if(!accept)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        } while (!accept);
    }

    private ArrayBlockingQueue<Result> getQueue(String market) {
        ArrayBlockingQueue<Result> queue = null;

        synchronized (queues) {
            if(!queues.containsKey(market)) {
                queue = new ArrayBlockingQueue<Result>(1024);
                ResultWriter resultWriter = new ResultWriter(market, queue);
                writerThreads.add(resultWriter);
                queues.put(market, queue);
                resultWriter.start();
            } else
                queue = queues.get(market);
        }
        return queue;
    }

    private static void stopAllWriterThreads() {
        for(ResultWriter resultWriter : writerThreads) {
            try {
                resultWriter.finalise();
                resultWriter.join();
                resultWriter.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
