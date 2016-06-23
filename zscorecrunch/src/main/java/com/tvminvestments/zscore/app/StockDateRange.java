package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.EMAException;
import com.tvminvestments.zscore.RangeBounds;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Design9.1 - news
 * Created by horse on 16/05/2016.
 */
public class StockDateRange {

    private static final Logger logger = LogManager.getLogger(StockDateRange.class);

    private static Random random = new Random();

    private final static Map<String, Database> exchangeDB = new HashMap<>();
    private final static Map<String, CloseData> symbolData = new HashMap<>();

    private static ArrayBlockingQueue<Result> queue = new ArrayBlockingQueue<Result>(1024);
    private static ResultWriter writerThread = new ResultWriter(queue);
    private static int count = 0;

    public static void main(String[] args) throws Exception {

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(4); // only 4 markets

            final boolean isTest = false;

            writerThread.start();

            if(isTest) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        StockDateRange stockDateRange = new StockDateRange();
                        stockDateRange.run("test");
                    }
                });

            } else {
                for (final String market : Conf.listAllMarkets()) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            StockDateRange stockDateRange = new StockDateRange();
                            stockDateRange.run(market);
                        }
                    });
                }
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

            System.out.println("count="+count);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                writerThread.finalise();
                writerThread.join();
                writerThread.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    class Result {
        public String symbol;
        public String exchange;
        // dates
        public int dateStart;
        public int dateEnd;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(symbol);
            sb.append(",").append(exchange);
            sb.append(",").append(dateStart);
            sb.append(",").append(dateEnd);
            sb.append('\n');
            return sb.toString();
        }
    }

    static private class ResultWriter extends Thread {

        private final BlockingQueue<Result> blockingQueue;
        private boolean finalised = false;
        private BufferedWriter writer;
        private boolean header = false;


        ResultWriter(BlockingQueue<Result> blockingQueue) {
            try {
                this.blockingQueue = blockingQueue;
                writer = new BufferedWriter(new FileWriter("StockDateRange.csv"));
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
            bw.append("Start Date,");
            bw.append("End Date,");

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

        ExecutorService executorService = Executors.newFixedThreadPool(16);

        try {
            Database db = DatabaseFactory.createDatabase(market);
            for (String symbol : db.listSymbols()) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        processSymbol(db, symbol);
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

    private void processSymbol(Database db, String symbol) {
        try {
            count++;
            RangeBounds bounds = db.findDataBounds(symbol);
            Result r = new Result();
            r.exchange = db.getMarket();
            r.symbol = symbol;
            r.dateStart = bounds.getMin();
            r.dateEnd = bounds.getMax();

            enqueueResult(queue, r);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
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

}
