package com.tvminvestments.zscore;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.Scenario;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Algorithm container class
 *
 * Created by horse on 13/11/14.
 */
public class ZScoreAlgorithmDaily {
    private static final Logger logger = LogManager.getLogger(ZScoreAlgorithmDaily.class);

    private final String symbol;
    private final AbstractScenarioFactory scenarioFactory;
    private final Database database;

    private int minScenarioDate;
    private int maxScenarioDate;

    // lazy load data - check data bounds vs max zscore
    private CloseData data = null;
    private boolean useAdjustedClose;


    public ZScoreAlgorithmDaily(String symbol, Database database, AbstractScenarioFactory scenarioFactory) {
        this.scenarioFactory = scenarioFactory;
        this.symbol = symbol;
        minScenarioDate = 0;
        maxScenarioDate = 0;
        this.database = database;
        this.data = null;
    }

    public void setUseAdjustedClose(boolean useAdjustedClose) {
        this.useAdjustedClose = useAdjustedClose;
    }

    public void zscore() throws Exception {
        Map<Integer, Integer> ranges = extractScenarioRanges();

        if(ranges.size() == 0) {
            logger.info("["+symbol+"]: no scenarios");
            return;
        }

        RangeBounds bounds = database.findDataBounds(symbol);
        Map<Integer, ZScoreEntry> zscores = new HashMap<Integer, ZScoreEntry>();

        logger.info("["+symbol+"] data bounds: "+bounds.getMin()+"-"+bounds.getMax());


        for(int startDate : ranges.keySet()) {
            int count = 0;
            int endDate = ranges.get(startDate);

            logger.info("["+symbol+"] scenario range: "+startDate+"-"+endDate);

            // edge case: allow 7 days for market to re-open after possible holidays
            if(DateUtil.addDays(startDate, 7) < bounds.getMin()) {
                logger.info("["+symbol+"] Scenario start date is before data start date ("+bounds.getMin()+"-"+bounds.getMax()+"). Skipping.\n");
                continue;
            }

            if(endDate <= bounds.getMin()) {
                logger.info("["+symbol+"] Scenario end date is before data start date ("+bounds.getMin()+"-"+bounds.getMax()+"). Skipping.\n");
                continue;
            }

            if(startDate > bounds.getMax()) {
                logger.info("["+symbol+"] Scenario start date is after data end date ("+bounds.getMin()+"-"+bounds.getMax()+"). Skipping.\n");
                continue;
            }

            int zscoreMaxDate = database.findMaxZScoreDataDate(symbol, startDate);

            logger.info("["+symbol+"] start zscore calc from: "+zscoreMaxDate);

            // if last zscore date >= scenario end date, we can skip this
            if(zscoreMaxDate >= endDate) {
                logger.info("["+symbol+"] scenario range already calculated.\n");
                continue;
            }

            // optimisation, check if zscore is up to date *before* loading the full set of zscores.
            if(bounds.getMax() <= zscoreMaxDate) {
                logger.error("["+symbol+"] Not enough available data to do anything. Skipping.\n");
                continue;
            }

            // made it this far, must need to load the data. so do it.
            if(data == null) // possibly save a data load if this scenario is completed
                data = database.loadData(symbol);

            if(data.date.length <= 3) {
                logger.error("[" + symbol + "] not enough data: " + data.date.length + " rows");
                continue;
            }
            logger.info("len="+data.date.length);
            logger.info("["+symbol+"] loaded data: "+data.date[0]+"-"+data.date[data.date.length-1]);


            int zscoreStartDate = DateUtil.addDays(zscoreMaxDate, 1);

            int startIndex = data.findDateIndex(startDate);
            if(startIndex == -1) {
                logger.error("["+symbol+"] Could not find start date: "+startDate);
                continue;
            }

            // should not fail
            int calcIndex = data.findDateIndex(zscoreStartDate, false);
            if(calcIndex == -1) {
                logger.error("["+symbol+"] Could not find index for starting date: "+zscoreStartDate+"\n");
                continue;
            }

            // should not fail
            int calcEndIndex = data.findDateIndex(endDate);
            if(calcEndIndex == -1) {
                logger.error("["+symbol+"] Could not find end date: "+endDate+"\n");
                continue;
            }

            // take into account weekends etc... indexes may not match precisely with dates
            if(calcEndIndex == calcIndex) {
                logger.error("["+symbol+"] already up to date within limits of available data. Skipping.\n");
                continue;
            }

            // everything in order, now start cranking:
            int numZScores = calcEndIndex - calcIndex;
            if(calcIndex != startIndex) {
                numZScores += 1;
            }
            // startIndex == calcIndex - guaranteed there will be a NaN for first value
            ZScoreEntry entry = new ZScoreEntry(calcEndIndex - calcIndex + 1);
            zscores.put(startDate, entry);
            SummaryStatistics stats = new SummaryStatistics();

            // preload stats thing
            while(startIndex < calcIndex) {
                stats.addValue(data.close[startIndex++]);
            }

            logger.info(String.format("calcIndex=%d, calcEndIndex=%d", calcIndex, calcEndIndex));
            logger.info(String.format("from=%d, to=%d", data.date[calcIndex], data.date[calcEndIndex]));


            while(calcIndex <= calcEndIndex) {
                if(useAdjustedClose)
                    stats.addValue(data.adjustedClose[calcIndex]);
                else
                    stats.addValue(data.close[calcIndex]);

                double stdev = stats.getStandardDeviation();
                if(stdev == 0) {
                    logger.debug(String.format("NaN: %d", data.date[calcIndex]));
                    // either this is the first value or all initial values this far have had no variance (were the same)
                    entry.addZScore(-1, 0);
                    calcIndex ++;
                    continue;
                }
                double avg = stats.getMean();
                double closeValue;
                if(useAdjustedClose)
                    closeValue = data.adjustedClose[calcIndex];
                else
                    closeValue = data.close[calcIndex];
                double zscore = (closeValue - avg) / stdev;

                //logger.debug(String.format("z: %d, %f, ci=%d, cei=%d", data.date[calcIndex], zscore, calcIndex, calcEndIndex));
                entry.addZScore(data.date[calcIndex], zscore);
                count++;
                calcIndex++;
            }
            if(count != numZScores)
                logger.warn("Sanity check failed: count != numZScores ("+count+" != "+numZScores+")");
            logger.info("calculated "+count+" zscores\n");
        }
        database.insertZScores(symbol, zscores);

    }

