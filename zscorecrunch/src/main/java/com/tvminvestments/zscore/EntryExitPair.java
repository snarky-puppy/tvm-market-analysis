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

    public AtomicDouble avgVolumePrev30 = new AtomicDouble(0.0);
    public AtomicDouble avgPricePrev30 = new AtomicDouble(0.0);


    public AtomicInteger pc1OpenDate = new AtomicInteger(0);
    public AtomicDouble pc1OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc2OpenDate = new AtomicInteger(0);
    public AtomicDouble pc2OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc3OpenDate = new AtomicInteger(0);
    public AtomicDouble pc3OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc4OpenDate = new AtomicInteger(0);
    public AtomicDouble pc4OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc5OpenDate = new AtomicInteger(0);
    public AtomicDouble pc5OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc6OpenDate = new AtomicInteger(0);
    public AtomicDouble pc6OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc7OpenDate = new AtomicInteger(0);
    public AtomicDouble pc7OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc8OpenDate = new AtomicInteger(0);
    public AtomicDouble pc8OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc9OpenDate = new AtomicInteger(0);
    public AtomicDouble pc9OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc10OpenDate = new AtomicInteger(0);
    public AtomicDouble pc10OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc15OpenDate = new AtomicInteger(0);
    public AtomicDouble pc15OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc20OpenDate = new AtomicInteger(0);
    public AtomicDouble pc20OpenPrice = new AtomicDouble(0.0);
    public AtomicInteger pc25OpenDate = new AtomicInteger(0);
    public AtomicDouble pc25OpenPrice = new AtomicDouble(0.0);

    public AtomicDouble eoy1994ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy1995ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy1996ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy1997ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy1998ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy1999ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2000ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2001ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2002ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2003ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2004ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2005ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2006ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2007ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2008ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2009ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2010ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2011ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2012ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2013ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2014ClosePrice = new AtomicDouble(0.0);
    public AtomicDouble eoy2015ClosePrice = new AtomicDouble(0.0);

    public AtomicDouble rsi7_0Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_1Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_2Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_3Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_4Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_5Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_6Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_7Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_8Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_9Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_10Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_11Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_12Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_13Days = new AtomicDouble(0.0);
    public AtomicDouble rsi7_14Days = new AtomicDouble(0.0);

    public AtomicDouble rsi14_0Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_1Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_2Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_3Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_4Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_5Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_6Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_7Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_8Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_9Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_10Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_11Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_12Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_13Days = new AtomicDouble(0.0);
    public AtomicDouble rsi14_14Days = new AtomicDouble(0.0);

    public AtomicInteger next0DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next0DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next1DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next1DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next2DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next2DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next3DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next3DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next4DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next4DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next5DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next5DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next6DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next6DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next7DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next7DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next8DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next8DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next9DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next9DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next10DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next10DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next11DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next11DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next12DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next12DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next13DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next13DayOpenPrice = new AtomicDouble(0.0);
    public AtomicInteger next14DayOpenDate = new AtomicInteger(0);
    public AtomicDouble next14DayOpenPrice = new AtomicDouble(0.0);
            


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
                '}';
    }
}
