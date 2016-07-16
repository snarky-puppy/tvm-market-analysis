package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.EMAException;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.ws.Response;
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
public class TrendContinuationStrategy4 {

    private static final Logger logger = LogManager.getLogger(TrendContinuationStrategy4.class);

    private static Random random = new Random();

    // k=market
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
                        TrendContinuationStrategy4 trendContinuationStrategy4 = new TrendContinuationStrategy4();
                        trendContinuationStrategy4.run("test");
                    }
                });

            } else {
                for (final String market : Conf.listAllMarkets()) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            TrendContinuationStrategy4 trendContinuationStrategy4 = new TrendContinuationStrategy4();
                            trendContinuationStrategy4.run(market);
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



    class Result {

        public int date;
        public String symbol;
        public String exchange;
        public String category;
        public double prev30AvgPrice;
        public double prev30AvgVol;
        public double price;
        public double zScore30Day;

        public int nextDayOpenDate;
        public double nextDayOpenPrice;

        public double ema30;
        public double ema50;
        public double ema100;

        public double vma20;
        public double vma50;

        public static final int weekRange = 8;
        public Integer[] weekDate = new Integer[weekRange];
        public Double[] weekPrice = new Double[weekRange];

        // 3 - 24
        public static final int monthRange = 8;
        public Integer[] monthDate = new Integer[monthRange];
        public Double[] monthPrice = new Double[monthRange];

        // 3, 6, 9, 12
        public static final int lowMonthRange = 4;
        public Integer[] lowMonthDate = new Integer[lowMonthRange];
        public Double[] lowMonthPrice = new Double[lowMonthRange];

        // 3 - 36
        public static final int maxMonthRange = 12; // (36/3);
        public Integer[] maxMonthDate = new Integer[maxMonthRange];
        public Double[] maxMonthPrice = new Double[maxMonthRange];

        // 10,20,30,40,50 % price target
        public static final int priceTargetRange = 5;
        public Integer[] priceTargetDate = new Integer[priceTargetRange];
        public Double[] priceTargetPrice = new Double[priceTargetRange];

        public Integer lastRecordedDate;
        public Double lastRecordedPrice;

        public Integer endOfYearDate;
        public Double endOfYearPrice;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("");
            sb.append(date);
            sb.append(",\"").append(symbol).append("\"");
            sb.append(",\"").append(exchange).append("\"");
            sb.append(",\"").append(category).append("\"");
            sb.append(",").append(prev30AvgPrice);
            sb.append(",").append(prev30AvgVol);
            sb.append(",").append(price);
            sb.append(",").append(zScore30Day);
            sb.append(",").append(nextDayOpenDate);
            sb.append(",").append(nextDayOpenPrice);
            sb.append(",").append(ema30);
            sb.append(",").append(ema50);
            sb.append(",").append(ema100);
            sb.append(",").append(vma20);
            sb.append(",").append(vma50);

            appendDatePricePair(sb, weekDate, weekPrice);
            appendDatePricePair(sb, monthDate, monthPrice);
            appendDatePricePair(sb, lowMonthDate, lowMonthPrice);
            appendDatePricePair(sb, maxMonthDate, maxMonthPrice);
            appendDatePricePair(sb, priceTargetDate, priceTargetPrice);

            sb.append(",").append(lastRecordedDate);
            sb.append(",").append(lastRecordedPrice);
            sb.append(",").append(endOfYearDate);
            sb.append(",").append(endOfYearPrice);

            sb.append('\n');
            return sb.toString();
        }

        private void appendDatePricePair(StringBuffer sb, Integer[] date, Double[] price) {
            for(int i = 0; i < date.length; i++) {
                if(date[i] != null)
                    sb.append(",").append(date[i]);
                else
                    sb.append(",");
                if(price[i] != null)
                    sb.append(",").append(price[i]);
                else
                    sb.append(",");
            }
        }
    }

    static private class ResultWriter extends Thread {

        private final BlockingQueue<Result> blockingQueue;
        private boolean finalised = false;
        private BufferedWriter writer;
        private boolean header = false;


        ResultWriter(String market, BlockingQueue<Result> blockingQueue) {
            try {
                this.blockingQueue = blockingQueue;
                writer = new BufferedWriter(new FileWriter("strategy4-trendcontinuation-"+market+".csv"));
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
            bw.append("Date,");
            bw.append("Symbol,");
            bw.append("Exchange,");
            bw.append("Category,");
            bw.append("30 Day Average Price,");
            bw.append("30 Day Average Volume,");
            bw.append("Price,");
            bw.append("30 day zscore,");
            bw.append("Next day open date,");
            bw.append("Next day open price,");
            bw.append("EMA (30 day),");
            bw.append("EMA (50 day),");
            bw.append("EMA (100 day),");
            bw.append("VMA (20 day),");
            bw.append("VMA (50 day),");


            for(int i = 0; i < Result.weekRange; i++) {
                bw.append(String.format("%d week date,", i+1));
                bw.append(String.format("%d week price,", i+1));
            }

            for(int r = 3, i = 0; i < Result.monthRange; i++, r += 3) {
                bw.append(String.format("%d month date,", r));
                bw.append(String.format("%d month price,", r));
            }

            for(int r = 3, i = 0; i < Result.lowMonthRange; i++, r += 3) {
                bw.append(String.format("%d month lowest date,", r));
                bw.append(String.format("%d month lowest price,", r));
            }

            for(int r = 3, i = 0; i < Result.maxMonthRange; i++, r += 3) {
                bw.append(String.format("%d month max date,", r));
                bw.append(String.format("%d month max price,", r));
            }

            for(int r = 10, i = 0; i < Result.priceTargetRange; i++, r += 10) {
                bw.append(String.format("%d%% price target date,", r));
                bw.append(String.format("%d%% price target price,", r));
            }

            bw.append("Last recorded Date,Last recorded Price,");
            bw.append("End of year Date,End of year Price,");


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

        final boolean useAdjustedClose = false;

        double [] vma50history = new double[5];
        int vmaHistoryIdx = 0;

        try {
            CloseData data = db.loadData(symbol);


            boolean ema30EverCrossed50 = false;
            boolean ema50EverCrossed100 = false;
            double high = 0.0;

            // we can only have a meaningful start from the 100th day since
            // one of the triggers is an 100 day average

            // gather high from previous 99 days
            int idx = 0;
            do {
                high = Math.max(high, data.close[idx]);
                idx++;
            } while(idx < 99 && idx < data.close.length);

            // start off on day 100 to give the averages something to chew
            while(idx < data.close.length) {
                // ema
                double ema30 = ema(data.close, idx, 30);
                double ema50 = ema(data.close, idx, 50);
                double ema100 = ema(data.close, idx, 100);
                double vma20 = simpleMovingAverage(data.volume, idx, 20);
                double vma50 = simpleMovingAverage(data.volume, idx, 50);
                vma50history[vmaHistoryIdx++ % vma50history.length] = vma50;

                boolean highest = data.close[idx] > high;
                high = Math.max(high, data.close[idx]);

                boolean vma20Flag = vma20 > 50000;
                boolean dailyVolFlag = false;

                for(int i = 0; i < vma50history.length && !dailyVolFlag; i++) {
                    if(data.volume[idx] > vma50history[i] * 1.5)
                        dailyVolFlag = true;
                }

                if(ema30 > ema50) {
                    ema30EverCrossed50 = true;
                }

                if(ema50 > ema100) {
                    ema50EverCrossed100 = true;
                }

                boolean trigger = dailyVolFlag && vma20Flag && highest &&
                                    ema30EverCrossed50 && ema50EverCrossed100;

                if(trigger) {
                    Result r = new Result();
                    r.date = data.date[idx];
                    r.symbol = symbol;
                    r.exchange = db.getMarket();
                    r.category = "";

                    AtomicDouble aDbl = new AtomicDouble(0.0);
                    data.avgPricePrev30Days(r.date, aDbl, useAdjustedClose);
                    r.prev30AvgPrice = aDbl.get();

                    aDbl.set(0.0);
                    data.avgVolumePrev30Days(r.date, aDbl);
                    r.prev30AvgVol = aDbl.get();

                    r.price = data.close[idx];

                    r.zScore30Day = data.zscore(r.date, 30);

                    if(idx + 1 < data.close.length) {
                        r.nextDayOpenDate = data.date[idx+1];
                        r.nextDayOpenPrice = data.close[idx+1];
                    }

                    r.ema30 = ema30;
                    r.ema50 = ema50;
                    r.ema100 = ema100;

                    r.vma20 = vma20;
                    r.vma50 = vma50;

                    AtomicInteger aInt = new AtomicInteger(0);
                    for(int i = 0; i < Result.weekRange; i++) {
                        aInt.set(0);
                        aDbl.set(0.0);
                        data.findNWeekData(i+1, r.date, aInt, aDbl, useAdjustedClose);
                        r.weekDate[i] = aInt.get();
                        r.weekPrice[i] = aDbl.get();
                    }

                    for(int t = 3, i = 0; i < Result.monthRange; i++, t += 3) {
                        aInt.set(0);
                        aDbl.set(0.0);
                        data.findNMonthData(t, r.date, aInt, aDbl, useAdjustedClose);
                        r.monthDate[i] = aInt.get();
                        r.monthPrice[i] = aDbl.get();
                    }

                    for(int t = 3, i = 0; i < Result.lowMonthRange; i++, t += 3) {
                        aInt.set(0);
                        aDbl.set(0.0);
                        data.findMinPriceFromEntry(r.date, t, aInt, aDbl, useAdjustedClose);
                        r.lowMonthDate[i] = aInt.get();
                        r.lowMonthPrice[i] = aDbl.get();
                    }

                    for(int t = 3, i = 0; i < Result.maxMonthRange; i++, t += 3) {
                        aInt.set(0);
                        aDbl.set(0.0);
                        data.findMaxPriceFromEntry(r.date, t, aInt, aDbl, useAdjustedClose);
                        r.maxMonthDate[i] = aInt.get();
                        r.maxMonthPrice[i] = aDbl.get();
                    }


                    for(int t = 10, i = 0; i < Result.priceTargetRange; i++, t += 10) {
                        aInt.set(0);
                        aDbl.set(0.0);
                        data.findPCIncreaseFromEntry(r.date, t, aInt, aDbl, useAdjustedClose);
                        r.priceTargetDate[i] = aInt.get();
                        r.priceTargetPrice[i] = aDbl.get();
                    }

                    // last recorded
                    r.lastRecordedDate = data.date[data.date.length - 1];
                    r.lastRecordedPrice = data.close[data.close.length - 1];

                    // end of year
                    data.findEndOfYearPrice(r.date, aInt, aDbl, useAdjustedClose);
                    r.endOfYearDate = aInt.get();
                    r.endOfYearPrice = aDbl.get();

                    enqueueResult(queue, r);
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
