package com.tvm.crunch.apps;

import com.tvm.crunch.*;
import com.tvm.crunch.database.FileDatabaseFactory;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by horse on 23/08/2016.
 */
public class Strategy4BackTest extends MarketExecutor {

    public static void main(String[] args) {
        boolean visualvm = false;

        Util.waitForKeypress(visualvm);

        if(false) {
            Strategy4BackTest strategy4BackTest = new Strategy4BackTest("NYSE");
            strategy4BackTest.executeAllSymbols();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), Strategy4BackTest::new);
        }

        Util.waitForKeypress(visualvm);
    }

    public Strategy4BackTest(String market) {
        super(market);
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

            if(slope <= -0.2) {
                //double avgVolume = new Mean().evaluate(data.volume, idx, 21);
                double avgVolume = Arrays.stream(data.volume, idx, idx + 21).average().getAsDouble();
                double avgClose = new Mean().evaluate(data.close, idx, 21);
                if (avgClose * avgVolume >= 10000000) {
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
                    r.dollarVolume = avgVolume * avgClose;

                    int n = idx + Strategy4Result.holdMin;
                    for(int i = 0, q = n + i; q < data.open.length && i < Strategy4Result.numHolds; i++, q++) {
                        r.holdDate[i] = data.date[q];
                        r.holdPc[i] = change(data.open[n], data.open[q]);
                        r.holdPrice[i] = data.open[q];
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
