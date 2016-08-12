package com.tvm.crunch;

import com.tvm.crunch.scenario.AbstractScenarioFactory;
import com.tvm.crunch.scenario.Scenario;
import com.tvm.crunch.scenario.ScenarioSet;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

/**
 * Wrap market data & search functions
 *
 * Created by horse on 18/11/14.
 */
public class Data {
    private static final Logger logger = LogManager.getLogger(Data.class);

    public int[] date;
    public double[] open;
    //public double[] high;
    //public double[] low;
    public double[] close;
    public long[] volume;
    public String symbol;
    public String market;
    //public double[] openInterest;


    public Data(String symbol, String market, int size) {
        this.symbol = symbol;
        this.market = market;
        date = new int[size];
        open = new double[size];
        //high = new double[size];
        //low = new double[size];
        close = new double[size];
        volume = new long[size];
        //openInterest = new double[size];
    }

    // verify distance between dates is less than a certain number of days
    private boolean verifyDateDistance(int idx, int targetDate, int days) {
        int foundDate = date[idx];
        int distance = 0;
        if(foundDate > targetDate)
            distance = DateUtil.distance(targetDate, foundDate);
        else
            distance = DateUtil.distance(foundDate, targetDate);

        return distance <= days;
    }

    private int distanceCheck(int idx, int targetDate, int days) {
        return verifyDateDistance(idx, targetDate, days) ? idx : -1;
    }

    public int findDateIndex(int findDate) {
        return findDateIndex(findDate, 30, true);
    }

    public int findDateIndex(int findDate, int maxDistanceDays, boolean softEnd) {
        if (date.length == 0) {
            //logger.debug("findDate[" + findDate + "] no data found");
            return -1;
        }
        int idx = Arrays.binarySearch(date, findDate);
        //System.out.println("binSearch["+findDate+"]: "+date[(idx<0?-idx:idx)-1]);
        if (idx >= 0) {
            return distanceCheck(idx, findDate, maxDistanceDays);
        } else {
            idx = (-idx) - 1;
            if (idx >= date.length) {
                // special case end of data
                if (softEnd) {
                    //logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return distanceCheck(date.length - 1, findDate, maxDistanceDays);
                } else {
                    return -1;
                }
            } else {
                // More usual case, some kind of gap in the data.
                // use the previous value
                if(idx != 0)
                    idx--;
                return distanceCheck(idx, findDate, maxDistanceDays);
            }
        }
    }

    public Point findPCIncrease(int entryIndex, int percent, double[] values) {
        double target = values[entryIndex] * (1.0 + ((double) percent / 100.0));
        while (++entryIndex < values.length) {
            if (values[entryIndex] >= target)
                return new Point(this, entryIndex);
        }
        return null;
    }

    public Point findPCDecrease(int entryIndex, int percent, double[] values) {
        double target = (values[entryIndex] * (1.0 - ((double) percent / 100.0)));
        while (++entryIndex < values.length) {
            if (values[entryIndex] <= target)
                return new Point(this, entryIndex);
        }
        return null;
    }


    public Point findNMonthPoint(int entryIndex, int months, int plusDays) {
        int idx;
        if(months < 0)
            idx = findDateIndex(DateUtil.minusMonths(date[entryIndex], -months), 14, false);
        else
            idx = findDateIndex(DateUtil.addMonths(date[entryIndex], months), 14, false);
        if (idx != -1) {
            idx += plusDays;
            if(idx > 0 && idx < date.length)
                return new Point(this, idx);
        }
        return null;
    }

    public Point findNWeekPoint(int entryIndex, int weeks, int plusDays) {
        int idx;
        if(weeks < 0)
            idx = findDateIndex(DateUtil.minusWeeks(date[entryIndex], -weeks), 7, false);
        else
            idx = findDateIndex(DateUtil.addWeeks(date[entryIndex], weeks), 7, false);
        if (idx != -1) {
            idx += plusDays;
            if(idx > 0 && idx < date.length)
                return new Point(this, idx);
        }
        return null;
    }

