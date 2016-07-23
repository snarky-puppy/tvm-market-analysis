package com.tvm.crunch;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Wrap market data
 *
 * Created by horse on 18/11/14.
 */
public class Data {
    private static final Logger logger = LogManager.getLogger(Data.class);
    public final String symbol;
    public final String market;

    public int[] date;
    public double[] open;
    //public double[] high;
    //public double[] low;
    public double[] close;
    public long[] volume;
    //public double[] openInterest;


    public Data(String market, String symbol, int size) {
        date = new int[size];
        open = new double[size];
        //high = new double[size];
        //low = new double[size];
        close = new double[size];
        volume = new long[size];
        //openInterest = new double[size];


        this.symbol = symbol;
        this.market = market;
    }

    // verify distance between dates is less than a certain number of days
    private int verifyDateDistance(int idx, int targetDate, int days) {
        int foundDate = date[idx];
        int distance = 0;
        if(foundDate > targetDate)
            distance = DateUtil.distance(targetDate, foundDate);
        else
            distance = DateUtil.distance(foundDate, targetDate);

        if(distance <= days)
            return idx;
        else
            return -1;
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
        if (idx >= 0) {
            return verifyDateDistance(idx, findDate, maxDistanceDays);
        } else {
            idx = (-idx) - 1;

            if (idx >= date.length) {
                if (softEnd) {
                    //logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return verifyDateDistance(date.length - 1, findDate, maxDistanceDays);
                } else {
                    return -1;
                }
            } else {
                //logger.debug("findDate[" + findDate + "] not found, next data date: " + date[idx] + " (idx=" + idx + ")");
                return verifyDateDistance(idx, findDate, maxDistanceDays);
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
        int endIdx = findDateIndex(DateUtil.addMonths(date[entryIndex], months));
        if (endIdx == -1)
            return null;

        int lastIndex = entryIndex;
        double lastPrice = values[entryIndex];


        while (entryIndex <= endIdx) {
            if (values[entryIndex] > lastPrice) {
                lastPrice = values[entryIndex];
                lastIndex = date[entryIndex];
            }
            entryIndex++;
        }

        if(lastIndex >= values.length)
            return null;

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
        OptionalLong rv;
        rv = range(entryIdx, days, values).reduce(Long::sum);
        if(rv.isPresent())
            return rv.getAsLong();
        else
            return null;
    }

    private Double rangeSum(int entryIdx, int days, double[] values) {
        OptionalDouble rv;
        rv = range(entryIdx, days, values).reduce(Double::sum);
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

    private Double rangeAvg(int entryIdx, int days, long[] values) {
        OptionalDouble rv;
        rv = range(entryIdx, days, values).average();
        if(rv.isPresent())
            return rv.getAsDouble();
        else
            return null;
    }

    private Double rangeAvg(int entryIdx, int days, double[] values) {
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
                endIdx = entryIdx; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findDateIndex(DateUtil.minusDays(date[entryIdx], -days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = endIdx;
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
                endIdx = entryIdx; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findDateIndex(DateUtil.minusDays(date[entryIdx], -days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = endIdx;
            return Arrays.stream(values, endIdx, entryIdx);
        } else {
            return Arrays.stream(values, entryIdx, entryIdx);
        }
    }
    // END OF UGLYNESS
    // UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY


    public Point findEndOfYearPriceOfIndex(int entryIdx) {
        int dt = DateUtil.findEndOfYearWeekDate(date[entryIdx]);
        int idx = findDateIndex(dt, 7, false);
        if(idx != -1) {
            return new Point(this, idx);
        } else
            return null;
    }

    public Point findEndOfYearPrice(int year) {
        int dt = DateUtil.findEndOfYearWeekDate(year*10000+0101);


        int idx = findDateIndex(dt, 7, false);

        //System.out.println("year="+year+" dt="+dt+" date[idx]="+date[idx]+" idx="+idx);

        if(idx != -1) {
            return new Point(this, idx);
        } else
            return null;
    }

    public static double ema(int startIdx, int days, double[] close) {
        if(days <= 1) {
            //throw new EMAException("not enough days for meaningful EMA: "+days);
            return -1;
        }
        if(days > startIdx + 1) {
            //throw new EMAException("not enough data to calculate "+days+"day EMA");
            return -1;
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

    public static double simpleMovingAverage(int startIdx, int days, double[] data) {
        double sum = 0.0;
        for (int i = startIdx - days + 1; i <= startIdx ; i++) {
            sum += data[i];
        }
        return sum / days;
    }

    public static double simpleMovingAverage(int startIdx, int days, long[] data) {
        double sum = 0.0;
        for (int i = startIdx - days + 1; i <= startIdx ; i++) {
            sum += data[i];
        }
        return sum / days;
    }

    public static Double slopeDaysPrev(int entryIdx, int days, int date[], double values[]) {

        // NB "days" meaning changed to "data points"
        //int startIdx = findDateIndex(DateUtil.minusDays(entryDate, days));
        int startIdx = entryIdx - days;
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
        int startIdx = findDateIndex(DateUtil.minusDays(date[entryIdx], days));
        int endIdx = findDateIndex(date[entryIdx]);

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
