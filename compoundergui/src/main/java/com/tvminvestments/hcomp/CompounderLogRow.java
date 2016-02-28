package com.tvminvestments.hcomp;

/**
 * Created by horse on 28/02/2016.
 */
public class CompounderLogRow {

    // starting factors
    public double startSpread;
    public int startPercent;
    public double startBank;

    // working factors
    public int iteration;
    public int period;

    public double spread;
    public int percent;

    public double minInvest;
    public double iterationBank;

    // finishing factors
    public double cash;
    public double trades;
    public double total;

    public static String header() {
        return "Start Spread ,Start Percent ,Start Bank ,Iteration ,Period ,Spread ,Percent ,Min Invest ,Iteration Bank ,Final Cash ,Final Trades ,Final Total";
    }

}