    /**
     * Returns a map k=start_date v=end_date representing all the ranges present in our bundle of scenarios.
     *
     * Bonus: Reduces scenarios with the same starting date.
     */
    private Map<Integer, Integer> extractScenarioRanges() throws Exception {
        int scenarioCount = 0;
        Set<Scenario> scenarios = scenarioFactory.getScenarios(symbol);
        Map<Integer, Integer> ranges = new HashMap<Integer, Integer>();

        for(Scenario s : scenarios) {
            //logger.info(s);
            // new skool
            scenarioCount++;
            filterScenario(s.sampleStart, s.trackingEnd, ranges);


        }
        logger.info("["+symbol+"] reduced "+scenarioCount+" scenarios to "+ranges.size()+" ranges");
        logger.info("["+symbol+"] min="+minScenarioDate+", max="+maxScenarioDate);
        return ranges;
    }

    private void filterScenario(int start, int end, Map<Integer, Integer> ranges) {
        // find min/max to reduce data loading
        if (start < minScenarioDate || minScenarioDate == 0)
            minScenarioDate = start;
        if (end > maxScenarioDate)
            maxScenarioDate = end;

        if (ranges.containsKey(start)) {
            if (end > ranges.get(start)) {
                ranges.put(start, end);
            }
        } else {
            ranges.put(start, end);
        }
    }

