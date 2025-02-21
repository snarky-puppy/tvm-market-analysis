package com.tvminvestments.zscore;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.scenario.Scenario;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by horse on 18/09/15.
 */
public class Result {

    private final Scenario scenario;
    private final String symbol;
    private final EntryExitPair pair;
    private final String exchange;

    public Result(String exchange, String symbol, Scenario scenario, EntryExitPair entryExitPair) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.scenario = scenario;
        this.pair = entryExitPair;
    }

    public boolean isEntry() {
        return pair.resultCode == ResultCode.ENTRY || pair.resultCode == ResultCode.ENTRY_EXIT;
    }


    public Scenario getScenario() {
        return scenario;
    }

    public String toRestrictedString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,", exchange));
        builder.append(String.format("%s,%s,%d,", symbol, scenario.name, scenario.subScenario));
        builder.append(String.format("%d,%f,%f,%f,%f", pair.entryDate, pair.entryClosePrice, pair.entryZScore, pair.avgVolumePrev30.get(), pair.avgPricePrev30.get()));
        builder.append("\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,", exchange));
        builder.append(String.format("%s,%s,%d,%d,%d,%d,", symbol, scenario.name, scenario.subScenario, scenario.sampleStart, scenario.trackingStart, scenario.trackingEnd));
        builder.append(String.format("%f,%f,%f,", pair.entryZScore, pair.avgVolumePrev30.get(), pair.avgPricePrev30.get()));

        builder.append(String.format("%d,", pair.pc1OpenDate.get()));
        builder.append(String.format("%f,", pair.pc1OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc2OpenDate.get()));
        builder.append(String.format("%f,", pair.pc2OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc3OpenDate.get()));
        builder.append(String.format("%f,", pair.pc3OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc4OpenDate.get()));
        builder.append(String.format("%f,", pair.pc4OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc5OpenDate.get()));
        builder.append(String.format("%f,", pair.pc5OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc6OpenDate.get()));
        builder.append(String.format("%f,", pair.pc6OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc7OpenDate.get()));
        builder.append(String.format("%f,", pair.pc7OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc8OpenDate.get()));
        builder.append(String.format("%f,", pair.pc8OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc9OpenDate.get()));
        builder.append(String.format("%f,", pair.pc9OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc10OpenDate.get()));
        builder.append(String.format("%f,", pair.pc10OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc15OpenDate.get()));
        builder.append(String.format("%f,", pair.pc15OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc20OpenDate.get()));
        builder.append(String.format("%f,", pair.pc20OpenPrice.get()));
        builder.append(String.format("%d,", pair.pc25OpenDate.get()));
        builder.append(String.format("%f,", pair.pc25OpenPrice.get()));

        builder.append(String.format("%f,", pair.eoy1994ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy1995ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy1996ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy1997ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy1998ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy1999ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2000ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2001ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2002ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2003ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2004ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2005ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2006ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2007ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2008ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2009ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2010ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2011ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2012ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2013ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2014ClosePrice.get()));
        builder.append(String.format("%f,", pair.eoy2015ClosePrice.get()));

        builder.append(String.format("%f,", pair.rsi7_0Days.get()));
        builder.append(String.format("%f,", pair.rsi7_1Days.get()));
        builder.append(String.format("%f,", pair.rsi7_2Days.get()));
        builder.append(String.format("%f,", pair.rsi7_3Days.get()));
        builder.append(String.format("%f,", pair.rsi7_4Days.get()));
        builder.append(String.format("%f,", pair.rsi7_5Days.get()));
        builder.append(String.format("%f,", pair.rsi7_6Days.get()));
        builder.append(String.format("%f,", pair.rsi7_7Days.get()));
        builder.append(String.format("%f,", pair.rsi7_8Days.get()));
        builder.append(String.format("%f,", pair.rsi7_9Days.get()));
        builder.append(String.format("%f,", pair.rsi7_10Days.get()));
        builder.append(String.format("%f,", pair.rsi7_11Days.get()));
        builder.append(String.format("%f,", pair.rsi7_12Days.get()));
        builder.append(String.format("%f,", pair.rsi7_13Days.get()));
        builder.append(String.format("%f,", pair.rsi7_14Days.get()));

        builder.append(String.format("%f,", pair.rsi14_0Days.get()));
        builder.append(String.format("%f,", pair.rsi14_1Days.get()));
        builder.append(String.format("%f,", pair.rsi14_2Days.get()));
        builder.append(String.format("%f,", pair.rsi14_3Days.get()));
        builder.append(String.format("%f,", pair.rsi14_4Days.get()));
        builder.append(String.format("%f,", pair.rsi14_5Days.get()));
        builder.append(String.format("%f,", pair.rsi14_6Days.get()));
        builder.append(String.format("%f,", pair.rsi14_7Days.get()));
        builder.append(String.format("%f,", pair.rsi14_8Days.get()));
        builder.append(String.format("%f,", pair.rsi14_9Days.get()));
        builder.append(String.format("%f,", pair.rsi14_10Days.get()));
        builder.append(String.format("%f,", pair.rsi14_11Days.get()));
        builder.append(String.format("%f,", pair.rsi14_12Days.get()));
        builder.append(String.format("%f,", pair.rsi14_13Days.get()));
        builder.append(String.format("%f,", pair.rsi14_14Days.get()));

        builder.append(String.format("%d,", pair.next0DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next0DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next1DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next1DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next2DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next2DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next3DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next3DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next4DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next4DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next5DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next5DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next6DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next6DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next7DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next7DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next8DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next8DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next9DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next9DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next10DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next10DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next11DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next11DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next12DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next12DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next13DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next13DayOpenPrice.get()));
        builder.append(String.format("%d,", pair.next14DayOpenDate.get()));
        builder.append(String.format("%f,", pair.next14DayOpenPrice.get()));

        builder.append("\n");
        return builder.toString();
    }

    private void toString(StringBuilder builder, AtomicInteger date, AtomicDouble price) {
        if(date.get() != 0) {
            builder.append(String.format("%d,%f,", date.get(), price.get()));
        } else
            builder.append(",,");
    }
}
