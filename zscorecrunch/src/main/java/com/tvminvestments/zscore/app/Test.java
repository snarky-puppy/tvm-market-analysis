package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.ZScoreAlgorithm;
import com.tvminvestments.zscore.app.reports.SplitScreener;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.CSVScenarioFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by horse on 17/07/15.
 */
public class Test {

    private static final Logger logger = LogManager.getLogger(Test.class);

    public static double slope(int dates[], double close[]) {

        if(dates == null || close == null || dates.length == 0 || close.length == 0)
            return -1;

        int cnt = dates.length;

        double xy[] = new double[cnt];
        double x2[] = new double[cnt];
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumClose = 0.0;
        double sumDate = 0.0;

        logger.info("cnt="+cnt);

        for(int i = 0, j = 0; i < dates.length; i++, j++) {
            xy[j] = ((double)dates[i]) * close[i];
            x2[j] = Math.pow(dates[i], 2);

            sumXY += xy[j];
            sumX2 += x2[j];
            sumClose += close[i];
            sumDate += dates[i];
        }

        /*
        double sumDatePow2 = Math.pow(sumDate, 2);

        double calcAA = (cnt * sumXY);
        double calcAB = (sumDate * sumClose);
        double calcA = calcAA - calcAB;

        double calcBA = (cnt * sumX2);
        double calcB = calcBA  - sumDatePow2;

        double slope =  calcA / calcB;
        */

        double slope = ((cnt * sumXY) - (sumDate * sumClose)) / ((cnt * sumX2) - Math.pow(sumDate, 2));

        return slope;

        //if(!Double.isNaN(slope))
        //    slopeVal.set(slope);
    }

    public static void main(String[] args) throws Exception {

        System.out.println(DateUtil.minusDays(20150302, 3));

    }
}
