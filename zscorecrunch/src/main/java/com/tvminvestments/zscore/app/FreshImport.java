package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Delete and re-import all data
 *
 * Created by horse on 16/07/15.
 */
public class FreshImport {

    private static final Logger logger = LogManager.getLogger(FreshImport.class);


    /**
     * A note on execution time vs number of threads (tested with 4090 files):
     * 1 thread - 15 mins
     * 16 threads - 7:14
     * 32 threads - 7:30
     */

    private static final int nThreads = Runtime.getRuntime().availableProcessors() * 2;

    private static final int blockSize = 500;


    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            for (String market : Conf.listAllMarkets()) {
                Database db = DatabaseFactory.createDatabase(market);
                db.freshImport();

                String path = String.format("%s/%s", Conf.PD_DIR, market);

                FreshImport fi = new FreshImport();
                fi.importData(db, path);

            }
        } catch (Exception e) {
            logger.error("top level: ", e);
        } finally {
            logger.info("all done in "+stopWatch.toString());
        }
    }


    private int processBlock(int blockSize, Iterator<Path > iterator, final Database database) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        int count = 0;

        while(blockSize >= 0 && iterator.hasNext()) {
            final Path path = iterator.next();

            String symbol = path.getName(path.getNameCount()-1).toString();
            symbol = symbol.replaceAll(".csv", "");

            final String finalSymbol = symbol;

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        readCSVFile(finalSymbol, path, database);
                    } catch (RuntimeException e) {
                        logger.error("RunTimeException: ", e);
                        System.exit(1);

                    } catch (Exception e) {
                        logger.error("Exception: ", e);
                        System.exit(1);
                    }
                }
            });
            count++;
            blockSize --;
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Writing data...");
        database.commitDataTransaction();

        return count;
    }

    public void importData(Database database, String path) throws IOException {

        int count = 0;

        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), "*.csv");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        java.util.Iterator<Path> iterator = stream.iterator();

        while(iterator.hasNext()) {
            count += processBlock(blockSize, iterator, database);
        }

        stopWatch.stop();
        logger.info("Processed "+count+" files in "+stopWatch.toString());

        //System.out.println("Indexes...");
        //stopWatch.reset();
        //stopWatch.start();
        //database.updateDataIndexes();
        //stopWatch.stop();
        //System.out.println("Index update took  "+stopWatch.toString());
    }

    private void readCSVFile(String symbol, Path fileName, Database database) throws Exception {

        boolean first = true;
        String line;
        int counter = 0;

        logger.info("Processing "+fileName.toString());

        BufferedReader br = new BufferedReader(new FileReader(fileName.toString()));
        while((line = br.readLine()) != null) {
            if(first) {
                first = false;

            } else {
                String[] data = line.split(",");
                int date = Integer.parseInt(data[DailyUpdate.dateIndex]);
                double close = Double.parseDouble(data[DailyUpdate.closeIndex]);
                double volume = Double.parseDouble(data[DailyUpdate.volumeIndex]);
                double open = Double.parseDouble(data[DailyUpdate.openIndex]);

                database.insertData(symbol, date, close, volume, open);

                counter++;
            }
        }

        br.close();
        System.out.println(fileName.toString()+": Processed "+counter+" entries.");
    }

}
