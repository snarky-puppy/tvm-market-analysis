package com.tvminvestments.zscore;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrap close data - date/close pairs
 *
 * Created by horse on 18/11/14.
 */
public class CloseData {
    private static final Logger logger = LogManager.getLogger(CloseData.class);
    public final String symbol;

    public int[] date;
    public double[] close;
    public double[] volume;
    public double[] adjustedClose;
    //public double[] ratio;
    public double[] open;

    public int index;


    public CloseData(String symbol, int size) {
        date = new int[size];
        close = new double[size];
        volume = new double[size];
        open = new double[size];
        adjustedClose = new double[size];
        //ratio = new double[size];
        this.symbol = symbol;

        index = 0;
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

    public double findAdjustedClosePriceAtDate(int date) {
        return adjustedClose[findDateIndex(date)];
    }


    public double findOpenPriceAtDate(int entryDate) {
        return open[findDateIndex(entryDate)];
    }

    public void sanity() {
        assert(date.length == close.length);
        assert(close.length == volume.length);
    }

    public int getDate() { return date[index]; }

    public double getClose() { return close[index]; }

    public double getVolume() { return volume[index]; }

    public void adjustClose(int startIdx, double factor) {

        //ratio[startIdx + 1] = factor;

        for(int i = startIdx; i >= 0; i--) {
            adjustedClose[i] = adjustedClose[i] * factor;
        }
    }

    public void findMaxAdjustedClosePriceAfterEntry(EntryExitPair pair) {
        findMaxClosePriceInternal(pair, adjustedClose);
    }

    public void findMaxClosePriceAfterEntry(EntryExitPair pair) {
        findMaxClosePriceInternal(pair, close);
    }

    private void findMaxClosePriceInternal(EntryExitPair pair, double[] array) {
        int idx = Arrays.binarySearch(date, pair.entryDate);
        if(idx < 0)
            throw new ArrayIndexOutOfBoundsException("Entry date not found");

        pair.maxPriceAfterEntry = 0;
        pair.maxPriceDate = -1;
        pair.maxPriceZScore = 0;

        while(idx < array.length) {
            if(array[idx] > pair.maxPriceAfterEntry) {
                pair.maxPriceAfterEntry = array[idx];
                pair.maxPriceDate = date[idx];
                if(idx > 0) {
                    pair.maxPricePrevDate = date[idx-1];
                    pair.maxPricePrev = array[idx-1];
                }
            }
            idx++;
        }
    }

    public void undoAdjustments() {
        System.arraycopy(close, 0, adjustedClose, 0, close.length);
        //Arrays.fill(ratio, 0.0);
    }

    public void findPCIncreaseFromEntry(int entryDate, int pc, AtomicInteger pcoDate, AtomicDouble pcoPrice, AtomicInteger pcoNextDayDate, AtomicDouble pcoNextDayOpenPrice, AtomicDouble pcoNextDayClosePrice, boolean useAdjustedClose) {
        int idx = findPCIncreaseFromEntryIndex(pc, entryDate, useAdjustedClose);
        if(idx == -1) {
            pcoDate.set(0);
            pcoPrice.set(0.0);
            pcoNextDayDate.set(0);
            pcoNextDayOpenPrice.set(0.0);
            pcoNextDayClosePrice.set(0.0);
        } else {
            pcoDate.set(date[idx]);
            if (useAdjustedClose)
                pcoPrice.set(adjustedClose[idx]);
            else
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
                if(useAdjustedClose)
                    pcoNextDayClosePrice.set(adjustedClose[idx]);
                else
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

    private int findPCIncreaseFromEntryIndex(int pc, int entryDate, boolean useAdjustedClose) {
        int idx = findDateIndex(entryDate);
        if(idx == -1)
            return -1;
        double[] arr = useAdjustedClose ? adjustedClose : close;
        double target = arr[idx] * (1.0 + ((double)pc / 100.0));
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

    public void totalPricePost30Days(int entryDate, AtomicDouble totalPricePost30, boolean useAdjustedClose) {
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

    public void slopeDaysPrev(int days, int entryDate, AtomicDouble slopeVal, boolean useAdjustedClose) {
        int entryIdx = findDateIndex(entryDate);
        if(entryIdx == -1)
            return;
        // NB changed to use data points as "days"
        //int startIdx = findVerifiedDateIndex(DateUtil.minusDays(entryDate, days));
        int startIdx = entryIdx - days;
        if(startIdx < 0)
            return;

        int cnt = entryIdx - startIdx + 1;

        if(cnt == 0)
            return;

        double xy[] = new double[cnt];
        double x2[] = new double[cnt];
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumClose = 0.0;
        double sumDate = 0.0;

        logger.debug("cnt="+cnt);

        for(int i = startIdx, j = 0; i <= entryIdx; i++, j++) {
            xy[j] = ((double)date[i]) * (useAdjustedClose ? adjustedClose[i] : close[i]);
            x2[j] = Math.pow(date[i], 2);

            sumXY += xy[j];
            sumX2 += x2[j];
            sumClose += (useAdjustedClose ? adjustedClose[i] : close[i]);
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
            slopeVal.set(slope);
    }


}
