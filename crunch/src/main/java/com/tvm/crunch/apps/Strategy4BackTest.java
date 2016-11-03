package com.tvm.crunch.apps;

import com.tvm.crunch.*;
import com.tvm.crunch.database.DatabaseFactory;
import com.tvm.crunch.database.FileDatabaseFactory;
import com.tvm.crunch.database.YahooDatabaseFactory;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by horse on 23/08/2016.
 */
public class Strategy4BackTest extends MarketExecutor {

    final int DOL_VOL_CUTOFF = 3000000;
    final double SLOPE_CUTOFF = 0;

    public static void main(String[] args) throws IOException {
        boolean visualvm = false;

        Util.waitForKeypress(visualvm);

        YahooDatabaseFactory factory = new YahooDatabaseFactory();
        //DatabaseFactory factory = new FileDatabaseFactory();

        if(false) {
            //Strategy4BackTest strategy4BackTest = new Strategy4BackTest("ASX", factory);
            //strategy4BackTest.executeAllSymbols();
            Strategy4BackTest strategy4BackTest = new Strategy4BackTest("NASDAQ", factory);
            //strategy4BackTest.processSymbol("CECO");

        } else {
            factory.updateFromWeb(100, 0);

            executeAllMarkets(factory, Strategy4BackTest::new);
        }

        Util.waitForKeypress(visualvm);
    }

    public Strategy4BackTest(String market, DatabaseFactory databaseFactory) {
        super(market, databaseFactory);
    }

    @Override
    protected ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue) {
        return new ResultWriter(queue) {
            @Override
            protected String getProjectName() {
                return "News";
            }

            @Override
            protected String getMarket() {
                return market;
            }
        };
    }

    private double change(double start, double end) {
        return ((end - start)/start);
    }

    @Override
    protected void processSymbol(String symbol) {
        Data data = db().loadData(market, symbol);

        if(data == null)
            return;

        double[] c = data.close;

        int idx = 0;
        // 21 days is how many we need for the slope calc and dollar volume.
        // if we trigger with less than the hold time left then we can still report it.
        while(idx + 21 - 1 < data.close.length) {
            double p1, p2, p3;

            p1 = change(c[idx], c[idx + 7-1]);
            p2 = change(c[idx], c[idx + 14-1]);
            p3 = change(c[idx], c[idx + 21-1]);

            SimpleRegression simpleRegression = new SimpleRegression();

            simpleRegression.addData(1, p1);
            simpleRegression.addData(2, p2);
            simpleRegression.addData(3, p3);

            double slope = simpleRegression.getSlope();

            if(slope <= SLOPE_CUTOFF) {
                //double avgVolume = new Mean().evaluate(data.volume, idx, 21);
                double avgVolume = Arrays.stream(data.volume, idx, idx + 21).average().getAsDouble();
                double avgClose = new Mean().evaluate(data.close, idx, 21);
                if (avgClose * avgVolume >= DOL_VOL_CUTOFF) {
                    Strategy4Result r = new Strategy4Result();
                    r.symbol = symbol;
                    r.exchange = market;

                    r.d0 = data.date[idx];
                    r.d7 = data.date[idx + 7 - 1];
                    r.d14 = data.date[idx + 14 - 1];
                    r.d21 = data.date[idx + 21 - 1];

                    r.pl7 = p1;
                    r.pl14 = p2;
                    r.pl21 = p3;
                    r.slope = slope;
                    r.dollarVolume21Day = avgVolume * avgClose;

                    int threeMonthsInDays = (21*3);
                    if(idx + threeMonthsInDays < data.close.length) {

                        double avgClose3Month = 0;
                        double avgVol3Month = 0;
                        OptionalDouble optionalDouble = null;
                        try {
                            optionalDouble = Arrays.stream(data.volume, idx, idx + threeMonthsInDays).average();
                        } catch(ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                            System.out.println(e);
                            System.out.println("idx="+idx+", len="+data.close.length);
                            System.exit(1);
                        }
                        if (optionalDouble.isPresent())
                            avgVol3Month = optionalDouble.getAsDouble();
                        avgClose3Month = new Mean().evaluate(data.close, idx, threeMonthsInDays);
                        r.dollarVolume3Month = avgVol3Month * avgClose3Month;
                    } else
                        r.dollarVolume3Month = -1;

                    int n = idx + Strategy4Result.holdMin;
                    for(int i = 0, q = n + i; q < data.open.length && i < Strategy4Result.numHolds; i++, q++) {
                        r.holdDate[i] = data.date[q];
                        r.holdOpenPc[i] = change(data.open[n], data.open[q]);
                        r.holdOpenPrice[i] = data.open[q];
                        r.holdClosePc[i] = change(data.close[n], data.close[q]);
                        r.holdClosePrice[i] = data.close[q];
                    }

                    List<NewsDB.NewsRow> newsResult = new NewsDB().findNews(data.date[idx], symbol);
                    if(newsResult != null) {
                        for(NewsDB.NewsRow row : newsResult) {
                            if(r.news == null)
                                r.news = row.news;
                            else
                                r.news += row.news + ";";
                            r.category = row.category;
                        }
                    } else {
                        r.news = "";
                        r.category = "";
                    }
                    enqueueResult(r);
                }
            }
            idx++;
        }

    }
}
