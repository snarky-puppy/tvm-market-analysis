package com.tvminvestments.zscore.app;

import com.google.api.client.util.Data;
import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.Result;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.db.FileDB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Design9 - news
 * Created by horse on 16/05/2016.
 */
public class Design9 {

    private static final Logger logger = LogManager.getLogger(Design9.class);

    private static Random random = new Random();

    // k=symbol, v=exchange
    private final static Map<String, String> symbolToExchange = new HashMap<>();

    private final static Map<String, Database> exchangeDB = new HashMap<>();
    private final static Map<String, CloseData> symbolData = new HashMap<>();

    // k=category
    private static final Map<String, ArrayBlockingQueue<Result>> queues = new HashMap<>();
    private static final List<ResultWriter> writerThreads = new ArrayList<>();

    class NewsRow {
        public int date;
        public String symbol;
        public String news;
    }

    class Result {
        public int date;
        public String symbol;
        public String exchange;
        public String headline;
        public String category;
        public double prev30AvgPrice;
        public double prev30AvgVol;
        public int headlineDate;
        public double headlinePrice;
        public double headline30DayZScore;
        public int headlineNextDayOpenDate;
        public double headlineNextDayOpenPrice;
        public int post1WeekDate;
        public double post1WeekOpenPrice;
        public int post2WeekDate;
        public double post2WeekOpenPrice;
        public int post3WeekDate;
        public double post3WeekOpenPrice;
        public int post4WeekDate;
        public double post4WeekOpenPrice;

        public int preControlDate;
        public double preControlPrice;

        public int postControlDate;
        public double postControlPrice;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("");
            sb.append(date);
            sb.append(",\"").append(symbol).append("\"");
            sb.append(",\"").append(exchange).append("\"");
            sb.append(",\"").append(headline).append("\"");
            sb.append(",\"").append(category).append("\"");
            sb.append(",").append(prev30AvgPrice);
            sb.append(",").append(prev30AvgVol);
            sb.append(",").append(headlineDate);
            sb.append(",").append(headlinePrice);
            sb.append(",").append(headline30DayZScore);
            sb.append(",").append(headlineNextDayOpenDate);
            sb.append(",").append(headlineNextDayOpenPrice);
            sb.append(",").append(post1WeekDate);
            sb.append(",").append(post1WeekOpenPrice);
            sb.append(",").append(post2WeekDate);
            sb.append(",").append(post2WeekOpenPrice);
            sb.append(",").append(post3WeekDate);
            sb.append(",").append(post3WeekOpenPrice);
            sb.append(",").append(post4WeekDate);
            sb.append(",").append(post4WeekOpenPrice);
            sb.append(",").append(preControlDate);
            sb.append(",").append(preControlPrice);
            sb.append(",").append(postControlDate);
            sb.append(",").append(postControlPrice);
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
                writer = new BufferedWriter(new FileWriter("design9-"+category+".csv"));
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
            bw.append("Headline,");
            bw.append("Category,");
            bw.append("30 Day Average Price,");
            bw.append("30 Day Average Volume,");
            bw.append("Headline Date,");
            bw.append("Headline Price,");
            bw.append("Headline 30 Day ZScore,");
            bw.append("Headline Next Day Open Date,");
            bw.append("Headline Next Day Open Price,");
            bw.append("1 Week Date,");
            bw.append("1 Week Open Price,");
            bw.append("2 Week Date,");
            bw.append("2 Week Open Price,");
            bw.append("3 Week Date,");
            bw.append("3 Week Open Price,");
            bw.append("4 Week Date,");
            bw.append("4 Week Open Price,");

            bw.append("preControlDate,");
            bw.append("preControlPrice,");

            bw.append("postControlDate,");
            bw.append("postControlPrice");
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

        // populate symbol->exchange map
        try {
            for (String market : Conf.listAllMarkets()) {
                Database db = DatabaseFactory.createDatabase(market);
                exchangeDB.put(market, db);
                for (String symbol : db.listSymbols()) {
                    if (symbolToExchange.containsKey(symbol)) {
                        System.out.println(String.format("Duplicate symbol: symbol=%s exchanges=%s,%s",
                                symbol, symbolToExchange.get(symbol), market));
                        //System.exit(1);
                    }
                    symbolToExchange.put(symbol, market);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        File newsDir = new File("/Users/horse/Projects/news");
        final File[] files = newsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches(".*\\.csv");
            }
        });

