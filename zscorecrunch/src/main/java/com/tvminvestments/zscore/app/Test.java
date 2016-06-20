package com.tvminvestments.zscore.app;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.ZScoreAlgorithm;
import com.tvminvestments.zscore.app.reports.SplitScreener;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.CSVScenarioFactory;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by horse on 17/07/15.
 */
public class Test {

    private static final Logger logger = LogManager.getLogger(Test.class);

    public static double slope(double[] y, double[] x) {

        if(x.length != y.length) {
            logger.error("slope: x/y length mismatch");
            return -1;
        }

        int len = x.length;

        double xy[] = new double[len];
        double x2[] = new double[len];
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumClose = 0.0;
        double sumDate = 0.0;


        for(int i = 0; i < len; i++) {
            xy[i] = y[i] * x[i];
            x2[i] = Math.pow(x[i], 2);

            sumXY += xy[i];
            sumX2 += x2[i];
            sumClose += x[i];
            sumDate += y[i];
        }

        double slope = ((len * sumXY) - (sumDate * sumClose)) / ((len * sumX2) - Math.pow(sumDate, 2));

        return slope;
    }

    public static void main(String[] args) throws Exception {
        double[] y = new double[] { 0.555,0.666,0.777 };
        double[] x = new double[] { 0.888, 0.999, 0 };

        System.out.println(String.format("%.6f", slope(y, x)));

        SimpleRegression simpleRegression = new SimpleRegression();

        simpleRegression.addData(0.888, 0.555);
        simpleRegression.addData(0.999, 0.666);
        simpleRegression.addData(0, 0.777);



        System.out.println(String.format("%.6f", simpleRegression.getSlope()));
    }
}
