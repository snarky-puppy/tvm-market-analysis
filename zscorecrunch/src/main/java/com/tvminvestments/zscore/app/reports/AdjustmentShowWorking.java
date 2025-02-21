package com.tvminvestments.zscore.app.reports;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.app.Conf;
import com.tvminvestments.zscore.app.Util;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 17/07/15.
 */
public class AdjustmentShowWorking {

    private static final Logger logger = LogManager.getLogger(AdjustmentShowWorking.class);

    public static void main(String[] args) throws Exception {
        AdjustmentShowWorking adjustmentShowWorking = new AdjustmentShowWorking();
        adjustmentShowWorking.doit("test", "TGTX");
    }

    int[] dates;
    double[] work;
    double[] orig;
    List<List<Double>> adjustments;

    private void doit(String dbName, String symbol) throws Exception {
        adjustments = new ArrayList<>();
        Database database = DatabaseFactory.createDatabase(dbName);
        CloseData data = database.loadData(symbol);
        work = new double[data.close.length];
        orig = new double[data.close.length];
        dates = new int[data.close.length];
        System.arraycopy(data.close, 0, orig, 0, data.close.length);
        System.arraycopy(data.close, 0, work, 0, data.close.length);
        System.arraycopy(data.date, 0, dates, 0, data.close.length);
        double maxPrice = data.close[0];
        double lastPrice = data.close[0];

        boolean maxBreach;

        for (int i = 1; i < data.close.length; i++) {
            double thisPrice = work[i];

            maxPrice = Math.max(maxPrice, thisPrice);
            maxBreach = maxPrice >= Conf.MAX_ADJUSTMENTS;
            double ratio = thisPrice / lastPrice;

            if (maxBreach) {
                if ((ratio <= Conf.MIN_ADJ_RATIO) || (ratio >= Conf.MAX_ADJ_RATIO)) {
                    for(int r = i; r >= 0; r--) {
                        work[r] = work[r] * ratio;
                    }

                    List<Double> log = new ArrayList<>();
                    for(int r = 0; r <= i; r++) {
                        log.add(work[r]);
                        //log.add(0.0);
                        //log.add(ratio);
                    }

                    adjustments.add(log);
                }
            }

            lastPrice = thisPrice;
        }

        // output results
        String fname = Util.getOutFile(dbName, String.format("%s-adjust", symbol));
        BufferedWriter bw = new BufferedWriter(new FileWriter(fname));
        bw.write("Date,Orig Series");
        for(int i = 1; i <= adjustments.size(); i++) {
            bw.write(String.format(",adj%d", i));
        }
        bw.write("\n");

        for(int i = 0; i < orig.length; i++) {
            bw.write(String.format("%d,%f", dates[i], orig[i]));
            for(int r = 0; r < adjustments.size(); r++) {
                List<Double> l = adjustments.get(r);
                if(i < l.size()) {
                    bw.write(String.format(",%f", l.get(i)));
                } else {
                    bw.write(",");
                }
            }
            bw.write("\n");
        }

        bw.flush();
        bw.close();
    }

}
