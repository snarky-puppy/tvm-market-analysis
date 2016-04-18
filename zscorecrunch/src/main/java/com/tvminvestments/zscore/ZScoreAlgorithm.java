package com.tvminvestments.zscore;


import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.Scenario;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Algorithm container class
 *
 * Created by horse on 13/11/14.
 */
public class ZScoreAlgorithm {
    private static final Logger logger = LogManager.getLogger(ZScoreAlgorithm.class);

    private final String symbol;
    private final AbstractScenarioFactory scenarioFactory;
    private final Database database;
    private Map<Integer, ZScoreEntry> zscores;

    private int minScenarioDate;
    private int maxScenarioDate;

    // lazy load data - check data bounds vs max zscore
    private CloseData data = null;
    private boolean useAdjustedClose;


    public ZScoreAlgorithm(String symbol, Database database, AbstractScenarioFactory scenarioFactory) {
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
        zscores = new HashMap<Integer, ZScoreEntry>();

        logger.debug("["+symbol+"] data bounds: "+bounds.getMin()+"-"+bounds.getMax());


        for(int startDate : ranges.keySet()) {
            int count = 0;
            int endDate = ranges.get(startDate);

            logger.debug("["+symbol+"] scenario range: "+startDate+"-"+endDate);

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

            logger.debug("["+symbol+"] start zscore calc from: "+zscoreMaxDate);

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
            logger.debug("len="+data.date.length);
            logger.debug("["+symbol+"] loaded data: "+data.date[0]+"-"+data.date[data.date.length-1]);


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

            logger.debug(String.format("calcIndex=%d, calcEndIndex=%d", calcIndex, calcEndIndex));
            logger.debug(String.format("from=%d, to=%d", data.date[calcIndex], data.date[calcEndIndex]));


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
        logger.debug("["+symbol+"] reduced "+scenarioCount+" scenarios to "+ranges.size()+" ranges");
        logger.debug("["+symbol+"] min="+minScenarioDate+", max="+maxScenarioDate);
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

    public void inMemSearch(double entryLimit, double exitLimit) throws Exception {
        Set<Scenario> scenarios = scenarioFactory.getScenarios(symbol);

        for(Scenario scenario : scenarios) {
            logger.info("["+symbol+"] searching scenario: "+scenario);

            ZScoreEntry zscore;
            if(zscores.containsKey(scenario.sampleStart)) {
                zscore = zscores.get(scenario.sampleStart);
            } else {
                zscore = database.loadZScores(symbol, scenario.sampleStart);
            }
            if(zscore == null) {
                logger.debug("["+symbol+"]: No zscore data for scenario");
                SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            int iterations = 0;
            boolean go = true;
            int idx = zscore.findClosestDateIndex(scenario.trackingStart);

            if(idx == -1) {
                logger.debug("["+symbol+"]: Not enough zscore data for scenario (no tracking start date)");
                SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            logger.debug("ZScore Search, startDate="+zscore.date[idx]);

            while(go) {

                if(idx >= zscore.date.length || zscore.date[idx] > scenario.trackingEnd) {
                    if(iterations == 0) {
                        SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                    }
                    go = false;
                    continue;
                }

                // find entry point
                int entryIdx = zscore.findIndexOfZScoreLTE(idx, entryLimit, scenario.trackingEnd);
                if(entryIdx == -1) {
                    if(iterations == 0) {
                        SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                    }
                    go = false;
                    continue;
                }

                if(data == null)
                    data = database.loadData(symbol);


                // have an entry point, note it down
                EntryExitPair pair = new EntryExitPair(ResultCode.ENTRY_NO_EXIT);
                pair.entryDate = zscore.date[entryIdx];
                pair.entryZScore = zscore.zscore[entryIdx];
                //pair.entryPrice = database.findClosePrice(symbol, pair.entryDate);
                if(useAdjustedClose)
                    pair.entryClosePrice = data.findAdjustedClosePriceAtDate(pair.entryDate);
                else
                    pair.entryClosePrice = data.findClosePriceAtDate(pair.entryDate);
                pair.entryOpenPrice = data.findOpenPriceAtDate(pair.entryDate);
                if(entryIdx > 0) {
                    pair.entryPrevDayDate = zscore.date[entryIdx-1];
                    if(useAdjustedClose)
                        pair.entryPrevDayPrice = data.findAdjustedClosePriceAtDate(pair.entryPrevDayDate);
                    else
                        pair.entryPrevDayPrice = data.findClosePriceAtDate(pair.entryPrevDayDate);
                }

                // find EMA
                EMA ema;
                if(useAdjustedClose) {
                    ema = new EMA(data.adjustedClose);
                } else {
                    ema = new EMA(data.close);
                }
                int dataEntryIdx = data.findDateIndex(pair.entryDate);
                try {
                    pair.ema50 = ema.calculate(50, dataEntryIdx);
                    pair.ema100 = ema.calculate(100, dataEntryIdx);
                    pair.ema200 = ema.calculate(200, dataEntryIdx);
                } catch(EMAException e) {
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

                data.findPCDecreaseFromEntry(pair.entryDate, 5, 1, pair.pcDec5Date, pair.pcDec5Price, useAdjustedClose);
                data.findPCDecreaseFromEntry(pair.entryDate, 10, 1, pair.pcDec10Date, pair.pcDec10Price, useAdjustedClose);
                data.findPCDecreaseFromEntry(pair.entryDate, 15, 1, pair.pcDec15Date, pair.pcDec15Price, useAdjustedClose);
                data.findPCDecreaseFromEntry(pair.entryDate, 20, 1, pair.pcDec20Date, pair.pcDec20Price, useAdjustedClose);
                data.findPCDecreaseFromEntry(pair.entryDate, 25, 1, pair.pcDec25Date, pair.pcDec25Price, useAdjustedClose);
                data.findPCDecreaseFromEntry(pair.entryDate, 30, 1, pair.pcDec30Date, pair.pcDec30Price, useAdjustedClose);

                data.findMinPriceFromEntry(pair.entryDate, 1, pair.entry1MonthMinDate, pair.entry1MonthMinPrice, useAdjustedClose);
                data.findMaxPriceFromEntry(pair.entryDate, 1, pair.entry1MonthMaxDate, pair.entry1MonthMaxPrice, useAdjustedClose);


                data.findNMonthData(1, pair.entryDate, pair.ex1Date, pair.ex1Price, useAdjustedClose);
                data.findNMonthData(2, pair.entryDate, pair.ex2Date, pair.ex2Price, useAdjustedClose);
                data.findNMonthData(3, pair.entryDate, pair.ex3Date, pair.ex3Price, useAdjustedClose);
                data.findNMonthData(4, pair.entryDate, pair.ex4Date, pair.ex4Price, useAdjustedClose);
                data.findNMonthData(5, pair.entryDate, pair.ex5Date, pair.ex5Price, useAdjustedClose);
                data.findNMonthData(6, pair.entryDate, pair.ex6Date, pair.ex6Price, useAdjustedClose);
                data.findNMonthData(7, pair.entryDate, pair.ex7Date, pair.ex7Price, useAdjustedClose);
                data.findNMonthData(8, pair.entryDate, pair.ex8Date, pair.ex8Price, useAdjustedClose);
                data.findNMonthData(9, pair.entryDate, pair.ex9Date, pair.ex9Price, useAdjustedClose);
                data.findNMonthData(10, pair.entryDate, pair.ex10Date, pair.ex10Price, useAdjustedClose);
                data.findNMonthData(11, pair.entryDate, pair.ex11Date, pair.ex11Price, useAdjustedClose);
                data.findNMonthData(12, pair.entryDate, pair.ex12Date, pair.ex12Price, useAdjustedClose);

                data.avgVolumePrev30Days(pair.entryDate, pair.avgVolumePrev30);
                data.avgPricePrev30Days(pair.entryDate, pair.avgPricePrev30, useAdjustedClose);
                data.avgVolumePost30Days(pair.entryDate, pair.avgVolumePost30);
                data.avgPricePost30Days(pair.entryDate, pair.avgPricePost30, useAdjustedClose);

                data.totalVolumePrev30Days(pair.entryDate, pair.totalVolumePrev30);
                data.totalPricePrev30Days(pair.entryDate, pair.totalPricePrev30, useAdjustedClose);
                data.totalVolumePost30Days(pair.entryDate, pair.totalVolumePost30);
                data.totalPricePost30Days(pair.entryDate, pair.totalPricePost30, useAdjustedClose);

                data.slopeDaysPrev(30, pair.entryDate, pair.slope30, useAdjustedClose);
                data.slopeDaysPrev(3, pair.entryDate, pair.slope3, useAdjustedClose);

                data.find2DaysLater(pair.entryDate, pair.day2Date, pair.day2Price, useAdjustedClose);
                if(pair.day2Date.get() != 0) {
                    data.findPCIncreaseFromEntry(pair.day2Date.get(), 10, pair.day2PC10Date, pair.day2PC10Price, useAdjustedClose);
                }

                data.find1DayLater(pair.entryDate, pair.entryNextDayOpenDate, pair.entryNextDayOpenPrice, pair.entryNextDayClosePrice, useAdjustedClose);

                data.findPCIncreaseFromEntry(pair.entryDate, 10, pair.pco10Date, pair.pco10Price, pair.pco10NextDayDate, pair.pco10NextDayOpenPrice, pair.pco10NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 11, pair.pco11Date, pair.pco11Price, pair.pco11NextDayDate, pair.pco11NextDayOpenPrice, pair.pco11NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 12, pair.pco12Date, pair.pco12Price, pair.pco12NextDayDate, pair.pco12NextDayOpenPrice, pair.pco12NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 13, pair.pco13Date, pair.pco13Price, pair.pco13NextDayDate, pair.pco13NextDayOpenPrice, pair.pco13NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 14, pair.pco14Date, pair.pco14Price, pair.pco14NextDayDate, pair.pco14NextDayOpenPrice, pair.pco14NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 15, pair.pco15Date, pair.pco15Price, pair.pco15NextDayDate, pair.pco15NextDayOpenPrice, pair.pco15NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 16, pair.pco16Date, pair.pco16Price, pair.pco16NextDayDate, pair.pco16NextDayOpenPrice, pair.pco16NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 17, pair.pco17Date, pair.pco17Price, pair.pco17NextDayDate, pair.pco17NextDayOpenPrice, pair.pco17NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 18, pair.pco18Date, pair.pco18Price, pair.pco18NextDayDate, pair.pco18NextDayOpenPrice, pair.pco18NextDayClosePrice, useAdjustedClose);
                data.findPCIncreaseFromEntry(pair.entryDate, 19, pair.pco19Date, pair.pco19Price, pair.pco19NextDayDate, pair.pco19NextDayOpenPrice, pair.pco19NextDayClosePrice, useAdjustedClose);

                data.findNWeekData(1, pair.entryDate, pair.week1Date, pair.week1Price, pair.week1NextDayDate, pair.week1NextDayOpenPrice, pair.week1NextDayClosePrice, useAdjustedClose);
                data.findNWeekData(2, pair.entryDate, pair.week2Date, pair.week2Price, pair.week2NextDayDate, pair.week2NextDayOpenPrice, pair.week2NextDayClosePrice, useAdjustedClose);
                data.findNWeekData(3, pair.entryDate, pair.week3Date, pair.week3Price, pair.week3NextDayDate, pair.week3NextDayOpenPrice, pair.week3NextDayClosePrice, useAdjustedClose);
                data.findNWeekData(4, pair.entryDate, pair.week4Date, pair.week4Price, pair.week4NextDayDate, pair.week4NextDayOpenPrice, pair.week4NextDayClosePrice, useAdjustedClose);

                data.findEndOfYearPrice(pair.entryDate, pair.endOfYearDate, pair.endOfYearPrice, useAdjustedClose);

                logger.debug(String.format("Found entry: %d/%f", pair.entryDate, pair.entryZScore));

                /**
                 * 1. Look for the highest close price after entry and note this dollar amount
                 * 2. Provide the date of the price and
                 * 3. The z score on that day (if you already have the z score calcd it might help us)
                 */
                if(useAdjustedClose)
                    data.findMaxAdjustedClosePriceAfterEntry(pair);
                else
                    data.findMaxClosePriceAfterEntry(pair);
                if(pair.maxPriceDate != -1) {
                    logger.debug(String.format("[%s]: max price after entry %d: %f/%d", symbol, pair.entryDate, pair.maxPriceAfterEntry, pair.maxPriceDate));
                    pair.maxPriceZScore = zscore.findZScoreAtDate(pair.maxPriceDate);
                }


                // find exit point
                int exitIdx = zscore.findIndexOfZScoreGTE(entryIdx, exitLimit, scenario.trackingEnd);
                if(exitIdx <= 0) {

                    exitIdx = -exitIdx;
                    pair.exitDate = zscore.date[exitIdx];
                    pair.exitZScore = zscore.zscore[exitIdx];
                    //pair.exitPrice = database.findClosePrice(symbol, pair.exitDate);
                    if(useAdjustedClose)
                        pair.exitPrice = data.findAdjustedClosePriceAtDate(pair.exitDate);
                    else
                        pair.exitPrice = data.findClosePriceAtDate(pair.exitDate);

                    logger.debug(String.format("No exit: %d/%f", pair.exitDate, pair.exitZScore));

                    SearchResults.addResult(database.getMarket(), symbol, scenario, pair);
                    go = false;
                    continue;
                }

                pair.resultCode = ResultCode.ENTRY_EXIT;
                pair.exitDate = zscore.date[exitIdx];
                pair.exitZScore = zscore.zscore[exitIdx];
                //pair.exitPrice = database.findClosePrice(symbol, pair.exitDate);
                if(useAdjustedClose)
                    pair.exitPrice = data.findAdjustedClosePriceAtDate(pair.exitDate);
                else
                    pair.exitPrice = data.findClosePriceAtDate(pair.exitDate);


                logger.debug(String.format("Found exit: %d/%f", pair.exitDate, pair.exitZScore));

                SearchResults.addResult(database.getMarket(), symbol, scenario, pair);

                // prepare for next run at it
                idx = exitIdx;
                iterations ++;
            }
        }
        //SearchResults.mergeResults(results);

    }
}
