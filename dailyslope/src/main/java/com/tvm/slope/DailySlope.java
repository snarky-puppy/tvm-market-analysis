package com.tvm.slope;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static yahoofinance.histquotes.HistQuotesRequest.DEFAULT_TO;

/**
 * Created by horse on 4/09/2016.
 */
public class DailySlope {

    private static final Logger logger = LogManager.getLogger(DailySlope.class);

    class Result {
        public String symbol;
        public String exchange;
        public String category;

        // dates
        public LocalDate d0;
        public LocalDate d7;
        public LocalDate d14;
        public LocalDate d21;

        // profit/loss
        public double pl7;
        public double pl14;
        public double pl21;
        public double slope;
        public double dollarVolume;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Result{");
            sb.append("symbol='").append(symbol).append('\'');
            sb.append(", exchange='").append(exchange).append('\'');
            sb.append(", category='").append(category).append('\'');
            sb.append(", d0=").append(d0);
            sb.append(", d7=").append(d7);
            sb.append(", d14=").append(d14);
            sb.append(", d21=").append(d21);
            sb.append(", pl7=").append(pl7);
            sb.append(", pl14=").append(pl14);
            sb.append(", pl21=").append(pl21);
            sb.append(", slope=").append(slope);
            sb.append(", dollarVolume=").append(dollarVolume);
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        DailySlope dailySlope = new DailySlope();
        dailySlope.run();
    }

    private void run() {
        List<Database.ActiveSymbol> activeSymbols = Database.getActiveSymbols();
        activeSymbols.forEach(this::processSymbol);
    }

    private void processSymbol(Database.ActiveSymbol activeSymbol) {
        updateData(activeSymbol);
        Database.YahooData data = Database.getYahooData(activeSymbol);
        if(data != null) {
            Result rs = regression(activeSymbol, data);
            if(rs != null)
                System.out.println(rs);
        }
    }

    private double change(double start, double end) {
        return ((end - start)/start);
    }

    private Result regression(Database.ActiveSymbol activeSymbol, Database.YahooData data) {
        double p1, p2, p3;
        double[] c = data.close;
        int idx = 0;

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
                Result r = new Result();
                r.symbol = activeSymbol.symbol;
                r.exchange = activeSymbol.exchange;

                r.d0 = data.date[idx];
                r.d7 = data.date[idx + 7 - 1];
                r.d14 = data.date[idx + 14 - 1];
                r.d21 = data.date[idx + 21 - 1];

                r.pl7 = p1;
                r.pl14 = p2;
                r.pl21 = p3;
                r.slope = slope;
                r.dollarVolume = avgVolume * avgClose;

                return r;
            }
        }
        return null;
    }

    private void updateData(Database.ActiveSymbol symbol) {
        LocalDate dt = Database.getLastDate(symbol);

        System.out.println(symbol.symbol + "-" + dt);

        Calendar c = Calendar.getInstance();
        c.set(dt.getYear(), dt.getMonthValue() - 1, dt.getDayOfMonth());

        try {
            Stock stock = YahooFinance.get(symbol.symbol.replace('.', '-'), c, DEFAULT_TO, Interval.DAILY);
            Database.saveData(symbol, stock);
            Database.updateLastCheck(symbol);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
