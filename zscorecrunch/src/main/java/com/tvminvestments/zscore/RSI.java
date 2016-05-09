package com.tvminvestments.zscore;

import com.google.common.util.concurrent.AtomicDouble;


/**
 * Created by horse on 7/05/2016.
 */
public class RSI {
    private int periodLength;
    private final double[] prices;

    public RSI(int periodLength, double[] prices) {
        this.periodLength = periodLength;
        this.prices = prices;
    }

    public void calculate(int startIdx, AtomicDouble price) {
        try {
            double p = _calculate(startIdx);
            if(p != -1)
                price.set(p);
        } catch(ArrayIndexOutOfBoundsException e) {
        }
    }


    private double _calculate(int startIdx) {
        double value = 0;
        int lastPrice = startIdx;
        int firstPrice = lastPrice - periodLength + 1;

        double gains = 0;
        double losses = 0;
        double avgUp = 0;
        double avgDown = 0;


        //double delta = prices[lastPrice] - prices[lastPrice - 1];
        //gains = Math.max(0, delta);
        //losses = Math.max(0, -delta);

        for (int bar = firstPrice; bar <= lastPrice; bar++) {
            double change = prices[bar] - prices[bar - 1];
            gains += Math.max(0, change);
            losses += Math.max(0, -change);
        }
        avgUp = gains / periodLength;
        avgDown = losses / periodLength;

        value = 100 - (100 / (1 + (avgUp / avgDown)));

        return value; //Math.round(value);
    }

}