    public Point findNDayPoint(int entryIndex, int days) {
        // not literal days, trading days! as in data points.
        // why? because weekends, long weekends etc.
        entryIndex += days;
        if (entryIndex < 0 || entryIndex >= date.length)
            return null;
        return new Point(this, entryIndex);
    }

    public Point findMinPriceLimitMonth(int entryIndex, int months, double[] values) {
        return findMinMaxPriceLimitMonth(entryIndex, months, (a,b) -> { return a < b; }, values);
    }

    public Point findMaxPriceLimitMonth(int entryIndex, int months, double[] values) {
        return findMinMaxPriceLimitMonth(entryIndex, months, (a,b) -> { return a > b; }, values);

    }

    private interface Comparison {
        boolean compare(double a, double b);
    }

    private Point findMinMaxPriceLimitMonth(int entryIndex, int months, Comparison compare, double[] values) {
        if(entryIndex < 0 || months < 0 || values == null || entryIndex >= values.length)
            return null;

        int maxDate = DateUtil.addMonths(date[entryIndex], months);

        System.out.println(String.format("range: %d - %d", date[entryIndex], maxDate));

        int lastIndex = entryIndex;
        double lastPrice = values[entryIndex];

        while (entryIndex < values.length && date[entryIndex] <= maxDate) {
            if (compare.compare(values[entryIndex], lastPrice)) {
                lastPrice = values[entryIndex];
                lastIndex = entryIndex;
            }
            entryIndex++;
        }

        return new Point(this, lastIndex);
    }



    public Double avgVolumePrev30Days(int entryIndex) {
        return rangeAvg(entryIndex, -30, volume);
    }

    public Double avgPricePrev30Days(int entryIndex) {
        return rangeAvg(entryIndex, -30, close);
    }

    public Double avgVolumePost30Days(int entryIndex) {
        return rangeAvg(entryIndex, 30, volume);
    }

    public Double avgPricePost30Days(int entryIndex) {
        return rangeAvg(entryIndex, 30, close);
    }

    public Long totalVolumePrev30Days(int entryIdx) {
        return rangeSum(entryIdx, -30, volume);
    }

    public Double totalPricePrev30Days(int entryIdx) {
        return rangeSum(entryIdx, -30, close);
    }

    public Long totalVolumePost30Days(int entryIndex) {
        return rangeSum(entryIndex, 30, volume);
    }

    public Double totalPricePost30Days(int entryIdx) {
        return rangeSum(entryIdx, 30, close);
    }

    // UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY
    // I can't seem to get rangeSum()/rangeAvg() generic enough.
    private Long rangeSum(int entryIdx, int days, long[] values) {
        if(entryIdx < 0 || values == null || values.length == 0 || entryIdx >= values.length)
            return null;
        OptionalLong rv;
        rv = range(entryIdx, days, values).reduce(Long::sum);
        if(rv.isPresent())
            return rv.getAsLong();
        else
            return null;
    }

