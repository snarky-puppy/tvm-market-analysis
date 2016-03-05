package com.tvminvestments.zscore.importer;

import com.mongodb.BulkWriteOperation;
import com.mongodb.WriteConcern;
import com.tvminvestments.zscore.db.Database;
import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by horse on 6/02/15.
 */
public class CSVFileImporter {

    /**
     * A note on execution time vs number of threads (tested with 4090 files):
     * 1 thread - 15 mins
     * 16 threads - 7:14
     * 32 threads - 7:30
     */

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private static final int closeIndex = 4;
    private static final int symIndex = 7;
    private static final int dateIndex = 0;

    private HashMap<String, BulkWriteOperation> collections;


    private int processBlock(int blockSize, Iterator<Path> iterator, final Database database) {
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        int count = 0;

        collections = new HashMap<String, BulkWriteOperation>();

        while(blockSize >= 0 && iterator.hasNext()) {
            final Path path = iterator.next();

            String symbol = path.getName(path.getNameCount()-1).toString();

            symbol = symbol.replaceAll(".csv", "");
            symbol = symbol.replaceAll("-", ".");

            final String finalSymbol = symbol;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        readCSVFile(finalSymbol, path, database);
                    } catch (IOException e) {
                        System.out.println("b0rked");
                        e.printStackTrace();
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

        System.out.println("Writing data...");
        for(BulkWriteOperation bulk : collections.values()) {
            bulk.execute(WriteConcern.UNACKNOWLEDGED);
        }

        return count;
    }

    public void importData(Database database, String path) throws IOException {

        int count = 0;

        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), "*.csv");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Iterator<Path> iterator = stream.iterator();

        while(iterator.hasNext()) {
            count += processBlock(500, iterator, database);
        }

        stopWatch.stop();
        System.out.println("Processed "+count+" files in "+stopWatch.toString());

        System.out.println("Indexes...");
        stopWatch.reset();
        stopWatch.start();
        //database.updateDataIndexes();
        stopWatch.stop();
        System.out.println("Index update took  "+stopWatch.toString());
    }

    private void readCSVFile(String symbol, Path fileName, Database database) throws IOException {

        boolean first = true;
        String line;
        int counter = 0;

        System.out.println("Processing "+fileName.toString());

        BufferedReader br = new BufferedReader(new FileReader(fileName.toString()));
        while((line = br.readLine()) != null) {
            if(first) {
                first = false;

            } else {
                String[] data = line.split(",");
                //String symbol = data[symIndex];
                int date = Integer.parseInt(data[dateIndex]);
                double close = Double.parseDouble(data[closeIndex]);

                /*
                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MMM yyyy");
                DateTime dt = formatter.parseDateTime(date);
                int formattedDate = DateUtil.dateTimeToInteger(dt);
                */

                /*
                synchronized (collections) {
                    if(!collections.containsKey(symbol)) {
                        collections.put(symbol, database.getDataCollection(symbol).initializeOrderedBulkOperation());
                    }
                    collections.get(symbol).insert(database.getInsertDataDocument(date, close));
                }
                */

                counter++;
            }
        }
        System.out.println(fileName.toString()+": Processed "+counter+" symbols.");
    }
}
