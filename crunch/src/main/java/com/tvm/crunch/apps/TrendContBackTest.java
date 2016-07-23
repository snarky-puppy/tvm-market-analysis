package com.tvm.crunch.apps;

import com.google.common.util.concurrent.AtomicDouble;
import com.tvm.crunch.*;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by horse on 23/07/2016.
 */
public class TrendContBackTest extends MarketExecutor {

    public static void main(String[] args) {
        boolean test = false;

        if(test) {
            TrendContBackTest trendContBackTest = new TrendContBackTest("test");
            trendContBackTest.execute();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), new MarketExecutorFactory() {
                @Override
                public MarketExecutor create(String market) {
                    return new TrendContBackTest(market);
                }
            });
        }
    }

    public TrendContBackTest(String market) {
        super(market);
    }

    class MyResult extends Result {

        public String symbol;
        public String exchange;

        public Double closePrev30DayAvg;
        public Double volPrev30DayAvg;
        public Integer date;
        public Double close;
        public Double zscore30Day;

        public Point nextDayOpen;

        // 10, 20, 30, 40, 50
        public static final int targetPercents = 5;
        public Point[] targetPc = new Point[5];
        public Double[] closePcPrev30DayAvg = new Double[5];
        public Double[] volPcPrev30DayAvg = new Double[5];

        public Point closeLastRecorded;
        public Double closeLastRecorded30DayAvg;
        public Double volLastRecorded30DayAvg;

        public static final int eoyStart = 1995;
        public static final int eoyEnd = 2015;
        public static final int years = eoyEnd - eoyStart;
        public Point[] eoy = new Point[years+1];


        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("");
            sb.append(symbol);
            append(sb, exchange);
            append(sb, closePrev30DayAvg);
            append(sb, volPrev30DayAvg);
            append(sb, date);
            append(sb, close);
            append(sb, zscore30Day);

            appendPointOpen(sb, nextDayOpen);

            for(int i = 0; i < targetPercents; i++) {
                appendPointOpen(sb, targetPc[i]);
                append(sb, closePcPrev30DayAvg[i]);
                append(sb, volPcPrev30DayAvg[i]);
            }

            appendPointClose(sb, closeLastRecorded);
            append(sb, closeLastRecorded30DayAvg);
            append(sb, volLastRecorded30DayAvg);

            for(Point p : eoy)
                appendPointClose(sb, p);

            sb.append("\n");
            return sb.toString();
        }
    }

    private class MyResultWriter extends ResultWriter {

        MyResultWriter(BlockingQueue<Result> blockingQueue) {
            super(blockingQueue);
        }

        @Override
        protected String getHeader() {
            final StringBuilder sb = new StringBuilder("");
            sb.append("Symbol");
            sb.append(",Exchange");
            sb.append(",Close Prev 30 Day Avg");
            sb.append(",Volume Prev 30 Day Avg");
            sb.append(",Date");
            sb.append(",Close");
            sb.append(",ZScore 30 Day");
            sb.append(",Next Day Date");
            sb.append(",Next Day Open Price");

            for(int i = 0, p = 10; i < MyResult.targetPercents; i++, p += 10) {
                sb.append(String.format(",%d%% Target Next Day Date", p));
                sb.append(String.format(",%d%% Target Next Day Open", p));
                sb.append(String.format(",%d%% Target Prev 30 Day Close Avg", p));
                sb.append(String.format(",%d%% Target Prev 30 Day Volume Avg", p));
            }

            sb.append(",Last Recorded Date");
            sb.append(",Last Recorded Close");
            sb.append(",Last Recorded Prev 30 Day Close Avg");
            sb.append(",Last Recorded Prev 30 Day Volume Avg");

            for(int y = MyResult.eoyStart; y <= MyResult.eoyEnd; y++) {
                sb.append(String.format(",%d End Of Year Date", y));
                sb.append(String.format(",%d End Of Year Close", y));
            }

            sb.append("\n");
            return sb.toString();
        }

        @Override
        protected String getMarket() {
            return market;
        }

        protected String getProjectName() {
            return "TrendCont";
        }
    }

    @Override
    protected ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue) {
        return new MyResultWriter(queue);
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
                    MyResult r = new MyResult();
                    r.symbol = symbol;
                    r.exchange = market;

                    r.closePrev30DayAvg = data.avgPricePrev30Days(idx);
                    r.volPrev30DayAvg = data.avgVolumePrev30Days(idx);

                    r.date = data.date[idx];
                    r.close = data.close[idx];

                    r.zscore30Day = data.zscore(idx, 30);

                    r.nextDayOpen = data.findNDayPoint(idx, 1);

                    for(int i = 0, p = 10; i < MyResult.targetPercents; i++, p += 10) {
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

                    for(int i = 0, y = MyResult.eoyStart; y <= MyResult.eoyEnd; y++, i++) {
                        r.eoy[i] = data.findEndOfYearPrice(y);
                    }

                    enqueueResult(r);
                }
                idx++;
            }

    }

    @Override
    protected Database db() {
        return new FileDatabase();
    }

    @Override
    protected MarketExecutor createInstance(String market) {
        return new TrendContBackTest(market);
    }
}
