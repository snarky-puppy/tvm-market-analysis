package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.SearchResults;
import com.tvminvestments.zscore.ZScoreAlgorithm;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.BasicScenarioFactory;
import com.tvminvestments.zscore.scenario.CSVScenarioFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ZScore application
 *
 * Created by horse on 4/06/15.
 */
public class ZScore {

    private static final Logger logger = LogManager.getLogger(ZScore.class);
    public static /*final*/ int N_THREADS = (int) Math.floor(Runtime.getRuntime().availableProcessors() * 1.5);
    private static boolean useRestrictedOutput = false;

    private final Object sleepLock = new Object();
    private int sleepTimer;

    private final String market;
    private final Database database;

    private static boolean useAdjustedClose = false;

    private static double ENTRY_Z = -2;
    private static double EXIT_Z = 2;

    Ticker ticker;

    SearchResults searchResults;

    public String getName() {
        return String.format("ZScore[%s][%4.2f-%4.2f]", (useAdjustedClose ? "adjst" : "nonadjst"), ENTRY_Z, EXIT_Z);
    }

    protected AbstractScenarioFactory getScenarioFactory() {        return new CSVScenarioFactory(); }

    //protected AbstractScenarioFactory getScenarioFactory() { return new BasicScenarioFactory(database);  }

    public ZScore(String market) throws Exception {
        this.market = market;
        database = DatabaseFactory.createDatabase(market);
        ticker = new Ticker(database);
        searchResults = new SearchResults(market, getName(), useRestrictedOutput);
        sleepTimer = 0;
    }

    private void doit() {
        try {
            zscore(ENTRY_Z, EXIT_Z);
            searchResults.finalise();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private int getSleepTimer() {
        final int max = 1500;
        final int min = 0;
        final int step = 10;
        final int thresh = 10;


        int result;
        synchronized (sleepLock) {
            result = sleepTimer;
        }
        int q = searchResults.queueSize();

        if(q < thresh)
            result -= step;
        if(q > thresh)
            result += step;

        if(result > max)
            result = max;
        if(result < min)
            result = min;

        synchronized (sleepLock) {
            sleepTimer = result;
        }

        return result;
    }


    public void zscore(final double entryLimit, final double exitLimit) throws Exception {
        Set<String> symbols = database.listSymbols();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS); // cpu is quite idle
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(final String symbol : symbols) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, getScenarioFactory(), searchResults);
                    algo.setUseAdjustedClose(useAdjustedClose);
                    try {
                        algo.zscore();
                        int cnt = algo.inMemSearch(entryLimit, exitLimit);
                        int sleep = getSleepTimer();
                        logger.error("["+symbol+"] "+cnt+" results ("+sleep+")");
                        Thread.sleep(sleep); // delay to let @SearchResults writer catch up so we don't run out of RAM due to caching of results
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
        logger.info("All jobs submitted in "+stopWatch.toString());
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        stopWatch.stop();
        logger.info("ZScore Search completed in "+stopWatch.toString());
    }


    public static void executeDSA(String market) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("Execute DSA: "+market);

        try {
            ZScore zScore = new ZScore(market);
            zScore.doit();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        stopWatch.stop();
        logger.info(String.format("Market %s executed in %s", market, stopWatch.toString()));
    }

    public static void executeAllDataSources() {
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        List<String> markets = Conf.listAllMarkets();


        for(String market : markets) {
            //if(market.compareTo("OTC") != 0)
                executeDSA(market);
        }
        stopWatch.stop();
        logger.info("All jobs completed in " + stopWatch.toString());
    }

    public static void main(String[] args) {

        useRestrictedOutput = false;


        if(true) {
            useAdjustedClose = false;

            //executeDSA("OTC");

            //executeDSA("ASX");


            executeAllDataSources();

            //ENTRY_Z = -3;
            //executeAllDataSources();

            //useAdjustedClose = true;
            //executeAllDataSources();
        } else {
            N_THREADS = 1;
            DailyUpdate update = new DailyUpdate();
            update.updateMarket("test");
            executeDSA("test");
        }
    }

}
