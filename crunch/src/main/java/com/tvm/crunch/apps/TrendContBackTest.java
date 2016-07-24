package com.tvm.crunch.apps;

import com.tvm.crunch.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Trend Continuation strategy back test
 *
 * Created by horse on 23/07/2016.
 */
public class TrendContBackTest extends MarketExecutor {

    public static void main(String[] args) {

        if(true) {
            TrendContBackTest trendContBackTest = new TrendContBackTest("test");
            trendContBackTest.execute();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), TrendContBackTest::new);
        }
    }

    private TrendContBackTest(String market) {
        super(market);
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

                for(int i = 0; i < vma50history.length && !dailyVolFlag; i++) {
                    if(data.volume[idx] > vma50history[i] * 1.5)
                        dailyVolFlag = true;
                }

                if(ema30 > ema50) {
                    ema30EverCrossed50 = true;
                }

                if(ema50 > ema100) {
                    ema50EverCrossed100 = true;
                }

                boolean trigger = dailyVolFlag && vma20Flag && highest &&
                        ema30EverCrossed50 && ema50EverCrossed100;

                if(trigger) {
                    Design10Result r = new Design10Result();
                    r.symbol = symbol;
                    r.exchange = market;

                    r.closePrev30DayAvg = data.avgPricePrev30Days(idx);
                    r.volPrev30DayAvg = data.avgVolumePrev30Days(idx);

                    r.date = data.date[idx];
                    r.close = data.close[idx];

                    r.zscore30Day = data.zscore(idx, 30);

                    r.nextDayOpen = data.findNDayPoint(idx, 1);

                    for(int i = 0, p = 10; i < Design10Result.targetPercents; i++, p += 10) {
                        Point closePt = data.findPCIncrease(idx, p, data.close);
                        if(closePt != null) {
                            Point nextPoint = data.findNDayPoint(closePt.index, 1);
                            if(nextPoint != null) {
                                r.targetPc[i] = nextPoint;
                                r.closePcPrev30DayAvg[i] = data.avgPricePrev30Days(nextPoint.index);
                                r.volPcPrev30DayAvg[i] = data.avgVolumePrev30Days(nextPoint.index);
                            }
                        }
                    }

                    Point lastRecorded = new Point(data, data.date.length - 1);
                    r.closeLastRecorded = lastRecorded;
                    r.closeLastRecorded30DayAvg = data.avgPricePrev30Days(lastRecorded.index);
                    r.volLastRecorded30DayAvg = data.avgVolumePrev30Days(lastRecorded.index);

                    for(int i = 0, y = Design10Result.eoyStart; y <= Design10Result.eoyEnd; y++, i++) {
                        r.eoy[i] = data.findEndOfYearPrice(y);
                    }

                    enqueueResult(r);
                }
                idx++;
            }

    }
}
