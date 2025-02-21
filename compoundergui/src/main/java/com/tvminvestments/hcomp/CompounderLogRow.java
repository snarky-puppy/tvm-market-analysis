package com.tvminvestments.hcomp;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Show Working Out log row
 *
 * Created by horse on 28/02/2016.
 */
public class CompounderLogRow {

    static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LocalDate date;

    // starting factors
    public double spread;
    public double percent;
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
        return "Date ,Percent ,Spread ,Iteration ,Starting Cash Balance ,True Profit ,Min Invest ,Cash ,Trades ,Total\n";
    }

    public String toString() {
        return String.format("%s,%.2f,%.2f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                date.format(dtf), percent, spread, iteration, balanceCash, profit, minInvest, cash, trades, total);
    }

}
