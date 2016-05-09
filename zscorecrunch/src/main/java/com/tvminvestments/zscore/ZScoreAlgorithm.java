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
    private final SearchResults searchResults;
    private Map<Integer, ZScoreEntry> zscores;

    private int minScenarioDate;
    private int maxScenarioDate;

    // lazy load data - check data bounds vs max zscore
    private CloseData data = null;
    private boolean useAdjustedClose;


    public ZScoreAlgorithm(String symbol, Database database, AbstractScenarioFactory scenarioFactory, SearchResults searchResults) {
        this.searchResults = searchResults;
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

    public int inMemSearch(double entryLimit, double exitLimit) throws Exception {
        Set<Scenario> scenarios = scenarioFactory.getScenarios(symbol);
        int count = 0;

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
                //SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            int iterations = 0;
            boolean go = true;
            int idx = zscore.findClosestDateIndex(scenario.trackingStart);

            if(idx == -1) {
                logger.debug("["+symbol+"]: Not enough zscore data for scenario (no tracking start date)");
                //SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                continue;
            }

            logger.debug("ZScore Search, startDate="+zscore.date[idx]);

            while(go) {

                if(idx >= zscore.date.length || zscore.date[idx] > scenario.trackingEnd) {
                    if(iterations == 0) {
                        //SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                    }
                    go = false;
                    continue;
                }

                // find entry point
                int entryIdx = zscore.findIndexOfZScoreLTE(idx, entryLimit, scenario.trackingEnd);
                if(entryIdx == -1) {
                    if(iterations == 0) {
                        //SearchResults.addResult(database.getMarket(), symbol, scenario, new EntryExitPair(ResultCode.NO_ENTRY));
                    }
                    go = false;
                    continue;
                }

                if(data == null)
                    data = database.loadData(symbol);


                // have an entry point, note it down
                EntryExitPair pair = new EntryExitPair(ResultCode.ENTRY);
                pair.entryDate = zscore.date[entryIdx];
                pair.entryZScore = zscore.zscore[entryIdx];
                //pair.entryPrice = database.findClosePrice(symbol, pair.entryDate);
                if(useAdjustedClose)
                    pair.entryClosePrice = data.findAdjustedClosePriceAtDate(pair.entryDate);
                else
                    pair.entryClosePrice = data.findClosePriceAtDate(pair.entryDate);

                pair.entryOpenPrice = data.findOpenPriceAtDate(pair.entryDate);

                data.avgVolumePrev30Days(pair.entryDate, pair.avgVolumePrev30);
                data.avgPricePrev30Days(pair.entryDate, pair.avgPricePrev30, useAdjustedClose);


                data.findPCIncreaseOpen(pair.entryDate, 1, pair.pc1OpenDate, pair.pc1OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 2, pair.pc2OpenDate, pair.pc2OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 3, pair.pc3OpenDate, pair.pc3OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 4, pair.pc4OpenDate, pair.pc4OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 5, pair.pc5OpenDate, pair.pc5OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 6, pair.pc6OpenDate, pair.pc6OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 7, pair.pc7OpenDate, pair.pc7OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 8, pair.pc8OpenDate, pair.pc8OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 9, pair.pc9OpenDate, pair.pc9OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 10, pair.pc10OpenDate, pair.pc10OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 15, pair.pc15OpenDate, pair.pc15OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 20, pair.pc20OpenDate, pair.pc20OpenPrice);
                data.findPCIncreaseOpen(pair.entryDate, 25, pair.pc25OpenDate, pair.pc25OpenPrice);


                data.findEndOfYearPrice(19941231, new AtomicInteger(0), pair.eoy1994ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(19951231, new AtomicInteger(0), pair.eoy1995ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(19961231, new AtomicInteger(0), pair.eoy1996ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(19971231, new AtomicInteger(0), pair.eoy1997ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(19981231, new AtomicInteger(0), pair.eoy1998ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(19991231, new AtomicInteger(0), pair.eoy1999ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20001231, new AtomicInteger(0), pair.eoy2000ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20011231, new AtomicInteger(0), pair.eoy2001ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20021231, new AtomicInteger(0), pair.eoy2002ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20031231, new AtomicInteger(0), pair.eoy2003ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20041231, new AtomicInteger(0), pair.eoy2004ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20051231, new AtomicInteger(0), pair.eoy2005ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20061231, new AtomicInteger(0), pair.eoy2006ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20071231, new AtomicInteger(0), pair.eoy2007ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20081231, new AtomicInteger(0), pair.eoy2008ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20091231, new AtomicInteger(0), pair.eoy2009ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20101231, new AtomicInteger(0), pair.eoy2010ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20111231, new AtomicInteger(0), pair.eoy2011ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20121231, new AtomicInteger(0), pair.eoy2012ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20131231, new AtomicInteger(0), pair.eoy2013ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20141231, new AtomicInteger(0), pair.eoy2014ClosePrice, useAdjustedClose);
                data.findEndOfYearPrice(20151231, new AtomicInteger(0), pair.eoy2015ClosePrice, useAdjustedClose);


                data.findOpenNDaysLater(pair.entryDate, 0, pair.next0DayOpenDate, pair.next0DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 1, pair.next1DayOpenDate, pair.next1DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 2, pair.next2DayOpenDate, pair.next2DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 3, pair.next3DayOpenDate, pair.next3DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 4, pair.next4DayOpenDate, pair.next4DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 5, pair.next5DayOpenDate, pair.next5DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 6, pair.next6DayOpenDate, pair.next6DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 7, pair.next7DayOpenDate, pair.next7DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 8, pair.next8DayOpenDate, pair.next8DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 9, pair.next9DayOpenDate, pair.next9DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 10, pair.next10DayOpenDate, pair.next10DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 11, pair.next11DayOpenDate, pair.next11DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 12, pair.next12DayOpenDate, pair.next12DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 13, pair.next13DayOpenDate, pair.next13DayOpenPrice);
                data.findOpenNDaysLater(pair.entryDate, 14, pair.next14DayOpenDate, pair.next14DayOpenPrice);

                int rsiEntryIdx = data.findDateIndex(pair.entryDate);
                RSI rsi7 = new RSI(7, data.close);
                rsi7.calculate(rsiEntryIdx + 0, pair.rsi7_0Days);
                rsi7.calculate(rsiEntryIdx + 1, pair.rsi7_1Days);
                rsi7.calculate(rsiEntryIdx + 2, pair.rsi7_2Days);
                rsi7.calculate(rsiEntryIdx + 3, pair.rsi7_3Days);
                rsi7.calculate(rsiEntryIdx + 4, pair.rsi7_4Days);
                rsi7.calculate(rsiEntryIdx + 5, pair.rsi7_5Days);
                rsi7.calculate(rsiEntryIdx + 6, pair.rsi7_6Days);
                rsi7.calculate(rsiEntryIdx + 7, pair.rsi7_7Days);
                rsi7.calculate(rsiEntryIdx + 8, pair.rsi7_8Days);
                rsi7.calculate(rsiEntryIdx + 9, pair.rsi7_9Days);
                rsi7.calculate(rsiEntryIdx + 10, pair.rsi7_10Days);
                rsi7.calculate(rsiEntryIdx + 11, pair.rsi7_11Days);
                rsi7.calculate(rsiEntryIdx + 12, pair.rsi7_12Days);
                rsi7.calculate(rsiEntryIdx + 13, pair.rsi7_13Days);
                rsi7.calculate(rsiEntryIdx + 14, pair.rsi7_14Days);

                RSI rsi14 = new RSI(14, data.close);
                rsi14.calculate(rsiEntryIdx + 0, pair.rsi14_0Days);
                rsi14.calculate(rsiEntryIdx + 1, pair.rsi14_1Days);
                rsi14.calculate(rsiEntryIdx + 2, pair.rsi14_2Days);
                rsi14.calculate(rsiEntryIdx + 3, pair.rsi14_3Days);
                rsi14.calculate(rsiEntryIdx + 4, pair.rsi14_4Days);
                rsi14.calculate(rsiEntryIdx + 5, pair.rsi14_5Days);
                rsi14.calculate(rsiEntryIdx + 6, pair.rsi14_6Days);
                rsi14.calculate(rsiEntryIdx + 7, pair.rsi14_7Days);
                rsi14.calculate(rsiEntryIdx + 8, pair.rsi14_8Days);
                rsi14.calculate(rsiEntryIdx + 9, pair.rsi14_9Days);
                rsi14.calculate(rsiEntryIdx + 10, pair.rsi14_10Days);
                rsi14.calculate(rsiEntryIdx + 11, pair.rsi14_11Days);
                rsi14.calculate(rsiEntryIdx + 12, pair.rsi14_12Days);
                rsi14.calculate(rsiEntryIdx + 13, pair.rsi14_13Days);
                rsi14.calculate(rsiEntryIdx + 14, pair.rsi14_14Days);

                searchResults.addResult(database.getMarket(), symbol, scenario, pair);
                count++;

                // prepare for next run at it
                idx = entryIdx + 1;
                iterations ++;
            }
        }
        //SearchResults.mergeResults(results);

        return count;

    }
}
