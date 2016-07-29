package com.tvm.crunch.apps;

import com.tvm.crunch.Data;
import com.tvm.crunch.DataException;
import com.tvm.crunch.Point;
import com.tvm.crunch.Result;
import com.tvm.crunch.scenario.Scenario;

/**
 * Design 10 Result object
 *
 * Created by horse on 24/07/2016.
 */
class Design10Result extends Result {

    private String symbol;
    private String exchange;

    private Scenario scenario;

    private Double closePrev30DayAvg;
    private Double volPrev30DayAvg;
    private Integer date;
    private Double close;
    private Double zscore30Day;
    private Double triggerZScore;

    private Point nextDayOpen;

    // 10, 20, 30, 40, 50
    private static final int targetPercents = 5;
    private Point[] targetPc = new Point[5];
    private Double[] closePcPrev30DayAvg = new Double[5];
    private Double[] volPcPrev30DayAvg = new Double[5];

    private Point closeLastRecorded;
    private Double closeLastRecorded30DayAvg;
    private Double volLastRecorded30DayAvg;

    private static final int eoyStart = 1995;
    private static final int eoyEnd = 2015;
    private static final int years = eoyEnd - eoyStart;
    private Point[] eoy = new Point[years+1];


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("");
        sb.append(symbol);
        append(sb, exchange);

        if(scenario != null) {
            append(sb, scenario.name);
            append(sb, scenario.subScenario);
            append(sb, scenario.sampleStart);
            append(sb, scenario.trackingStart);
            append(sb, scenario.trackingEnd);
        } else
            sb.append(",,,,,");

        append(sb, closePrev30DayAvg);
        append(sb, volPrev30DayAvg);
        append(sb, date);
        append(sb, close);
        append(sb, zscore30Day);
        append(sb, triggerZScore);

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

        sb.append(",Scenario");
        sb.append(",Sub Scenario");
        sb.append(",Sample Start");
        sb.append(",Tracking Start");
        sb.append(",Tracking End");

        sb.append(",Close Prev 30 Day Avg");
        sb.append(",Volume Prev 30 Day Avg");
        sb.append(",Date");
        sb.append(",Close");
        sb.append(",ZScore 30 Day");
        sb.append(",ZScore Trigger");
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

    public static Design10Result create(Data data, int idx) {
        return create(data, idx, 0, null);
    }

    public static Design10Result create(Data data, int idx, double zscore, Scenario scenario) {

        Design10Result r = new Design10Result();
        r.symbol = data.symbol;
        r.exchange = data.market;

        if(scenario != null) {
            r.scenario = scenario;
        }

        r.closePrev30DayAvg = data.avgPricePrev30Days(idx);
        r.volPrev30DayAvg = data.avgVolumePrev30Days(idx);

        r.date = data.date[idx];
        r.close = data.close[idx];

        DataException.ignore(() -> r.zscore30Day = data.zscore(idx, 30));

        // scenario null when called from TRendCont context
        if(scenario != null)
            r.triggerZScore = zscore;

        r.nextDayOpen = data.findNDayPoint(idx, 1);

        for(int i = 0, p = 10; i < Design10Result.targetPercents; i++, p += 10) {
            Point closePt = data.findPCIncrease(idx, p, data.close);
            if(closePt != null) {
                Point nextPoint = data.findNDayPoint(closePt.index, 1);
                if(nextPoint != null) {
                    r.targetPc[i] = nextPoint;
                    r.closePcPrev30DayAvg[i] = data.avgPricePrev30Days(nextPoint.index);
                    r.volPcPrev30DayAvg[i] = data.avgVolumePrev30Days(nextPoint.index);
                }
            }
        }

        Point lastRecorded = new Point(data, data.date.length - 1);
        r.closeLastRecorded = lastRecorded;
        r.closeLastRecorded30DayAvg = data.avgPricePrev30Days(lastRecorded.index);
        r.volLastRecorded30DayAvg = data.avgVolumePrev30Days(lastRecorded.index);

        for(int i = 0, y = Design10Result.eoyStart; y <= Design10Result.eoyEnd; y++, i++) {
            r.eoy[i] = data.findEndOfYearPrice(y);
        }

        return r;
    }
}
