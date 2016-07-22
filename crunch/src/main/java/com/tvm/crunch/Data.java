package com.tvm.crunch;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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
    public int[] volume;
    //public double[] openInterest;


    public Data(String market, String symbol, int size) {
        date = new int[size];
        open = new double[size];
        //high = new double[size];
        //low = new double[size];
        close = new double[size];
        volume = new int[size];
        //openInterest = new double[size];


        this.symbol = symbol;
        this.market = market;
    }

    // verify distance
    private int verifyDateDistance(int idx, int targetDate, int days) {
        int foundDate = date[idx];
        int distance = 0;
        if (foundDate > targetDate)
            distance = DateUtil.distance(targetDate, foundDate);
        else
            distance = DateUtil.distance(foundDate, targetDate);

        if (distance <= days)
            return idx;
        else
            return -1;
    }

    public int findDateIndex(int startDate) {
        return findDateIndex(startDate, true);
    }

    public int findDateIndex(int findDate, boolean softEnd) {
        if (date.length == 0) {
            logger.debug("findDate[" + findDate + "] no data found");
            return -1;
        }
        int idx = Arrays.binarySearch(date, findDate);
        if (idx >= 0) {
            return idx;
        } else {
            idx = (-idx) - 1;

            if (idx >= date.length) {
                if (softEnd) {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return date.length - 1;
                } else {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning -1.");
                    return -1;
                }
            } else {
                logger.debug("findDate[" + findDate + "] not found, next data date: " + date[idx] + " (idx=" + idx + ")");
                return idx;
            }
        }
    }

    public int findVerifiedDateIndex(int findDate) {
        return findVerifiedDateIndex(findDate, 30, true);
    }

    public int findVerifiedDateIndex(int findDate, int maxDistanceDays, boolean softEnd) {
        if (date.length == 0) {
            logger.debug("findDate[" + findDate + "] no data found");
            return -1;
        }
        int idx = Arrays.binarySearch(date, findDate);
        if (idx >= 0) {
            return verifyDateDistance(idx, findDate, maxDistanceDays);
        } else {
            idx = (-idx) - 1;

            if (idx >= date.length) {
                if (softEnd) {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return verifyDateDistance(date.length - 1, findDate, maxDistanceDays);
                } else {
                    return -1;
                }
            } else {
                logger.debug("findDate[" + findDate + "] not found, next data date: " + date[idx] + " (idx=" + idx + ")");
                return verifyDateDistance(idx, findDate, maxDistanceDays);
            }
        }
    }


    public void sanity() {
        assert (date.length == close.length);
        assert (close.length == volume.length);
    }

    private int findPCIncreaseIndex(int entryIndex, int percent, double[] values) {
        double target = values[entryIndex] * (1.0 + ((double) percent / 100.0));
        while (++entryIndex < values.length) {
            if (values[entryIndex] >= target)
                return entryIndex;
        }
        return -1;
    }

    public Point findPCIncrease(int entryIndex, int percent, double[] values) {
        int idx = findPCIncreaseIndex(entryIndex, percent, values);
        if(idx != -1)
            return new Point(this, idx);
        return null;
    }


    public void findPCDecreaseFromEntry(int entryDate, int pc, int months, AtomicInteger pcDate, AtomicDouble pcPrice, boolean useAdjustedClose) {
        int idx = findPCDecreaseFromEntryIndex(pc, entryDate, months, useAdjustedClose);
        if (idx == -1) {
            pcPrice.set(0.0);
            pcDate.set(0);
        } else {
            pcDate.set(date[idx]);

            pcPrice.set(close[idx]);
        }
    }

    private int findPCDecreaseFromEntryIndex(int pc, int entryDate, int months, boolean useAdjustedClose) {
        int plusOneMonth = DateUtil.addMonths(entryDate, months);
        int idx = findVerifiedDateIndex(entryDate);
        if (idx == -1)
            return -1;
        if (date[idx] > plusOneMonth)
            return -1;
        double[] arr =   close;
        double target = (arr[idx] * (1.0 - ((double) pc / 100.0)));
        logger.debug(String.format("findPCDecrease [%d]: entry price=%f, target=%f", pc, arr[idx], target));
        while (++idx < arr.length) {
            //logger.info(String.format("%f <= target", arr[idx]));
            if (arr[idx] <= target)
                return idx;
        }
        return -1;
    }





    public void findNMonthData(int month, int entryDate, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, month), 14, false);
        if (idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);

                price.set(close[idx]);
        }
    }

    public void findNWeekData(int week, int entryDate, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addWeeks(entryDate, week), 7, false);
        if (idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);

                price.set(close[idx]);
        }
    }


    public void findNWeekData(int week, int entryDate, AtomicInteger dt, AtomicDouble price, AtomicInteger nDt, AtomicDouble nOpenPrice, AtomicDouble nClosePrice, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addWeeks(entryDate, week), 7, false);
        if (idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);

                price.set(close[idx]);

            int nextDay = DateUtil.addDays(date[idx], 1);
            idx = findVerifiedDateIndex(nextDay, 7, false);
            if (idx == -1) {
                nDt.set(0);
                nOpenPrice.set(0.0);
                nClosePrice.set(0.0);
            } else {
                nDt.set(date[idx]);
                nOpenPrice.set(open[idx]);

                    nClosePrice.set(close[idx]);
            }

        }
    }

    public void findOpenNDaysLater(int entryDate, int days, AtomicInteger dayDate, AtomicDouble dayPrice) {
        // not literal days, trading days! dummy!
        //int newDate = DateUtil.addDays(entryDate, days);
        //int idx = findVerifiedDateIndex(newDate, 2, false);\
        int idx = findDateIndex(entryDate);
        idx += days;
        if (idx == -1 || idx >= date.length) {
            dayDate.set(0);
            dayPrice.set(0.0);
        } else {
            dayDate.set(date[idx]);
            dayPrice.set(open[idx]);
        }
    }

    public void find2DaysLater(int entryDate, AtomicInteger day2Date, AtomicDouble day2Price, boolean useAdjustedClose) {
        int newDate = DateUtil.addDays(entryDate, 2);
        int idx = findVerifiedDateIndex(newDate);
        if (idx == -1) {
            day2Date.set(0);
            day2Price.set(0.0);
        } else {
            day2Date.set(date[idx]);

                day2Price.set(close[idx]);
        }
    }


    public void find1DayLater(int entryDate, AtomicInteger openDate, AtomicDouble openPrice, AtomicDouble closePrice, boolean useAdjustedClose) {
        int newDate = DateUtil.addDays(entryDate, 1);
        int idx = findVerifiedDateIndex(newDate);
        if (idx == -1) {
            openDate.set(0);
            openPrice.set(0.0);
            closePrice.set(0.0);
        } else {
            openDate.set(date[idx]);
            openPrice.set(open[idx]);

                closePrice.set(close[idx]);
        }
    }



    /**
     * Find min price in entryDate -> entryDate + months
     *
     * @param entryDate
     * @param months
     * @param dt
     * @param price
     * @param useAdjustedClose
     */
    public void findMinPriceFromEntry(int entryDate, int months, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        dt.set(0);
        price.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if (entryIdx == -1)
            return;
        int endIdx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, months));
        if (endIdx == -1)
            return;

        double[] arr = close;
        double tmpPrice = arr[entryIdx];
        int tmpDate = date[entryIdx];

        while (entryIdx <= endIdx) {
            if (arr[entryIdx] < tmpPrice) {
                tmpPrice = arr[entryIdx];
                tmpDate = date[entryIdx];
            }
            entryIdx++;
        }

        dt.set(tmpDate);
        price.set(tmpPrice);
    }

    public void findMaxPriceFromEntry(int entryDate, int months, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        dt.set(0);
        price.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if (entryIdx == -1)
            return;
        int endIdx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, months));
        if (endIdx == -1)
            return;

        double[] arr =  close;
        double tmpPrice = arr[entryIdx];
        int tmpDate = date[entryIdx];

        while (entryIdx <= endIdx) {
            if (arr[entryIdx] > tmpPrice) {
                tmpPrice = arr[entryIdx];
                tmpDate = date[entryIdx];
            }
            entryIdx++;
        }

        dt.set(tmpDate);
        price.set(tmpPrice);
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

    public Integer totalVolumePrev30Days(int entryIdx) {
        return rangeSum(entryIdx, -30, volume);
    }

    public Double totalPricePrev30Days(int entryIdx) {
        return rangeSum(entryIdx, -30, close);
    }

    public Integer totalVolumePost30Days(int entryIndex) {
        return rangeSum(entryIndex, 30, volume);
    }

    public Double totalPricePost30Days(int entryIdx) {
        return rangeSum(entryIdx, 30, close);
    }

    // UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY
    // I can't seem to get rangeSum()/rangeAvg() generic enough.
    private Integer rangeSum(int entryIdx, int days, int[] values) {
        OptionalInt rv;
        rv = range(entryIdx, days, values).reduce(Integer::sum);
        if(rv.isPresent())
            return rv.getAsInt();
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

    private Double rangeAvg(int entryIdx, int days, int[] values) {
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
    private IntStream range(int entryIdx, int days, int[] values) {
        if(days > 0) {
            int endIdx = findVerifiedDateIndex(DateUtil.addDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = entryIdx + 1; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findVerifiedDateIndex(DateUtil.minusDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = endIdx + 1;
            return Arrays.stream(values, endIdx, entryIdx);
        } else {
            return Arrays.stream(values, entryIdx, entryIdx);
        }
    }

    private DoubleStream range(int entryIdx, int days, double[] values) {
        if(days > 0) {
            int endIdx = findVerifiedDateIndex(DateUtil.addDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = entryIdx + 1; // endExclusive
            return Arrays.stream(values, entryIdx, endIdx);
        } else if(days < 0) {
            int endIdx = findVerifiedDateIndex(DateUtil.minusDays(date[entryIdx], days));
            if (endIdx == -1)
                endIdx = entryIdx;
            else
                endIdx = endIdx + 1;
            return Arrays.stream(values, endIdx, entryIdx);
        } else {
            return Arrays.stream(values, entryIdx, entryIdx);
        }
    }
    // END OF UGLYNESS
    // UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY UGLY


    public Point findEndOfYearPrice(int entryIdx) {
        int dt = DateUtil.findEndOfYearWeekDate(date[entryIdx]);
        int idx = findVerifiedDateIndex(dt, 7, false);
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

    public static Double slopeDaysPrev(int entryIdx, int days, int date[], double values[]) {

        // NB "days" meaning changed to "data points"
        //int startIdx = findVerifiedDateIndex(DateUtil.minusDays(entryDate, days));
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

    public Double zscore(int entryIdx, int days, int date[], double values[]) {
        int startIdx = findVerifiedDateIndex(DateUtil.minusDays(date[entryIdx], days));
        int endIdx = findVerifiedDateIndex(date[entryIdx]);

        if(startIdx < 0 || endIdx < 0)
            return null;

        SummaryStatistics stats = new SummaryStatistics();

        double zscore = 0;

        while(startIdx <= endIdx) {
            stats.addValue(values[startIdx]);

            double stdev = stats.getStandardDeviation();
            if(stdev == 0) {
                // either this is the first value or all initial values this far have had no variance (were the same)
                startIdx ++;
                continue;
            }
            double avg = stats.getMean();
            double closeValue;
            closeValue = values[startIdx];
            zscore = (closeValue - avg) / stdev;

            //logger.debug(String.format("z: %d, %f, ci=%d, cei=%d", data.date[calcIndex], zscore, calcIndex, calcEndIndex));

            startIdx++;
        }

        return zscore;
    }

}
