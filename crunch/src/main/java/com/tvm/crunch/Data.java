package com.tvm.crunch;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

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
        if(foundDate > targetDate)
            distance = DateUtil.distance(targetDate, foundDate);
        else
            distance = DateUtil.distance(foundDate, targetDate);

        if(distance <= days)
            return idx;
        else
            return -1;
    }

    public int findDateIndex(int startDate) {
        return findDateIndex(startDate, true);
    }

    public int findDateIndex(int findDate, boolean softEnd) {
        if(date.length == 0) {
            logger.debug("findDate[" + findDate + "] no data found");
            return -1;
        }
        int idx = Arrays.binarySearch(date, findDate);
        if(idx >= 0) {
            return idx;
        } else {
            idx = (-idx) - 1;

            if (idx >= date.length) {
                if(softEnd) {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return date.length - 1;
                } else {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning -1.");
                    return -1;
                }
            } else {
                logger.debug("findDate["+findDate+"] not found, next data date: "+date[idx]+" (idx="+idx+")");
                return idx;
            }
        }
    }

    public int findVerifiedDateIndex(int findDate) {
        return findVerifiedDateIndex(findDate, 30, true);
    }

    public int findVerifiedDateIndex(int findDate, int maxDistanceDays, boolean softEnd) {
        if(date.length == 0) {
            logger.debug("findDate[" + findDate + "] no data found");
            return -1;
        }
        int idx = Arrays.binarySearch(date, findDate);
        if(idx >= 0) {
            return verifyDateDistance(idx, findDate, maxDistanceDays);
        } else {
            idx = (-idx) - 1;

            if (idx >= date.length) {
                if(softEnd) {
                    logger.debug("findDate[" + findDate + "] not found, past end of data. Returning end index (" + (date.length - 1) + ").");
                    return verifyDateDistance(date.length - 1, findDate, maxDistanceDays);
                } else {
                    return -1;
                }
            } else {
                logger.debug("findDate["+findDate+"] not found, next data date: "+date[idx]+" (idx="+idx+")");
                return verifyDateDistance(idx, findDate, maxDistanceDays);
            }
        }
    }


    public double findClosePriceAtDate(int date) {
        return close[findDateIndex(date)];
    }

    public double findOpenPriceAtDate(int entryDate) {
        return open[findDateIndex(entryDate)];
    }

    public void sanity() {
        assert(date.length == close.length);
        assert(close.length == volume.length);
    }

    public Point findPCIncreaseFromEntry(int entryDate, int pc, AtomicInteger pcoDate, AtomicDouble pcoPrice, AtomicInteger pcoNextDayDate, AtomicDouble pcoNextDayOpenPrice, AtomicDouble pcoNextDayClosePrice) {
        int idx = findPCIncreaseFromEntryIndex(pc, entryDate);
        if(idx == -1) {
            return null;
            pcoDate.set(0);
            pcoPrice.set(0.0);
            pcoNextDayDate.set(0);
            pcoNextDayOpenPrice.set(0.0);
            pcoNextDayClosePrice.set(0.0);
        } else {
            pcoDate.set(date[idx]);
            pcoPrice.set(close[idx]);

            int nextDay = DateUtil.addDays(date[idx], 1);
            idx = findDateIndex(nextDay, false);
            if(idx == -1) {
                pcoNextDayDate.set(0);
                pcoNextDayOpenPrice.set(0.0);
                pcoNextDayClosePrice.set(0.0);
            } else {
                pcoNextDayDate.set(date[idx]);
                pcoNextDayOpenPrice.set(open[idx]);
                pcoNextDayClosePrice.set(close[idx]);
            }
        }
    }

    public void findPCIncreaseFromEntry(int entryDate, int pc, AtomicInteger pcDate, AtomicDouble pcPrice, boolean useAdjustedClose) {
        int idx = findPCIncreaseFromEntryIndex(pc, entryDate, useAdjustedClose);
        if(idx == -1) {
            pcPrice.set(0.0);
            pcDate.set(0);
        } else {
            pcDate.set(date[idx]);
            if (useAdjustedClose)
                pcPrice.set(adjustedClose[idx]);
            else
                pcPrice.set(close[idx]);
        }
    }

    public void findPCIncreaseOpen(int entryDate, int pc, AtomicInteger pcDate, AtomicDouble pcPrice) {
        int idx = findPCIncreaseFromEntryOpenIndex(pc, entryDate);
        if(idx == -1) {
            pcPrice.set(0.0);
            pcDate.set(0);
        } else {
            pcDate.set(date[idx]);
            pcPrice.set(open[idx]);
        }
    }

    private int findPCIncreaseFromEntryOpenIndex(int pc, int entryDate) {
        int idx = findDateIndex(entryDate);
        if(idx == -1)
            return -1;
        double target = open[idx] * (1.0 + ((double)pc / 100.0));
        while(++idx < open.length) {
            if(open[idx] >= target)
                return idx;
        }
        return -1;
    }

    private int findPCIncreaseFromEntryIndex(int pc, int entryDate) {
        int idx = findDateIndex(entryDate);
        if(idx == -1)
            return -1;
        double target = close[idx] * (1.0 + ((double)pc / 100.0));
        logger.debug(String.format("findPCIncrease [%d]: entry price=%f, target=%f", pc, arr[idx], target));
        while(++idx < arr.length) {
            if(arr[idx] >= target)
                return idx;
        }
        return -1;
    }

    public void findNMonthData(int month, int entryDate, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, month), 14, false);
        if(idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);
            if(useAdjustedClose)
                price.set(adjustedClose[idx]);
            else
                price.set(close[idx]);
        }
    }

    public void findNWeekData(int week, int entryDate, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addWeeks(entryDate, week), 7, false);
        if(idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);
            if(useAdjustedClose)
                price.set(adjustedClose[idx]);
            else
                price.set(close[idx]);
        }
    }


    public void findNWeekData(int week, int entryDate, AtomicInteger dt, AtomicDouble price, AtomicInteger nDt, AtomicDouble nOpenPrice, AtomicDouble nClosePrice, boolean useAdjustedClose) {
        int idx = findVerifiedDateIndex(DateUtil.addWeeks(entryDate, week), 7, false);
        if(idx == -1) {
            dt.set(0);
            price.set(0.0);
        } else {
            dt.set(date[idx]);
            if(useAdjustedClose)
                price.set(adjustedClose[idx]);
            else
                price.set(close[idx]);

            int nextDay = DateUtil.addDays(date[idx], 1);
            idx = findVerifiedDateIndex(nextDay, 7, false);
            if(idx == -1) {
                nDt.set(0);
                nOpenPrice.set(0.0);
                nClosePrice.set(0.0);
            } else {
                nDt.set(date[idx]);
                nOpenPrice.set(open[idx]);
                if(useAdjustedClose)
                    nClosePrice.set(adjustedClose[idx]);
                else
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
        if(idx == -1 || idx >= date.length) {
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
        if(idx == -1) {
            day2Date.set(0);
            day2Price.set(0.0);
        } else {
            day2Date.set(date[idx]);
            if (useAdjustedClose)
                day2Price.set(adjustedClose[idx]);
            else
                day2Price.set(close[idx]);
        }
    }


    public void find1DayLater(int entryDate, AtomicInteger openDate, AtomicDouble openPrice, AtomicDouble closePrice, boolean useAdjustedClose) {
        int newDate = DateUtil.addDays(entryDate, 1);
        int idx = findVerifiedDateIndex(newDate);
        if(idx == -1) {
            openDate.set(0);
            openPrice.set(0.0);
            closePrice.set(0.0);
        } else {
            openDate.set(date[idx]);
            openPrice.set(open[idx]);
            if (useAdjustedClose)
                closePrice.set(adjustedClose[idx]);
            else
                closePrice.set(close[idx]);
        }
    }

    public void findPCDecreaseFromEntry(int entryDate, int pc, int months, AtomicInteger pcDate, AtomicDouble pcPrice, boolean useAdjustedClose) {
        int idx = findPCDecreaseFromEntryIndex(pc, entryDate, months, useAdjustedClose);
        if(idx == -1) {
            pcPrice.set(0.0);
            pcDate.set(0);
        } else {
            pcDate.set(date[idx]);
            if (useAdjustedClose)
                pcPrice.set(adjustedClose[idx]);
            else
                pcPrice.set(close[idx]);
        }
    }

    private int findPCDecreaseFromEntryIndex(int pc, int entryDate, int months, boolean useAdjustedClose) {
        int plusOneMonth = DateUtil.addMonths(entryDate, months);
        int idx = findVerifiedDateIndex(entryDate);
        if(idx == -1)
            return -1;
        if(date[idx] > plusOneMonth)
            return -1;
        double[] arr = useAdjustedClose ? adjustedClose : close;
        double target = (arr[idx] * (1.0 - ((double)pc / 100.0)));
        logger.debug(String.format("findPCDecrease [%d]: entry price=%f, target=%f", pc, arr[idx], target));
        while(++idx < arr.length) {
            //logger.info(String.format("%f <= target", arr[idx]));
            if(arr[idx] <= target)
                return idx;
        }
        return -1;
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
        if(entryIdx == -1)
            return;
        int endIdx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, months));
        if(endIdx == -1)
            return;

        double[] arr = useAdjustedClose ? adjustedClose : close;
        double tmpPrice = arr[entryIdx];
        int tmpDate = date[entryIdx];

        while(entryIdx <= endIdx) {
            if(arr[entryIdx] < tmpPrice) {
                tmpPrice = arr[entryIdx];
                tmpDate = date[entryIdx];
            }
            entryIdx ++;
        }

        dt.set(tmpDate);
        price.set(tmpPrice);
    }

    public void findMaxPriceFromEntry(int entryDate, int months, AtomicInteger dt, AtomicDouble price, boolean useAdjustedClose) {
        dt.set(0);
        price.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int endIdx = findVerifiedDateIndex(DateUtil.addMonths(entryDate, months));
        if(endIdx == -1)
            return;

        double[] arr = useAdjustedClose ? adjustedClose : close;
        double tmpPrice = arr[entryIdx];
        int tmpDate = date[entryIdx];

        while(entryIdx <= endIdx) {
            if(arr[entryIdx] > tmpPrice) {
                tmpPrice = arr[entryIdx];
                tmpDate = date[entryIdx];
            }
            entryIdx ++;
        }

        dt.set(tmpDate);
        price.set(tmpPrice);
    }

    public void avgVolumePrev30Days(int entryDate, AtomicDouble avgVolumePrev30) {
        avgVolumePrev30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int startIdx = findDateIndex(DateUtil.minusDays(entryDate, 30));
        if(startIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; startIdx <= entryIdx; startIdx++) {
            cnt++;
            sum += volume[startIdx];
        }
        avgVolumePrev30.set(sum/cnt);
    }

    public void avgPricePrev30Days(int entryDate, AtomicDouble avgPricePrev30, boolean useAdjustedClose) {
        avgPricePrev30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int startIdx = findDateIndex(DateUtil.minusDays(entryDate, 30));
        if(startIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; startIdx <= entryIdx; startIdx++) {
            cnt++;
            if(useAdjustedClose)
                sum += adjustedClose[startIdx];
            else
                sum += close[startIdx];
        }
        avgPricePrev30.set(sum/cnt);
    }

    public void avgVolumePost30Days(int entryDate, AtomicDouble avgVolumePost30) {
        avgVolumePost30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int endIdx = findDateIndex(DateUtil.addDays(entryDate, 30));
        if(endIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; entryIdx <= endIdx; entryIdx++) {
            cnt++;
            sum += volume[entryIdx];
        }
        avgVolumePost30.set(sum/cnt);
    }

    public void avgPricePost30Days(int entryDate, AtomicDouble avgPricePost30, boolean useAdjustedClose) {
        avgPricePost30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int endIdx = findDateIndex(DateUtil.addDays(entryDate, 30));
        if(endIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; entryIdx <= endIdx; entryIdx++) {
            cnt++;
            if(useAdjustedClose)
                sum += adjustedClose[entryIdx];
            else
                sum += close[entryIdx];
        }
        avgPricePost30.set(sum/cnt);
    }

    public void totalVolumePrev30Days(int entryDate, AtomicDouble totalVolumePrev30) {
        totalVolumePrev30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int startIdx = findDateIndex(DateUtil.minusDays(entryDate, 30));
        if(startIdx == -1)
            return;

        logger.debug(String.format("totalVolumePrev30: start=%d end=%d", date[startIdx], date[entryIdx]));

        int cnt = 0;
        double sum = 0.0;
        for(; startIdx <= entryIdx; startIdx++) {
            cnt++;
            sum += volume[startIdx];
        }
        totalVolumePrev30.set(sum);
    }

    public void totalPricePrev30Days(int entryDate, AtomicDouble totalPricePrev30, boolean useAdjustedClose) {
        totalPricePrev30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int startIdx = findDateIndex(DateUtil.minusDays(entryDate, 30));
        if(startIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; startIdx <= entryIdx; startIdx++) {
            cnt++;
            if(useAdjustedClose)
                sum += adjustedClose[startIdx];
            else
                sum += close[startIdx];
        }
        totalPricePrev30.set(sum);
    }

    public void totalVolumePost30Days(int entryDate, AtomicDouble totalVolumePost30) {
        totalVolumePost30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int endIdx = findDateIndex(DateUtil.addDays(entryDate, 30));
        if(endIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; entryIdx <= endIdx; entryIdx++) {
            cnt++;
            sum += volume[entryIdx];
        }
        totalVolumePost30.set(sum);
    }

    public void totalPricePost30Days(int entryDate, AtomicDouble totalPricePost30) {
        totalPricePost30.set(0.0);
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        int endIdx = findDateIndex(DateUtil.addDays(entryDate, 30));
        if(endIdx == -1)
            return;

        int cnt = 0;
        double sum = 0.0;
        for(; entryIdx <= endIdx; entryIdx++) {
            cnt++;
            if(useAdjustedClose)
                sum += adjustedClose[entryIdx];
            else
                sum += close[entryIdx];
        }
        totalPricePost30.set(sum);
    }

    public Point findEndOfYearPrice(int entryIdx) {
        int dt = DateUtil.findEndOfYearWeekDate(date[entryIdx]);
        int idx = findVerifiedDateIndex(dt, 7, false);
        if(idx != -1) {
            return new Point(this, idx);
        } else
            return null;
    }


}
