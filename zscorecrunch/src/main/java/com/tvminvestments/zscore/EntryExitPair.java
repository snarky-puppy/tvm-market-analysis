package com.tvminvestments.zscore;


import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The data contained in this structure will eventually become a row in a .csv file
 *
 * Created by horse on 26/11/14.
 */
public class EntryExitPair {

    public ResultCode resultCode;

    public double entryZScore;
    public int entryDate;
    public double entryClosePrice;
    public double entryOpenPrice;

    public double exitZScore;
    public int exitDate;
    public double exitPrice;

    public double maxPriceAfterEntry;
    public int maxPriceDate;
    public double maxPriceZScore;

    public double ema200;
    public double ema100;
    public double ema50;

    public int entryPrevDayDate;
    public double entryPrevDayPrice;

    public int maxPricePrevDate; // previous days date, from maxPriceDate
    public double maxPricePrev; // previous days trading price, from maxPriceDate


    public AtomicInteger pc100Date = new AtomicInteger(0);
    public AtomicDouble pc100Price = new AtomicDouble(0.0);

    public AtomicInteger pc90Date = new AtomicInteger(0);
    public AtomicDouble pc90Price = new AtomicDouble(0.0);

    public AtomicInteger pc80Date = new AtomicInteger(0);
    public AtomicDouble pc80Price = new AtomicDouble(0.0);

    public AtomicInteger pc70Date = new AtomicInteger(0);
    public AtomicDouble pc70Price = new AtomicDouble(0.0);

    public AtomicInteger pc60Date = new AtomicInteger(0);
    public AtomicDouble pc60Price = new AtomicDouble(0.0);

    public AtomicInteger pc50Date = new AtomicInteger(0);
    public AtomicDouble pc50Price = new AtomicDouble(0.0);

    public AtomicInteger pc40Date = new AtomicInteger(0);
    public AtomicDouble pc40Price = new AtomicDouble(0.0);

    public AtomicInteger pc30Date = new AtomicInteger(0);
    public AtomicDouble pc30Price = new AtomicDouble(0.0);

    public AtomicInteger pc20Date = new AtomicInteger(0);
    public AtomicDouble pc20Price = new AtomicDouble(0.0);

    public AtomicInteger pc10Date = new AtomicInteger(0);
    public AtomicDouble pc10Price = new AtomicDouble(0.0);

    // entry date + n months
    public AtomicInteger ex1Date = new AtomicInteger(0);
    public AtomicDouble  ex1Price = new AtomicDouble(0.0);
    public AtomicInteger ex2Date = new AtomicInteger(0);
    public AtomicDouble  ex2Price = new AtomicDouble(0.0);
    public AtomicInteger ex3Date = new AtomicInteger(0);
    public AtomicDouble  ex3Price = new AtomicDouble(0.0);
    public AtomicInteger ex4Date = new AtomicInteger(0);
    public AtomicDouble  ex4Price = new AtomicDouble(0.0);
    public AtomicInteger ex5Date = new AtomicInteger(0);
    public AtomicDouble  ex5Price = new AtomicDouble(0.0);
    public AtomicInteger ex6Date = new AtomicInteger(0);
    public AtomicDouble  ex6Price = new AtomicDouble(0.0);
    public AtomicInteger ex7Date = new AtomicInteger(0);
    public AtomicDouble  ex7Price = new AtomicDouble(0.0);
    public AtomicInteger ex8Date = new AtomicInteger(0);
    public AtomicDouble  ex8Price = new AtomicDouble(0.0);
    public AtomicInteger ex9Date = new AtomicInteger(0);
    public AtomicDouble  ex9Price = new AtomicDouble(0.0);
    public AtomicInteger ex10Date = new AtomicInteger(0);
    public AtomicDouble  ex10Price = new AtomicDouble(0.0);
    public AtomicInteger ex11Date = new AtomicInteger(0);
    public AtomicDouble  ex11Price = new AtomicDouble(0.0);
    public AtomicInteger ex12Date = new AtomicInteger(0);
    public AtomicDouble  ex12Price = new AtomicDouble(0.0);

    /**
     * 2 days later
     */
    public AtomicInteger day2Date = new AtomicInteger(0);
    public AtomicDouble  day2Price = new AtomicDouble(0.0);
    public AtomicInteger day2PC10Date = new AtomicInteger(0);
    public AtomicDouble  day2PC10Price = new AtomicDouble(0.0);

    // find min and max within 1 month from entry date
    public AtomicInteger entry1MonthMaxDate = new AtomicInteger(0);
    public AtomicDouble entry1MonthMaxPrice = new AtomicDouble(0.0);
    public AtomicInteger entry1MonthMinDate = new AtomicInteger(0);
    public AtomicDouble entry1MonthMinPrice = new AtomicDouble(0.0);


