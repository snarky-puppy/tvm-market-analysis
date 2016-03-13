package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.*;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.DailyScenarioFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by horse on 5/09/15.
 */
public class DailyTriggerReport {
    private static final Logger logger = LogManager.getLogger(DailyTriggerReport.class);

    private static final int ENTRY_LIMIT = -2;
    private static final int EXIT_LIMIT = 2;

    private static final int TRACKING_DAYS = 2; //1; PD lags behind a couple of days

    private static final int N_THREADS = (int) Math.floor(Runtime.getRuntime().availableProcessors() * 2);
    private static boolean useRestrictedOutput = true;
    private final Database database;

    String market;

    DailyTriggerReport(String market) throws Exception {
        this.market = market;
        this.database = DatabaseFactory.createDatabase(market);

    }

    public static void main(String[] args) throws Exception {


        useRestrictedOutput = true;


        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        DailyUpdate update = new DailyUpdate();
        update.updateAll();
        stopWatch.stop();
        logger.info("Data update completed in " + stopWatch.toString());
        stopWatch.reset();
        stopWatch.start();


        if(args.length > 0) {
            for(String market : args) {
                DailyTriggerReport report = new DailyTriggerReport(market);
                report.doit();
            }
            return;
        } else {

            List<String> markets = Conf.listAllMarkets();

            for (String market : markets) {
                if (market.compareTo("OTC") != 0) {
                    DailyTriggerReport report = new DailyTriggerReport(market);
                    report.doit();
                }
            }
        }

        // export
        int results = SearchResults.results.size();
        String fileName = Util.getDailyOutFile();
        exportResults(fileName);
        Gmailer.sendNotification(results,
                "/"+FilenameUtils.getPath(fileName),
                FilenameUtils.getName(fileName));

        stopWatch.stop();
        logger.info("All jobs completed in " + stopWatch.toString());
    }

    private void doit() {

        try {
            Ticker ticker = new Ticker(database);
            StopWatch stopWatch = new StopWatch();

            if(DateUtil.isFirstOfMonth()) {
                logger.info("Resetting zscore");
                stopWatch.start();
                database.dropAllZScore();
                stopWatch.stop();
                logger.info("ZScore reset completed in "+stopWatch.toString());
                stopWatch.reset();
            }

            // update zscores
            {
                stopWatch.start();
                ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
                for (final String symbol : database.listSymbols()) {
                    executorService.submit(new Runnable() {

                        @Override
                        public void run() {
                            ZScoreAlgorithmDaily algo = new ZScoreAlgorithmDaily(symbol, database, getScenarioFactory());
                            algo.setUseAdjustedClose(false);
                            try {
                                algo.zscore();
                                algo.inMemSearch(ENTRY_LIMIT, EXIT_LIMIT);
                                //logger.info("["+symbol+"] ZScore calc finished! "+database.getZScoreCollection(symbol).count()+" zscores total");
                                //database.updateZScoreIndex(symbol);
                            } catch (RuntimeException e) {
                                logger.error("RunTimeException: ", e);
                                System.exit(1);
                            } catch (Exception e) {
                                logger.error("[" + symbol + "] b0rked", e);
                                System.exit(1);
                            }
                            ticker.incrementTicker();
                        }
                    });
                }

                logger.info("All jobs submitted in " + stopWatch.toString());
                executorService.shutdown();
                executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
                stopWatch.stop();
                logger.info("ZScore Crunch completed in " + stopWatch.toString());
                stopWatch.reset();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void exportResults(String fp) {
        try {
            if(SearchResults.results.size() > 0) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(fp, true));
                SearchResults.writeResults(bw, useRestrictedOutput);
                bw.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public AbstractScenarioFactory getScenarioFactory() {
        return new DailyScenarioFactory(TRACKING_DAYS);
    }
}
