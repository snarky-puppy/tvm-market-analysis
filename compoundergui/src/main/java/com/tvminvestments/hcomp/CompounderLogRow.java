package com.tvminvestments.hcomp;

/**
 * Created by horse on 28/02/2016.
 */
public class CompounderLogRow {

    // starting factors
    public double spread;
    public int percent;
    public double balanceCash;

    // working factors
    public int iteration;
    public double profit;

    public double minInvest;

    // finishing factors
    public double cash;
    public double trades;
    public double total;

    public static String header() {
        return "Percent ,Spread ,Iteration ,Starting Cash Balance ,True Profit ,Min Invest ,Cash ,Trades ,Total\n";
    }

    public String toString() {
        return String.format("%d,%.2f,%d, %.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n", percent, spread, iteration, balanceCash, profit, minInvest, cash, trades, total);
    }

}
