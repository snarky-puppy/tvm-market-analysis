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

    public String news;

    public static int holdMin = 22;
    public static int holdMax = 49;
    public static int numHolds = holdMax - holdMin + 1;
    Integer holdDate[] = new Integer[numHolds];
    Double holdPrice[] = new Double[numHolds];
    Double holdPc[] = new Double[numHolds];

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
        sb.append(",\"").append(news).append("\"");

        for(int i = 0, h = holdMin; h <= holdMax; h++, i++) {
            if(holdDate[i] != null) {
                sb.append(",").append(holdDate[i]);
                sb.append(",").append(holdPrice[i]);
                sb.append(",").append(holdPc[i]);
            } else {
                sb.append(",,,");
            }
        }

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
        bw.append("News,");

        for(int i = 0, h = holdMin; h <= holdMax; h++, i++) {
            bw.append(String.format("Hold %d Date,", h));
            bw.append(String.format("Hold %d Price,", h));
            bw.append(String.format("Hold %d Pc,", h));
        }

        bw.append('\n');

        return bw.toString();

    }
}