        try {
            for (File file : files) {
                String category = file.getName().substring(0, file.getName().indexOf('-'));
                logger.info("file: " + file.getName());
                logger.info("category: " + category);

                Design9 design9 = new Design9();
                design9.run(file, category);
            }
        } finally {
            stopAllWriterThreads();
        }

    }

    private CloseData getCloseData(String symbol) {
        synchronized (symbolData) {
            if(!symbolData.containsKey(symbol)) {
                if(!symbolToExchange.containsKey(symbol)) {
                    return null;
                } else {
                    synchronized (exchangeDB) {
                        try {
                            Database db = exchangeDB.get(symbolToExchange.get(symbol));
                            if (db == null)
                                logger.error("no db for " + symbol + " / " + symbolToExchange.get(symbol));
                            CloseData cd = db.loadData(symbol);
                            symbolData.put(symbol, cd);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return symbolData.get(symbol);
        }
    }

    private List<NewsRow> loadNews(File file) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");

            List<NewsRow> rv = new ArrayList<>();
            for(String line : Files.readAllLines(file.toPath())) {
                // "date","SYMBOL","News blah blah"
                String[] fields = line.split("\"");

                //System.out.println(String.format("date=%s, sym=%s, news=%s",
                //        fields[1], fields[3], fields[5]));

                NewsRow newsRow = new NewsRow();
                newsRow.date = Integer.parseInt(sdf2.format(sdf.parse(fields[1])));
                newsRow.symbol = fields[3];
                newsRow.news = fields[5];

                rv.add(newsRow);
            }
            return rv;

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void run(File file, String category) {
        final boolean useAdjustedClose = false;

        for(NewsRow news : loadNews(file)) {
            Result r = new Result();

            r.category = category;
            r.date = news.date;
            r.symbol = news.symbol;
            r.headline = news.news;
            r.exchange = symbolToExchange.get(r.symbol);

            CloseData data = getCloseData(r.symbol);
            if(data == null)
                continue;

            AtomicDouble aDbl = new AtomicDouble(0.0);
            data.avgPricePrev30Days(r.date, aDbl, useAdjustedClose);
            r.prev30AvgPrice = aDbl.get();


            data.avgVolumePrev30Days(r.date, aDbl);
            r.prev30AvgVol = aDbl.get();

            r.headlineDate = r.date;


            r.headlinePrice = data.findClosePriceAtDate(r.date);


            r.headline30DayZScore = data.zscore(r.date, 30);

            AtomicInteger aInt = new AtomicInteger(0);
            data.findOpenNDaysLater(r.date, 1, aInt, aDbl);
            r.headlineNextDayOpenDate = aInt.get();
            r.headlineNextDayOpenPrice = aDbl.get();

            data.findNWeekData(1, r.date, aInt, aDbl, useAdjustedClose);
            r.post1WeekDate = aInt.get();
            r.post1WeekOpenPrice = aDbl.get();

            data.findNWeekData(2, r.date, aInt, aDbl, useAdjustedClose);
            r.post2WeekDate = aInt.get();
            r.post2WeekOpenPrice = aDbl.get();

            data.findNWeekData(3, r.date, aInt, aDbl, useAdjustedClose);
            r.post3WeekDate = aInt.get();
            r.post3WeekOpenPrice = aDbl.get();

            data.findNWeekData(4, r.date, aInt, aDbl, useAdjustedClose);
            r.post4WeekDate = aInt.get();
            r.post4WeekOpenPrice = aDbl.get();


            int pre = data.findDateIndex(DateUtil.minusDays(r.date, random.nextInt(100)));
            int post = data.findDateIndex(DateUtil.addDays(r.date, random.nextInt(100)));

            if(pre > 0) {
                r.preControlDate = data.date[pre];
                r.preControlPrice = data.findClosePriceAtDate(data.date[pre]);
            }

            if(post > 0) {
                r.postControlDate = data.date[post];
                r.postControlPrice = data.findClosePriceAtDate(data.date[post]);
            }

            enqueueResult(category, r);
        }
    }


    private void enqueueResult(String category, Result result) {
        ArrayBlockingQueue<Result> queue = null;

        synchronized (queues) {
            if(!queues.containsKey(category)) {
                queue = new ArrayBlockingQueue<Result>(1024);
                ResultWriter resultWriter = new ResultWriter(category, queue);
                writerThreads.add(resultWriter);
                queues.put(category, queue);
                resultWriter.start();
            } else
                queue = queues.get(category);
        }

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
