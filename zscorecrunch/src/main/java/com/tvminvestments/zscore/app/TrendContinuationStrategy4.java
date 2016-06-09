package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.CloseData;
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

/**
 * Design9.1 - news
 * Created by horse on 16/05/2016.
 */
public class TrendContinuationStrategy4 {

    private static final Logger logger = LogManager.getLogger(TrendContinuationStrategy4.class);

    private static Random random = new Random();

    private final static Map<String, Database> exchangeDB = new HashMap<>();
    private final static Map<String, CloseData> symbolData = new HashMap<>();

    // k=market
    private static final Map<String, ArrayBlockingQueue<Result>> queues = new HashMap<>();
    private static final List<ResultWriter> writerThreads = new ArrayList<>();


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

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("");
            sb.append(date);
            sb.append("\"").append(symbol).append("\"");
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
                writer = new BufferedWriter(new FileWriter("strategy4-"+market+".csv"));
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

    public static void main(String[] args) throws Exception {

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(8); // cpu is quite idle

            for (final String market : Conf.listAllMarkets()) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        TrendContinuationStrategy4 trendContinuationStrategy4 = new TrendContinuationStrategy4();
                        trendContinuationStrategy4.run(market);
                    }
                });
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

    private void run(String market) {
        final boolean useAdjustedClose = false;

        try {
            Database db = DatabaseFactory.createDatabase(market);
            for(String symbol : db.listSymbols()) {
                CloseData data = db.loadData(symbol);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        for(NewsRow news : loadNews(file)) {
            Result r = new Result();

            r.category = category;
            r.symbol = news.symbol;
            r.headline = news.news;
            r.exchange = symbolToExchange.get(r.symbol);

            CloseData data = getCloseData(r.symbol);
            if(data == null)
                continue;

            AtomicDouble aDbl = new AtomicDouble(0.0);
            data.avgPricePrev30Days(news.date, aDbl, useAdjustedClose);
            r.prev30AvgPrice = aDbl.get();


            data.avgVolumePrev30Days(news.date, aDbl);
            r.prev30AvgVol = aDbl.get();
            r.headlineDate = news.date;


            int idx = data.findDateIndex(news.date);
            for(int i = 0; i < Result.range && idx < data.date.length; i++, idx++) {
                r.date[i] = data.date[idx];
                r.open[i] = data.open[idx];
                r.volume[i] = data.volume[idx];
            }

            enqueueResult(category, r);
        }
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
