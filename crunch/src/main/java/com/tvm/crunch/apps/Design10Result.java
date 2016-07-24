package com.tvm.crunch.apps;

import com.tvm.crunch.Point;
import com.tvm.crunch.Result;

/**
 * Design 10 Result object
 *
 * Created by horse on 24/07/2016.
 */
class Design10Result extends Result {

    String symbol;
    String exchange;

    Double closePrev30DayAvg;
    Double volPrev30DayAvg;
    Integer date;
    Double close;
    Double zscore30Day;

    Point nextDayOpen;

    // 10, 20, 30, 40, 50
    static final int targetPercents = 5;
    Point[] targetPc = new Point[5];
    Double[] closePcPrev30DayAvg = new Double[5];
    Double[] volPcPrev30DayAvg = new Double[5];

    Point closeLastRecorded;
    Double closeLastRecorded30DayAvg;
    Double volLastRecorded30DayAvg;

    static final int eoyStart = 1995;
    static final int eoyEnd = 2015;
    static final int years = eoyEnd - eoyStart;
    Point[] eoy = new Point[years+1];


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("");
        sb.append(symbol);
        append(sb, exchange);
        append(sb, closePrev30DayAvg);
        append(sb, volPrev30DayAvg);
        append(sb, date);
        append(sb, close);
        append(sb, zscore30Day);

        appendPointOpen(sb, nextDayOpen);

        for(int i = 0; i < targetPercents; i++) {
            appendPointOpen(sb, targetPc[i]);
            append(sb, closePcPrev30DayAvg[i]);
            append(sb, volPcPrev30DayAvg[i]);
        }

        appendPointClose(sb, closeLastRecorded);
        append(sb, closeLastRecorded30DayAvg);
        append(sb, volLastRecorded30DayAvg);

        for(Point p : eoy)
            appendPointClose(sb, p);

        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String getHeader() {
        final StringBuilder sb = new StringBuilder("");
        sb.append("Symbol");
        sb.append(",Exchange");
        sb.append(",Close Prev 30 Day Avg");
        sb.append(",Volume Prev 30 Day Avg");
        sb.append(",Date");
        sb.append(",Close");
        sb.append(",ZScore 30 Day");
        sb.append(",Next Day Date");
        sb.append(",Next Day Open Price");

        for(int i = 0, p = 10; i < targetPercents; i++, p += 10) {
            sb.append(String.format(",%d%% Target Next Day Date", p));
            sb.append(String.format(",%d%% Target Next Day Open", p));
            sb.append(String.format(",%d%% Target Prev 30 Day Close Avg", p));
            sb.append(String.format(",%d%% Target Prev 30 Day Volume Avg", p));
        }

        sb.append(",Last Recorded Date");
        sb.append(",Last Recorded Close");
        sb.append(",Last Recorded Prev 30 Day Close Avg");
        sb.append(",Last Recorded Prev 30 Day Volume Avg");

        for(int y = eoyStart; y <= eoyEnd; y++) {
            sb.append(String.format(",%d End Of Year Date", y));
            sb.append(String.format(",%d End Of Year Close", y));
        }

        sb.append("\n");
        return sb.toString();
    }
}
