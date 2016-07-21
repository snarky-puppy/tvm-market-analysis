package com.tvm.crunch;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Created by horse on 21/07/2016.
 */
public class Calculations {

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

    public Double slopeDaysPrev(int entryIdx, int days, int date[], double values[]) {

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

    public double zscore(int entryIdx, int days, int date[], double values[]) {
        int startIdx = entryIdx - days;
        int endIdx = entryIdx;

        if(startIdx < 0 || endIdx < 0)
            return 0;

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
