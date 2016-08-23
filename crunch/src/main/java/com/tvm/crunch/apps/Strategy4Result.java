package com.tvm.crunch.apps;

import com.tvm.crunch.Result;

/**
 * Strategy 4 Result object
 *
 * Created by horse on 23/08/2016.
 */
public class Strategy4Result extends Result {

    public String symbol;
    public String exchange;
    public String category;

    // dates
    public int d0;
    public int d7;
    public int d14;
    public int d21;
    // profit/loss
    public double pl7;
    public double pl14;
    public double pl21;
    public double slope;
    public double dollarVolume;

    public int holdPl28Date;
    public double holdPl28Price;
    public double holdPl28Pc;

    public int holdPl35Date;
    public double holdPl35Price;
    public double holdPl35Pc;
    public String news;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(symbol);
        sb.append(",").append(exchange);
        sb.append(",").append(category);
        sb.append(",").append(d0);
        sb.append(",").append(d7);
        sb.append(",").append(d14);
        sb.append(",").append(d21);
        sb.append(",").append(pl7);
        sb.append(",").append(pl14);
        sb.append(",").append(pl21);
        sb.append(",").append(slope);
        sb.append(",").append(String.format("%.8f", dollarVolume));
        sb.append(",").append(holdPl28Date);
        sb.append(",").append(holdPl28Price);
        sb.append(",").append(holdPl28Pc);
        sb.append(",").append(holdPl35Date);
        sb.append(",").append(holdPl35Price);
        sb.append(",").append(holdPl35Pc);
        sb.append(",\"").append(news).append("\"");
        sb.append('\n');
        return sb.toString();
    }

    @Override
    public String getHeader() {
        StringBuilder bw = new StringBuilder();
        bw.append("Symbol,");
        bw.append("Exchange,");
        bw.append("Category,");
        bw.append("Date0,");
        bw.append("Date7,");
        bw.append("Date14,");
        bw.append("Date21,");

        bw.append("P1,");
        bw.append("P2,");
        bw.append("P3,");
        bw.append("Slope,");
        bw.append("Dollar Volume,");


        bw.append("Hold 28 Date,");
        bw.append("Hold 28 Price,");
        bw.append("Hold 28 Pc,");

        bw.append("Hold 35 Date,");
        bw.append("Hold 35 Price,");
        bw.append("Hold 35 Pc,");

        bw.append("News,");


        bw.append('\n');

        return bw.toString();

    }
}