    public void inMemSearch(int entryLimit, int exitLimit) throws Exception {
        Set<Scenario> scenarios = scenarioFactory.getScenarios(symbol);

        Map<Integer, ZScoreEntry> cache = new HashMap<Integer, ZScoreEntry>();

        for(Scenario scenario : scenarios) {
            logger.info("["+symbol+"] searching scenario: "+scenario);

            ZScoreEntry zscore;
            if(cache.containsKey(scenario.sampleStart)) {
                zscore = cache.get(scenario.sampleStart);
            } else {
                zscore = database.loadZScores(symbol, scenario.sampleStart);
            }
            if(zscore == null) {
                logger.debug("["+symbol+"]: No zscore data for scenario");
                SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            int iterations = 0;

            int idx = zscore.findClosestDateIndex(scenario.trackingStart);

            if(idx == -1) {
                logger.debug("["+symbol+"]: Not enough zscore data for scenario (no tracking start date)");
                SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            logger.info("ZScore Search, startDate="+zscore.date[idx]);

            while(idx < zscore.date.length) {

                if(zscore.zscore[idx] <= entryLimit) {
                    // find entry point
                    if (data == null)
                        data = database.loadData(symbol);


                    // have an entry point, note it down
                    EntryExitPair pair = new EntryExitPair(ResultCode.ENTRY);
                    pair.entryDate = zscore.date[idx];
                    pair.entryZScore = zscore.zscore[idx];


                    //pair.entryPrice = database.findClosePrice(symbol, pair.entryDate);
                    if (useAdjustedClose)
                        pair.entryClosePrice = data.findAdjustedClosePriceAtDate(pair.entryDate);
                    else
                        pair.entryClosePrice = data.findClosePriceAtDate(pair.entryDate);

                    data.avgVolumePrev30Days(pair.entryDate, pair.avgVolumePrev30);
                    data.avgPricePrev30Days(pair.entryDate, pair.avgPricePrev30, useAdjustedClose);
/*
                    if (idx > 0) {
                        pair.entryPrevDayDate = zscore.date[idx - 1];
                        if (useAdjustedClose)
                            pair.entryPrevDayPrice = data.findAdjustedClosePriceAtDate(pair.entryPrevDayDate);
                        else
                            pair.entryPrevDayPrice = data.findClosePriceAtDate(pair.entryPrevDayDate);
                    }

                    // find EMA
                    EMA ema;
                    if (useAdjustedClose) {
                        ema = new EMA(data.adjustedClose);
                    } else {
                        ema = new EMA(data.close);
                    }
                    int dataEntryIdx = data.findDateIndex(pair.entryDate);
                    try {
                        pair.ema50 = ema.calculate(50, dataEntryIdx);
                        pair.ema100 = ema.calculate(100, dataEntryIdx);
                        pair.ema200 = ema.calculate(200, dataEntryIdx);
                    } catch (EMAException e) {
                        logger.info(e.getMessage());
                    }

                    // find 60% of current
                    data.findPCIncreaseFromEntry(pair.entryDate, 100, pair.pc100Date, pair.pc100Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 90, pair.pc90Date, pair.pc90Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 80, pair.pc80Date, pair.pc80Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 70, pair.pc70Date, pair.pc70Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 60, pair.pc60Date, pair.pc60Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 50, pair.pc50Date, pair.pc50Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 40, pair.pc40Date, pair.pc40Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 30, pair.pc30Date, pair.pc30Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 20, pair.pc20Date, pair.pc20Price, useAdjustedClose);
                    data.findPCIncreaseFromEntry(pair.entryDate, 10, pair.pc10Date, pair.pc10Price, useAdjustedClose);
*/

                    /**
                     * 1. Look for the highest close price after entry and note this dollar amount
                     * 2. Provide the date of the price and
                     * 3. The z score on that day (if you already have the z score calcd it might help us)
                     */
/*
                    if (useAdjustedClose)
                        data.findMaxAdjustedClosePriceAfterEntry(pair);
                    else
                        data.findMaxClosePriceAfterEntry(pair);
                    if (pair.maxPriceDate != -1) {
                        logger.debug(String.format("[%s]: max price after entry %d: %f/%d", symbol, pair.entryDate, pair.maxPriceAfterEntry, pair.maxPriceDate));
                        pair.maxPriceZScore = zscore.findZScoreAtDate(pair.maxPriceDate);
                    }
*/

                    logger.debug(String.format("Found entry: %d/%f", pair.entryDate, pair.entryZScore));
                    SearchResults.addResult(database.getMarket(), symbol, scenario, pair);

                }

                /*

                if(zscore.zscore[idx] >= exitLimit) {
                    // find exit point

                    if (data == null)
                        data = database.loadData(symbol);

                    EntryExitPair pair = new EntryExitPair(ResultCode.EXIT);

                    pair.resultCode = ResultCode.EXIT;
                    pair.exitDate = zscore.date[idx];
                    pair.exitZScore = zscore.zscore[idx];
                    //pair.exitPrice = database.findClosePrice(symbol, pair.exitDate);
                    if (useAdjustedClose)
                        pair.exitPrice = data.findAdjustedClosePriceAtDate(pair.exitDate);
                    else
                        pair.exitPrice = data.findClosePriceAtDate(pair.exitDate);


                    logger.debug(String.format("Found exit: %d/%f", pair.exitDate, pair.exitZScore));
                    SearchResults.addResult(database.getMarket(), symbol, scenario, pair);
                }
                */


                // prepare for next run at it
                idx ++;
                iterations ++;
            }
        }
        //SearchResults.mergeResults(results);

    }
}