    private Double rangeSum(int entryIdx, int days, double[] values) {
        if(entryIdx < 0 || values == null || values.length == 0 || entryIdx >= values.length)
            return null;
        OptionalDouble rv;
        DoubleStream ds;
        rv = range(entryIdx, days, values).reduce(Double::sum);
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

    /*
    private Double rangeSum(int entryIdx, int days, double[] values) {
        if(entryIdx < 0 || values == null || values.length == 0 || entryIdx >= values.length)
            return null;
        OptionalDouble rv;
        rv = range(entryIdx, days, values).reduce(Double::sum);
        DoubleStream ds;
        if(days > 0)
            ds = range(entryIdx, DateUtil::addDays, days, values);
        else
            ds = range(entryIdx, DateUtil::minusDays, -days, values);
        rv = ds.reduce(Double::sum);
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }
    */

    private Double rangeAvg(int entryIdx, int days, long[] values) {
        if(entryIdx < 0 || values == null || values.length == 0 || entryIdx >= values.length)
            return null;
        OptionalDouble rv;
        rv = range(entryIdx, days, values).average();
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

    private Double rangeAvg(int entryIdx, int days, double[] values) {
        if(entryIdx < 0 || values == null || values.length == 0 || entryIdx >= values.length)
            return null;
        OptionalDouble rv;
        rv = range(entryIdx, days, values).average();
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

    // ugh, more ugly copying.
    private LongStream range(int entryIdx, int days, long[] values) {
        if(days > 0) {
            int endIdx = findDateIndex(DateUtil.addDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx++; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findDateIndex(DateUtil.minusDays(date[entryIdx], -days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                entryIdx++;
            return Arrays.stream(values, endIdx, entryIdx);
        } else {
            return Arrays.stream(values, entryIdx, entryIdx);
        }
    }

    private DoubleStream range(int entryIdx, int days, double[] values) {
        if(days > 0) {
            int endIdx = findDateIndex(DateUtil.addDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx++; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findDateIndex(DateUtil.minusDays(date[entryIdx], -days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                entryIdx++;
            //System.out.println("start="+date[endIdx]+" end="+date[entryIdx]);
            return Arrays.stream(values, endIdx, entryIdx);
        } else {
            return Arrays.stream(values, entryIdx, entryIdx);
        }
    }
    // END OF UGLYNESS
    // UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY



    public Point findEndOfYearPrice(int year) {
        int dt = DateUtil.findEndOfYearWeekDate(year*10000+0101);


        int idx = findDateIndex(dt, 30, false);
        if(idx == -1)
            return null;

        // sometimes the last trading date isn't the last working day, so the search will
        // produce a date in the next year.
        while(idx >= 0 && DateUtil.getYear(date[idx]) > year) {
            idx--;
        }

        if(idx < 0 || !verifyDateDistance(idx, dt, 28)) {
            return null;
        }

        return new Point(this, idx);

    }

    public static double ema(int startIdx, int days, double[] close) throws DataException {
        if(days <= 1) {
            //throw new EMAException("not enough days for meaningful EMA: "+days);
            throw new DataException();
        }
        if(days > startIdx + 1) {
            //throw new EMAException("not enough data to calculate "+days+"day EMA");
            throw new DataException();
        }

        double ema = 0.0;

        // start with SMA
        double prevDaysEMA = simpleMovingAverage(startIdx, days, close);

        // multiplier
        double k = (2 / (days + 1));

        for(int idx = startIdx - days + 1; idx <= startIdx; idx++) {
            ema = close[idx] * k + prevDaysEMA * (1 - k);
            prevDaysEMA = ema;
        }

        return ema;
    }

    public static Double simpleMovingAverage(int startIdx, int days, double[] data) throws DataException {
        double sum = 0.0;
        int idx = startIdx - days + 1;
        if(idx < 0)
            throw new DataException();;
        for (; idx <= startIdx; idx++) {
            sum += data[idx];
        }
        return sum / days;
    }

    public static Double simpleMovingAverage(int startIdx, int days, long[] data) throws DataException {
        double sum = 0.0;
        int idx = startIdx - days + 1;
        if(idx < 0)
            throw new DataException();
        for (; idx <= startIdx; idx++) {
            sum += data[idx];
        }
        return sum / days;
    }

    public static double slopeDaysPrev(int entryIdx, int days, int date[], double values[]) throws DataException {

        // NB "days" meaning changed to "data points"
        //int startIdx = findDateIndex(DateUtil.minusDays(entryDate, days));
        int startIdx = entryIdx - days + 1;
        if(startIdx < 0)
            throw new DataException();

        int cnt = entryIdx - startIdx + 1;

        if(cnt == 0)
            throw new DataException();

        double xy[] = new double[cnt];
        double x2[] = new double[cnt];
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumClose = 0.0;
        double sumDate = 0.0;

        for(int i = startIdx, j = 0; i <= entryIdx; i++, j++) {
            xy[j] = ((double)date[i]) * values[i];
            x2[j] = Math.pow(date[i], 2);

            sumXY += xy[j];
            sumX2 += x2[j];
            sumClose += values[i];
            sumDate += date[i];
        }

        /*
        double sumDatePow2 = Math.pow(sumDate, 2);

        double calcAA = (cnt * sumXY);
        double calcAB = (sumDate * sumClose);
        double calcA = calcAA - calcAB;

        double calcBA = (cnt * sumX2);
        double calcB = calcBA  - sumDatePow2;

        double slope =  calcA / calcB;
        */

        double slope = ((cnt * sumXY) - (sumDate * sumClose)) / ((cnt * sumX2) - Math.pow(sumDate, 2));

        if(!Double.isNaN(slope))
            return slope;
        else
            throw new DataException();
    }

    public double zscore(int entryIdx, int days) throws DataException {
        int startIdx = entryIdx - days + 1;
        int endIdx = entryIdx;

        if(startIdx < 0 || endIdx < 0)
            throw new DataException();

        SummaryStatistics stats = new SummaryStatistics();

        double zscore = 0;

        while(startIdx <= endIdx) {
            stats.addValue(close[startIdx]);

            double stdev = stats.getStandardDeviation();
            if(stdev == 0) {
                // either this is the first value or all initial values this far have had no variance (were the same)
                startIdx ++;
                continue;
            }
            double avg = stats.getMean();
            double closeValue;
            closeValue = close[startIdx];
            zscore = (closeValue - avg) / stdev;

            startIdx++;
        }

        return zscore;
    }

    public void zscore(AbstractScenarioFactory scenarioFactory, TriggerProcessor triggerProcessor) {
        Map<Integer, ScenarioSet> scenarios = scenarioFactory.getReducedScenarios();
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            for (final ScenarioSet ss : scenarios.values()) {
                executorService.submit(new Runnable() {
                    public void run() {
                        try {
                            zscore(ss, triggerProcessor);
                        } catch(Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void zscore(ScenarioSet ss, TriggerProcessor triggerProcessor) {
        int entryZScore = triggerProcessor.getEntryZScore();
        int exitZScore = triggerProcessor.getExitZScore();

        int startIdx = findDateIndex(ss.startDate);
        if(startIdx == -1) {
            //System.out.printf("Could not find start date %d [%s/%s]\n", ss.startDate, market, symbol);
            //System.exit(1);
            return;
        }

        SummaryStatistics stats = new SummaryStatistics();

        while(startIdx < date.length && date[startIdx] <= ss.maxDate) {
            stats.addValue(close[startIdx]);

            double stdev = stats.getStandardDeviation();
            if(stdev == 0) {
                // either this is the first value or all initial values this far have had no variance (were the same)
                startIdx ++;
                continue;
            }
            double avg = stats.getMean();
            double closeValue;
            closeValue = close[startIdx];
            final double zscore = (closeValue - avg) / stdev;

            if(zscore <= entryZScore) {
                int dt = date[startIdx];

                // with this code: 00:01:20.836
                for(Scenario s : ss.scenarios) {
                    if(dt >= s.trackingStart && dt <= s.trackingEnd)
                        triggerProcessor.processTrigger(this, startIdx, zscore, s);
                }
                /* with this code: 00:01:24.428
                final int tmpIdx = startIdx;
                ss.scenarios.stream()
                        .filter(s -> dt >= s.trackingStart && dt <= s.trackingEnd)
                        .forEach(s -> triggerProcessor.processTrigger(this, tmpIdx, zscore, s));
                 */
            }

            startIdx++;
        }
    }

}
