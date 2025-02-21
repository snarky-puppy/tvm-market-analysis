package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Design9.1 - news
 * Created by horse on 16/05/2016.
 */
public class Design9_1 {

    private static final Logger logger = LogManager.getLogger(Design9_1.class);

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
        public static final int range = 42;
        public String symbol;
        public String exchange;
        public String headline;
        public String category;
        public double prev30AvgPrice;
        public double prev30AvgVol;
        public int headlineDate;

        public Integer[] date = new Integer[range];
        public Double[] open = new Double[range];
        public Double[] volume = new Double[range];


        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("");
            sb.append("\"").append(symbol).append("\"");
            sb.append(",\"").append(exchange).append("\"");
            sb.append(",\"").append(headline).append("\"");
            sb.append(",\"").append(category).append("\"");
            sb.append(",").append(prev30AvgPrice);
            sb.append(",").append(prev30AvgVol);
            sb.append(",").append(headlineDate);

            for(int i = 0; i < range; i++) {
                if(date[i] != null)
                    sb.append(",").append(date[i]);
                else
                    sb.append(",");
                if(open[i] != null)
                    sb.append(",").append(open[i]);
                else
                    sb.append(",");
                if(volume[i] != null)
                    sb.append(",").append(volume[i]);
                else
                    sb.append(",");
            }
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
                writer = new BufferedWriter(new FileWriter("design9.1-"+category+".csv"));
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

            for(int i = 0; i < Result.range; i++) {
                bw.append(String.format("day %d date,", i));
                bw.append(String.format("day %d open,", i));
                bw.append(String.format("day %d volume,", i));
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

                Design9_1 design9 = new Design9_1();
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
                            if (db == null) {
                                logger.error("no db for " + symbol + " / " + symbolToExchange.get(symbol));
                            } else {
                                CloseData cd = db.loadData(symbol);
                                symbolData.put(symbol, cd);
                            }
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
            for(String line : Files.readAllLines(file.toPath())){
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
            if(data.date[idx] == news.date)
                idx++;

            for(int i = 0; i < Result.range && idx < data.date.length; i++, idx++) {
                r.date[i] = data.date[idx];
                r.open[i] = data.open[idx];
                r.volume[i] = data.volume[idx];
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
