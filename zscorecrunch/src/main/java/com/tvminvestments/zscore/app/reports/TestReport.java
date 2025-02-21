package com.tvminvestments.zscore.app.reports;

/**
 * Created by horse on 17/07/15.
 */
public class TestReport {
    public static void main(String[] args) throws Exception {
        DataGaps dataGaps = new DataGaps();
        dataGaps.scanMarket("test");

        SplitScreener splitScreener = new SplitScreener();
        splitScreener.scanMarket("test");
    }
}
