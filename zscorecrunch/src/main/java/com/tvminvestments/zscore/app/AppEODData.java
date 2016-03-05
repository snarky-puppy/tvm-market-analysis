package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.CSVScenarioFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * High level application interface
 *
 * Created by horse on 9/12/14.
 */
public class AppEODData {
    /*
    private static final Logger logger = LogManager.getLogger(AppEODData.class);

    private static final int N_THREADS = (int) Math.floor(Runtime.getRuntime().availableProcessors() * 1.5);

    private int totalSymbols;
    private int symbolsProcessed;

    protected final Database database;

    public AppEODData(String market) throws Exception {
        database = DatabaseFactory.createDatabase(market);
    }

    public void resetTicker() throws Exception {
        totalSymbols = database.listSymbols().size();
        symbolsProcessed = 0;
    }

    private synchronized void incrementTicker() {
        symbolsProcessed++;
        logger.info(String.format("progress: %f%%", ((float)symbolsProcessed/(float)totalSymbols) * 100));
    }



    public void importData(String path) throws IOException {
        ImportEODData importer = new ImportEODData();
        importer.importData(database, path);
    }

    public void calculateZScores() throws Exception {
        Set<String> symbols = database.listSymbols();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(final String symbol : symbols) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, getScenarioFactory());
                    try {
                        algo.zscore();
                        //logger.info("["+symbol+"] ZScore calc finished! "+database.getZScoreCollection(symbol).count()+" zscores total");
                        //database.updateZScoreIndex(symbol);
                    } catch(Exception e) {
                        logger.error("["+symbol+"] b0rked", e);
                    }
                    incrementTicker();
                }
            });
        }
        logger.info("All jobs submitted in "+stopWatch.toString());
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_ADJUSTMENTS, TimeUnit.DAYS);
        stopWatch.stop();
        logger.info("ZScore Crunch completed in "+stopWatch.toString());
    }

    public void findZScores(final int entryLimit, final int exitLimit) throws Exception {
        Set<String> symbols = database.listSymbols();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(final String symbol : symbols) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, getScenarioFactory());
                    try {
                        algo.inMemSearch(entryLimit, exitLimit);
                    } catch(Exception e) {
                        logger.error("["+symbol+"] b0rked", e);
                    }
                    incrementTicker();
                }
            });
        }
        logger.info("All jobs submitted in "+stopWatch.toString());
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_ADJUSTMENTS, TimeUnit.DAYS);
        stopWatch.stop();
        logger.info("ZScore Search completed in "+stopWatch.toString());

    }

    public void exportResults() throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(getOutFile()));
        bw.write("Symbol,Scenario ID,Sub-scenario" +
                ",Sample Start Date,Tracking Start Date,Tracking End Date" +
                ",Result Code,Entry Date,Entry ZScore,Entry Price,Exit Date,Exit ZScore,Exit Price" +
                "\n");

        database.writeResults(bw);

        bw.close();
    }


    public String getOutFile() {
        int rev = 1;
        String outFilePath;

        File file;
        do {
            outFilePath = String.format("%s-%d-%d.csv", database.market, DateUtil.today(), rev);
            file = new File(outFilePath);
            rev++;
        } while(file.isFile());

        return outFilePath;
    }

    protected AbstractScenarioFactory getScenarioFactory() {
        return new CSVScenarioFactory();
    }

    private static void doit(String market, boolean importData) {

        try {
            AppEODData app = new AppEODData(market);

            if (importData) {
                for (String sym : app.database.listSymbols()) {
                    app.database.getDataCollection(sym).drop();
                    app.database.getZScoreCollection(sym).drop();

                }
                app.importData(String.format("/Users/horse/Projects/data/%s/csv", market));
            }
            app.resetTicker();

            app.calculateZScores();

            app.database.clearResults();

            app.resetTicker();
            app.findZScores(-2, 2);

            app.exportResults();
        } catch(Exception e) {
            logger.error("["+market+"] b0rked", e);
        }
    }

    private static void add(ExecutorService executorService, final String market, final boolean dataImport) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                AppEODData.doit(market, dataImport);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        //add(executorService, "ASX", false);
        add(executorService, "HKEX", false);
        add(executorService, "LSE", true);
        add(executorService, "TSX", true);
        add(executorService, "NYSE", false);
        add(executorService, "NASDAQ", false);

        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_ADJUSTMENTS, TimeUnit.DAYS);
        stopWatch.stop();
        logger.info("All jobs completed in " + stopWatch.toString());

    }
    */
}
