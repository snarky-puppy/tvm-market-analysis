package com.tvminvestments.zscore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.stream.DoubleStream;

/**
 * Calculate exponential moving average
 *
 * Created by horse on 25/07/15.
 */
public class EMA {

    private static final Logger logger = LogManager.getLogger(EMA.class);

    private final double[] data;

    public EMA(double[] data) {
        this.data = data;
    }

    public double calculate(int days, int startIdx) throws EMAException {
        if(days <= 1) {
            throw new EMAException("not enough days for meaningful EMA: "+days);
        }
        if(days > startIdx + 1) {
            throw new EMAException("not enough data to calculate "+days+"day EMA");
        }

        double ema = 0.0;

        // start with SMA
        double prevDaysEMA = simpleMovingAverage(days, startIdx);

        // multiplier
        double k = (2 / (days + 1));

        for(int idx = startIdx - days + 1; idx <= startIdx; idx++) {
            ema = data[idx] * k + prevDaysEMA * (1 - k);
            prevDaysEMA = ema;
        }

        return ema;
    }

    private double simpleMovingAverage(int days, int startIdx) {
        double sum = 0.0;
        for (int i = startIdx - days + 1; i <= startIdx ; i++) {
            sum += data[i];
        }
        return sum / days;
    }
}
