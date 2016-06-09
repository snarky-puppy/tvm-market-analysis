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

        int dates[] = {
                20141114,
                20141117,
                20141118,
                20141119,
                20141120,
                20141121,
                20141124,
                20141125,
                20141126,
                20141128,
                20141201,
                20141202,
                20141203,
                20141204,
                20141205,
                20141208,
                20141209,
                20141210,
                20141211,
                20141212,
                20141215,
                20141216,
                20141217,
                20141218,
                20141219,
                20141222,
                20141223,
                20141224,
                20141226,
                20141229,
                20141230,
                20141231,
                20150102,
                20150105,
                20150106,
                20150107,
                20150108,
                20150109,
                20150112,
                20150113,
                20150114
        };

        double close[] = {
                3.98,
                3.995,
                4.02,
                4.18,
                4.22,
                4.1,
                4.23,
                4.22,
                4.0868,
                3.97,
                4.07,
                4.13,
                4.13,
                4.05,
                4.22,
                4.22,
                4.165,
                4.15,
                4.15,
                3.98,
                3.99,
                4,
                3.97,
                4.1,
                4,
                4.1399,
                4.16,
                4.16,
                4.17,
                3.97,
                3.97,
                3.97,
                3.98,
                3.9825,
                3.9,
                3.99,
                3.88,
                3.95,
                3.9,
                3.85,
                3.75

        };
        System.out.println(String.format("%.6f", slope(dates, close)));
        System.out.println(DateUtil.minusDays(20150114, 30));

    }
}