    // "stop" prices, negative percentages of entry price
    public AtomicInteger pcDec5Date = new AtomicInteger(0);
    public AtomicDouble  pcDec5Price = new AtomicDouble(0.0);
    public AtomicInteger pcDec10Date = new AtomicInteger(0);
    public AtomicDouble  pcDec10Price = new AtomicDouble(0.0);
    public AtomicInteger pcDec15Date = new AtomicInteger(0);
    public AtomicDouble  pcDec15Price = new AtomicDouble(0.0);
    public AtomicInteger pcDec20Date = new AtomicInteger(0);
    public AtomicDouble  pcDec20Price = new AtomicDouble(0.0);
    public AtomicInteger pcDec25Date = new AtomicInteger(0);
    public AtomicDouble  pcDec25Price = new AtomicDouble(0.0);
    public AtomicInteger pcDec30Date = new AtomicInteger(0);
    public AtomicDouble  pcDec30Price = new AtomicDouble(0.0);
    public AtomicDouble avgVolumePrev30 = new AtomicDouble(0.0);
    public AtomicDouble avgPricePrev30 = new AtomicDouble(0.0);
    public AtomicDouble avgVolumePost30 = new AtomicDouble(0.0);
    public AtomicDouble avgPricePost30 = new AtomicDouble(0.0);
    public AtomicDouble totalVolumePrev30 = new AtomicDouble(0.0);
    public AtomicDouble totalPricePrev30 = new AtomicDouble(0.0);
    public AtomicDouble totalVolumePost30 = new AtomicDouble(0.0);
    public AtomicDouble totalPricePost30 = new AtomicDouble(0.0);
    public AtomicDouble slope30 = new AtomicDouble(0.0);
    public AtomicDouble slope3 = new AtomicDouble(0.0);

    public AtomicInteger entryNextDayOpenDate = new AtomicInteger(0);
    public AtomicDouble entryNextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble entryNextDayClosePrice = new AtomicDouble(0.0);

    public AtomicInteger pco10Date = new AtomicInteger(0);
    public AtomicDouble pco10Price = new AtomicDouble(0.0);
    public AtomicInteger pco10NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco10NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco10NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco11Date = new AtomicInteger(0);
    public AtomicDouble pco11Price = new AtomicDouble(0.0);
    public AtomicInteger pco11NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco11NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco11NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco12Date = new AtomicInteger(0);
    public AtomicDouble pco12Price = new AtomicDouble(0.0);
    public AtomicInteger pco12NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco12NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco12NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco13Date = new AtomicInteger(0);
    public AtomicDouble pco13Price = new AtomicDouble(0.0);
    public AtomicInteger pco13NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco13NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco13NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco14Date = new AtomicInteger(0);
    public AtomicDouble pco14Price = new AtomicDouble(0.0);
    public AtomicInteger pco14NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco14NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco14NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco15Date = new AtomicInteger(0);
    public AtomicDouble pco15Price = new AtomicDouble(0.0);
    public AtomicInteger pco15NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco15NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco15NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco16Date = new AtomicInteger(0);
    public AtomicDouble pco16Price = new AtomicDouble(0.0);
    public AtomicInteger pco16NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco16NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco16NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco17Date = new AtomicInteger(0);
    public AtomicDouble pco17Price = new AtomicDouble(0.0);
    public AtomicInteger pco17NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco17NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco17NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco18Date = new AtomicInteger(0);
    public AtomicDouble pco18Price = new AtomicDouble(0.0);
    public AtomicInteger pco18NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco18NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco18NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger pco19Date = new AtomicInteger(0);
    public AtomicDouble pco19Price = new AtomicDouble(0.0);
    public AtomicInteger pco19NextDayDate = new AtomicInteger(0);
    public AtomicDouble pco19NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble pco19NextDayClosePrice = new AtomicDouble(0.0);

    public AtomicInteger week1Date = new AtomicInteger(0);
    public AtomicDouble week1Price = new AtomicDouble(0.0);
    public AtomicInteger week1NextDayDate = new AtomicInteger(0);
    public AtomicDouble week1NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble week1NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger week2Date = new AtomicInteger(0);
    public AtomicDouble week2Price = new AtomicDouble(0.0);
    public AtomicInteger week2NextDayDate = new AtomicInteger(0);
    public AtomicDouble week2NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble week2NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger week3Date = new AtomicInteger(0);
    public AtomicDouble week3Price = new AtomicDouble(0.0);
    public AtomicInteger week3NextDayDate = new AtomicInteger(0);
    public AtomicDouble week3NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble week3NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicInteger week4Date = new AtomicInteger(0);
    public AtomicDouble week4Price = new AtomicDouble(0.0);
    public AtomicInteger week4NextDayDate = new AtomicInteger(0);
    public AtomicDouble week4NextDayOpenPrice = new AtomicDouble(0.0);
    public AtomicDouble week4NextDayClosePrice = new AtomicDouble(0.0);
    public AtomicDouble endOfYearPrice = new AtomicDouble(0.0);

    public AtomicInteger endOfYearDate = new AtomicInteger(0);


    public EntryExitPair(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public EntryExitPair() {

    }

    @Override
    public String toString() {
        return "EntryExitPair{" +
                "resultCode=" + resultCode +
                ", entryZScore=" + entryZScore +
                ", entryDate=" + entryDate +
                ", entryPrice=" + entryClosePrice +
                ", exitZScore=" + exitZScore +
                ", exitDate=" + exitDate +
                ", exitPrice=" + exitPrice +
                ", maxPriceAfterEntry=" + maxPriceAfterEntry +
                ", maxPriceDate=" + maxPriceDate +
                ", maxPriceZScore=" + maxPriceZScore +
                '}';
    }
}
