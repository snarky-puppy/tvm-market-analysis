package com.tvminvestments.hcomp;

/**
 * Created by horse on 28/02/2016.
 */
public class CompounderLogRow {

    // starting factors
    public double spread;
    public int percent;
    public double startBank;

    // working factors
    public int iteration;
    public int period;

    public double minInvest;

    // finishing factors
    public double cash;
    public double trades;
    public double total;

    public static String header() {
        return "Percent ,Spread ,Start Bank ,Iteration ,Period ,Min Invest ,Cash ,Trades ,Total\n";
    }

    public String toString() {
        return String.format("%d,%.2f,%.2f,%d,%d,%.2f,%.2f,%.2f,%.2f\n", percent, spread, startBank, iteration, period, minInvest, cash, trades, total);
    }

}
