package com.tvm.crunch;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.TypeConverters;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalLong;
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
    //public double[] openInterest;


    public Data(int size) {
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
                /*
                // More usual case, some kind of gap in the data.
                // Find closest date, forward or backward and use that
                int backDistance = DateUtil.distance(date[idx-1], findDate);
                int frontDistance = DateUtil.distance(findDate, date[idx]);

                System.out.println(String.format("target=%d back=%d front=%d",
                        findDate, date[idx-1], date[idx]));

                System.out.println(String.format("backDistance=%d frontDistance=%d",
                        backDistance, frontDistance));
                // favour going back
                if(idx != 0 &&  backDistance <= frontDistance)
                    idx--;
                    */
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


    public Point findNMonthPoint(int entryIndex, int months) {
        int idx;
        if(months < 0)
            idx = findDateIndex(DateUtil.minusMonths(date[entryIndex], -months), 14, false);
        else
            idx = findDateIndex(DateUtil.addMonths(date[entryIndex], months), 14, false);
        if (idx != -1)
            return new Point(this, idx);
        return null;
    }

    public Point findNWeekPoint(int entryIndex, int weeks) {
        int idx;
        if(weeks < 0)
            idx = findDateIndex(DateUtil.minusWeeks(date[entryIndex], -weeks), 7, false);
        else
            idx = findDateIndex(DateUtil.addWeeks(date[entryIndex], weeks), 7, false);
        if (idx != -1)
            return new Point(this, idx);
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
        int endIdx = findDateIndex(DateUtil.addMonths(date[entryIndex], months));
        if (endIdx == -1)
            return null;

        int lastIndex = entryIndex;
        double lastPrice = values[entryIndex];


        while (entryIndex <= endIdx) {
            if (values[entryIndex] < lastPrice) {
                lastPrice = values[entryIndex];
                lastIndex = date[entryIndex];
            }
            entryIndex++;
        }

        if(lastIndex >= values.length)
            return null;

        return new Point(this, lastIndex);
    }

    public Point findMaxPriceLimitMonth(int entryIndex, int months, double[] values) {
        if(entryIndex < 0 || months < 0 || values == null || entryIndex >= values.length)
            return null;

        int endIdx = findDateIndex(DateUtil.addMonths(date[entryIndex], months));
        if (endIdx == -1) {
            System.out.println("bad end: "+date[entryIndex]+" + "+months);
            return null;
        }

        System.out.println(String.format("range: %d - %d", date[entryIndex], date[endIdx]));

        int lastIndex = entryIndex;
        double lastPrice = values[entryIndex];

        while (entryIndex <= endIdx) {
            if (values[entryIndex] > lastPrice) {
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
        rv = range(entryIdx, days, values).reduce(Double::sum);
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

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

    public static Double ema(int startIdx, int days, double[] close) {
        if(days <= 1) {
            //throw new EMAException("not enough days for meaningful EMA: "+days);
            return null;
        }
        if(days > startIdx + 1) {
            //throw new EMAException("not enough data to calculate "+days+"day EMA");
            return null;
        }

        double ema = 0.0;

        // start with SMA
        Double prevDaysEMA = simpleMovingAverage(startIdx, days, close);

        if(prevDaysEMA == null)
            return null;

        // multiplier
        double k = (2 / (days + 1));

        for(int idx = startIdx - days + 1; idx <= startIdx; idx++) {
            ema = close[idx] * k + prevDaysEMA * (1 - k);
            prevDaysEMA = ema;
        }

        return ema;
    }

    public static Double simpleMovingAverage(int startIdx, int days, double[] data) {
        double sum = 0.0;
        int idx = startIdx - days + 1;
        if(idx < 0)
            return null;
        for (; idx <= startIdx; idx++) {
            sum += data[idx];
        }
        return sum / days;
    }

    public static Double simpleMovingAverage(int startIdx, int days, long[] data) {
        double sum = 0.0;
        int idx = startIdx - days + 1;
        if(idx < 0)
            return null;
        for (; idx <= startIdx; idx++) {
            sum += data[idx];
        }
        return sum / days;
    }

    public static Double slopeDaysPrev(int entryIdx, int days, int date[], double values[]) {

        // NB "days" meaning changed to "data points"
        //int startIdx = findDateIndex(DateUtil.minusDays(entryDate, days));
        int startIdx = entryIdx - days + 1;
        if(startIdx < 0)
            return null;

        int cnt = entryIdx - startIdx + 1;

        if(cnt == 0)
            return null;

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
            return null;
    }

    public Double zscore(int entryIdx, int days) {
        int startIdx = entryIdx - days + 1;
        int endIdx = entryIdx;

        if(startIdx < 0 || endIdx < 0)
            return null;

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

            //logger.debug(String.format("z: %d, %f, ci=%d, cei=%d", data.date[calcIndex], zscore, calcIndex, calcEndIndex));

            startIdx++;
        }

        return zscore;
    }

}
