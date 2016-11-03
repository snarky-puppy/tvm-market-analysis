package com.tvm.crunch.apps;

import com.tvm.crunch.*;
import com.tvm.crunch.database.DatabaseFactory;
import com.tvm.crunch.database.FileDatabaseFactory;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Trend Continuation strategy back test
 *
 * Created by horse on 23/07/2016.
 */
public class TrendContBackTest extends MarketExecutor {

    public static void main(String[] args) {

        boolean visualvm = false;

        Util.waitForKeypress(visualvm);

        if(true) {
            TrendContBackTest trendContBackTest = new TrendContBackTest("ASX", new FileDatabaseFactory());
            trendContBackTest.executeAllSymbols();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), TrendContBackTest::new);
        }

        Util.waitForKeypress(visualvm);
    }

    private TrendContBackTest(String market, DatabaseFactory databaseFactory) {
        super(market, databaseFactory);
    }

    @Override
    protected ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue) {
        return new ResultWriter(queue) {
            @Override
            protected String getProjectName() {
                return "TrendCont";
            }

            @Override
            protected String getMarket() {
                return market;
            }
        };
    }

    @Override
    protected void processSymbol(String symbol) {

        double [] vma50history = new double[5];
        int vmaHistoryIdx = 0;

        Data data = db().loadData(market, symbol);

        boolean ema30EverCrossed50 = false;
        boolean ema50EverCrossed100 = false;
        double high = 0.0;

        // we can only have a meaningful start from the 100th day since
        // one of the triggers is an 100 day average

        // gather high from previous 99 days
        int idx = 0;
        do {
            high = Math.max(high, data.close[idx]);
            idx++;
        } while(idx < 99 && idx < data.close.length);

        // start off on day 100 to give the averages something to chew
        while(idx < data.close.length) {
            try {
                // ema
                double ema30 = Data.ema(idx, 30, data.close);
                double ema50 = Data.ema(idx, 50, data.close);
                double ema100 = Data.ema(idx, 100, data.close);
                double vma20 = Data.simpleMovingAverage(idx, 20, data.volume);
                double vma50 = Data.simpleMovingAverage(idx, 50, data.volume);
                vma50history[vmaHistoryIdx++ % vma50history.length] = vma50;

                boolean highest = data.close[idx] > high;
                high = Math.max(high, data.close[idx]);

                boolean vma20Flag = vma20 > 50000;
                boolean dailyVolFlag = false;

                for (int i = 0; i < vma50history.length && !dailyVolFlag; i++) {
                    if (data.volume[idx] > vma50history[i] * 1.5)
                        dailyVolFlag = true;
                }

                if (ema30 > ema50) {
                    ema30EverCrossed50 = true;
                }

                if (ema50 > ema100) {
                    ema50EverCrossed100 = true;
                }

                boolean trigger = dailyVolFlag && vma20Flag && highest &&
                        ema30EverCrossed50 && ema50EverCrossed100;

                if (trigger) {
                    Design10Result r = Design10Result.create(data, idx);
                    enqueueResult(r);
                }

            } catch(DataException e) {
            }
            idx++;
        }

    }
}
