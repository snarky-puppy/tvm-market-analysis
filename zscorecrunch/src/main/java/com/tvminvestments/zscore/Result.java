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

    public Scenario getScenario() {
        return scenario;
    }

    public String toRestrictedString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,", exchange));
        builder.append(String.format("%s,%s,%d,", symbol, scenario.name, scenario.subScenario));

        /*
        bw.write(",Entry Date");
        bw.write(",Entry Price");
        bw.write(",Entry ZScore");
        bw.write(",Average Volume");
        bw.write(",Average Price");
         */
        builder.append(String.format("%d,%f,%f,%f,%f", pair.entryDate, pair.entryClosePrice, pair.entryZScore, pair.avgVolumePrev30.get(), pair.avgPricePrev30.get()));


        builder.append("\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,", exchange));
        builder.append(String.format("%s,%s,%d,%d,%d,%d,", symbol, scenario.name, scenario.subScenario, scenario.sampleStart, scenario.trackingStart, scenario.trackingEnd));
        builder.append(String.format("%s,%d,%f,%f,%f,%d,%f,%f,", pair.resultCode, pair.entryDate, pair.entryZScore, pair.entryClosePrice, pair.entryOpenPrice, pair.exitDate, pair.exitZScore, pair.exitPrice));
        builder.append(String.format("%f,%d,", pair.maxPriceAfterEntry, pair.maxPriceDate));
        if (pair.maxPriceZScore == -9999)
            builder.append(",");
        else
            builder.append(String.format("%f,", pair.maxPriceZScore));
        if (pair.ema50 == 0)
            builder.append(",");
        else
            builder.append(String.format("%f,", pair.ema50));
        if (pair.ema100 == 0)
            builder.append(",");
        else
            builder.append(String.format("%f,", pair.ema100));
        if (pair.ema200 == 0)
            builder.append(",");
        else
            builder.append(String.format("%f,", pair.ema200));
        builder.append(String.format("%d,%f,%d,%f,", pair.entryPrevDayDate, pair.entryPrevDayPrice, pair.maxPricePrevDate, pair.maxPricePrev));


        toString(builder, pair.pc100Date, pair.pc100Price);
        toString(builder, pair.pc90Date, pair.pc90Price);
        toString(builder, pair.pc80Date, pair.pc80Price);
        toString(builder, pair.pc70Date, pair.pc70Price);
        toString(builder, pair.pc60Date, pair.pc60Price);
        toString(builder, pair.pc50Date, pair.pc50Price);
        toString(builder, pair.pc40Date, pair.pc40Price);
        toString(builder, pair.pc30Date, pair.pc30Price);
        toString(builder, pair.pc20Date, pair.pc20Price);
        toString(builder, pair.pc10Date, pair.pc10Price);

        toString(builder, pair.ex1Date, pair.ex1Price);
        toString(builder, pair.ex2Date, pair.ex2Price);
        toString(builder, pair.ex3Date, pair.ex3Price);
        toString(builder, pair.ex4Date, pair.ex4Price);
        toString(builder, pair.ex5Date, pair.ex5Price);
        toString(builder, pair.ex6Date, pair.ex6Price);
        toString(builder, pair.ex7Date, pair.ex7Price);
        toString(builder, pair.ex8Date, pair.ex8Price);
        toString(builder, pair.ex9Date, pair.ex9Price);
        toString(builder, pair.ex10Date, pair.ex10Price);
        toString(builder, pair.ex11Date, pair.ex11Price);
        toString(builder, pair.ex12Date, pair.ex12Price);

        toString(builder, pair.day2Date, pair.day2Price);
        toString(builder, pair.day2PC10Date, pair.day2PC10Price);

        toString(builder, pair.entry1MonthMinDate, pair.entry1MonthMinPrice);
        toString(builder, pair.entry1MonthMaxDate, pair.entry1MonthMaxPrice);

        toString(builder, pair.pcDec5Date, pair.pcDec5Price);
        toString(builder, pair.pcDec10Date, pair.pcDec10Price);
        toString(builder, pair.pcDec15Date, pair.pcDec15Price);
        toString(builder, pair.pcDec20Date, pair.pcDec20Price);
        toString(builder, pair.pcDec25Date, pair.pcDec25Price);
        toString(builder, pair.pcDec30Date, pair.pcDec30Price);

        builder.append(String.format("%f,", pair.avgVolumePrev30.get()));
        builder.append(String.format("%f,", pair.avgPricePrev30.get()));
        builder.append(String.format("%f,", pair.avgVolumePost30.get()));
        builder.append(String.format("%f,", pair.avgPricePost30.get()));
        builder.append(String.format("%f,", pair.totalVolumePrev30.get()));
        builder.append(String.format("%f,", pair.totalPricePrev30.get()));
        builder.append(String.format("%f,", pair.totalVolumePost30.get()));
        builder.append(String.format("%f,", pair.totalPricePost30.get()));
        builder.append(String.format("%f,", pair.slope30.get()));
        builder.append(String.format("%f,", pair.slope3.get()));

        builder.append(String.format("%d,", pair.entryNextDayOpenDate.get()));
        builder.append(String.format("%f,", pair.entryNextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.entryNextDayClosePrice.get()));

        builder.append(String.format("%d,", pair.pco10Date.get()));
        builder.append(String.format("%f,", pair.pco10Price.get()));
        builder.append(String.format("%d,", pair.pco10NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco10NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco10NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco11Date.get()));
        builder.append(String.format("%f,", pair.pco11Price.get()));
        builder.append(String.format("%d,", pair.pco11NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco11NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco11NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco12Date.get()));
        builder.append(String.format("%f,", pair.pco12Price.get()));
        builder.append(String.format("%d,", pair.pco12NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco12NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco12NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco13Date.get()));
        builder.append(String.format("%f,", pair.pco13Price.get()));
        builder.append(String.format("%d,", pair.pco13NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco13NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco13NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco14Date.get()));
        builder.append(String.format("%f,", pair.pco14Price.get()));
        builder.append(String.format("%d,", pair.pco14NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco14NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco14NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco15Date.get()));
        builder.append(String.format("%f,", pair.pco15Price.get()));
        builder.append(String.format("%d,", pair.pco15NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco15NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco15NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco16Date.get()));
        builder.append(String.format("%f,", pair.pco16Price.get()));
        builder.append(String.format("%d,", pair.pco16NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco16NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco16NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco17Date.get()));
        builder.append(String.format("%f,", pair.pco17Price.get()));
        builder.append(String.format("%d,", pair.pco17NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco17NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco17NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco18Date.get()));
        builder.append(String.format("%f,", pair.pco18Price.get()));
        builder.append(String.format("%d,", pair.pco18NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco18NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco18NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.pco19Date.get()));
        builder.append(String.format("%f,", pair.pco19Price.get()));
        builder.append(String.format("%d,", pair.pco19NextDayDate.get()));
        builder.append(String.format("%f,", pair.pco19NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.pco19NextDayClosePrice.get()));

        builder.append(String.format("%d,", pair.week1Date.get()));
        builder.append(String.format("%f,", pair.week1Price.get()));
        builder.append(String.format("%d,", pair.week1NextDayDate.get()));
        builder.append(String.format("%f,", pair.week1NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.week1NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.week2Date.get()));
        builder.append(String.format("%f,", pair.week2Price.get()));
        builder.append(String.format("%d,", pair.week2NextDayDate.get()));
        builder.append(String.format("%f,", pair.week2NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.week2NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.week3Date.get()));
        builder.append(String.format("%f,", pair.week3Price.get()));
        builder.append(String.format("%d,", pair.week3NextDayDate.get()));
        builder.append(String.format("%f,", pair.week3NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.week3NextDayClosePrice.get()));
        builder.append(String.format("%d,", pair.week4Date.get()));
        builder.append(String.format("%f,", pair.week4Price.get()));
        builder.append(String.format("%d,", pair.week4NextDayDate.get()));
        builder.append(String.format("%f,", pair.week4NextDayOpenPrice.get()));
        builder.append(String.format("%f,", pair.week4NextDayClosePrice.get()));
        

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
