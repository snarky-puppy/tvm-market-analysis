package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.SearchResult;
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
    private static /*final*/ int N_THREADS = (int) Math.floor(Runtime.getRuntime().availableProcessors() * 2);

    private final String market;
    private final Database database;

    private static boolean useAdjustedClose = false;

    private static double ENTRY_Z = -2;
    private static double EXIT_Z = 2;

    Ticker ticker;

    public String getName() {
        return String.format("ZScore[%s][%4.2f-%4.2f]", (useAdjustedClose ? "adjst" : "nonadjst"), ENTRY_Z, EXIT_Z);
    }

    protected AbstractScenarioFactory getScenarioFactory() {        return new CSVScenarioFactory(); }

    //protected AbstractScenarioFactory getScenarioFactory() { return new BasicScenarioFactory(database);  }

    public ZScore(String market) throws Exception {
        this.market = market;
        database = DatabaseFactory.createDatabase(market);
        ticker = new Ticker(database);
    }

    private void doit() {
        try {
            //database.dropAllZScore();
            //calculateZScores();
            //ticker.resetTicker();
            //findZScores(ENTRY_Z, EXIT_Z);
            zscore(ENTRY_Z, EXIT_Z);
            exportResults();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
                    algo.setUseAdjustedClose(useAdjustedClose);
                    try {
                        algo.zscore();
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
        logger.info("All jobs submitted in "+stopWatch.toString());
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        stopWatch.stop();
        logger.info("ZScore Crunch completed in "+stopWatch.toString());
    }

    public void findZScores(final double entryLimit, final double exitLimit) throws Exception {
        Set<String> symbols = database.listSymbols();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS); // cpu is quite idle
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(final String symbol : symbols) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, getScenarioFactory());
                    algo.setUseAdjustedClose(useAdjustedClose);
                    try {
                        algo.inMemSearch(entryLimit, exitLimit);
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

    public void zscore(final double entryLimit, final double exitLimit) throws Exception {
        Set<String> symbols = database.listSymbols();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS); // cpu is quite idle
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(final String symbol : symbols) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, getScenarioFactory());
                    algo.setUseAdjustedClose(useAdjustedClose);
                    try {
                        algo.zscore();
                        algo.inMemSearch(entryLimit, exitLimit);
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


    public void exportResults() {
        try {
            //BufferedWriter bw = new BufferedWriter(new FileWriter(Util.getOutFile(market, getName())));
            SearchResults.writeResults(market, getName());
            //bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void executeDSA(String market) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info(market);

        try {
            ZScore zScore = new ZScore(market);
            zScore.doit();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        stopWatch.stop();
        logger.info(String.format("Executed %s in %s", market, stopWatch.toString()));
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


        if(true) {
            useAdjustedClose = false;
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
