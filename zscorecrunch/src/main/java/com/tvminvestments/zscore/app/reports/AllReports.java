package com.tvminvestments.zscore.app.reports;

/**
 * Created by horse on 17/07/15.
 */
public class AllReports {
    public static void main(String[] args) throws Exception {
        DataGaps dataGaps = new DataGaps();
        SplitScreener splitScreener = new SplitScreener();

        dataGaps.scan();
        splitScreener.scan();
    }
}
