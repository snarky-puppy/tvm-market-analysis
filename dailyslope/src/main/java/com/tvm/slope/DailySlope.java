package com.tvm.slope;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static yahoofinance.histquotes.HistQuotesRequest.DEFAULT_TO;

/**
 * Produce a slope report from Yahoo data
 * <p>
 * Created by horse on 4/09/2016.
 */
public class DailySlope {

    private static final Path OUT_DIR = Paths.get("/Users/horse/Google Drive/Stuff from Matt/slope");

    private static String getHeader() {
        return "symbol,exchange" +
                ",category" +
                ",d0" +
                ",d7" +
                ",d14" +
                ",d21" +
                ",pl7" +
                ",pl14" +
                ",pl21" +
                ",slope" +
                ",dollarVolume";
    }

    public static void main(String[] args) throws IOException {
        DailySlope dailySlope = new DailySlope();
        dailySlope.run();

        //1 | NYSE      | A      | Health Care            | 2016-09-18
        //Database.ActiveSymbol activeSymbol = new Database.ActiveSymbol(4402, "TTC", "NYSE", "Industrials", null);
        //dailySlope.processSymbol(activeSymbol);
    }

    private static long timestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        return Long.parseLong(sdf.format(date));
    }

    private void run() throws IOException {
        List<Result> results = new ArrayList<>();
        for (Database.ActiveSymbol symbol : Database.getActiveSymbols()) {
            Result r = processSymbol(symbol);
            if (r != null)
                results.add(r);
        }
        if (results.size() > 0) {
            writeResults(results);
        }
    }

    private void writeResults(List<Result> results) throws IOException {
        FileWriter fw = new FileWriter(String.valueOf(OUT_DIR.resolve("slope-" + timestamp() + ".csv")));
        fw.write(getHeader() + "\n");
        for (Result r : results) {
            fw.write(r.toString() + "\n");
        }
        fw.close();
    }

    private Result processSymbol(Database.ActiveSymbol activeSymbol) {
        updateData(activeSymbol);
        Database.YahooData data = Database.getYahooData(activeSymbol);
        if (data != null) {
            return regression(activeSymbol, data);
        }
        return null;
    }

    private double change(double start, double end) {
        return ((end - start) / start);
    }

    private Result regression(Database.ActiveSymbol activeSymbol, Database.YahooData data) {
        double p1, p2, p3;
        double[] c = data.close;
        int idx = 0;

        p1 = change(c[idx], c[idx + 7 - 1]);
        p2 = change(c[idx], c[idx + 14 - 1]);
        p3 = change(c[idx], c[idx + 21 - 1]);

        SimpleRegression simpleRegression = new SimpleRegression();

        simpleRegression.addData(1, p1);
        simpleRegression.addData(2, p2);
        simpleRegression.addData(3, p3);

        double slope = simpleRegression.getSlope();

        if (slope <= -0.2) {
            //double avgVolume = new Mean().evaluate(data.volume, idx, 21);
            double avgVolume = Arrays.stream(data.volume, idx, idx + 21 - 1).average().getAsDouble();
            double avgClose = new Mean().evaluate(data.close, idx, 21 - 1);
            if (avgClose * avgVolume >= 10000000) {
                Result r = new Result();
                r.symbol = activeSymbol.symbol;
                r.exchange = activeSymbol.exchange;
                r.category = activeSymbol.sector;

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
        updateData(symbol, false);
    }

    private void updateData(Database.ActiveSymbol symbol, boolean secondRun) {

        if (symbol.lastCheck != null && LocalDate.now().isEqual(symbol.lastCheck))
            return;

        LocalDate dt = Database.getLastDate(symbol);

        Calendar c = Calendar.getInstance();
        c.set(dt.getYear(), dt.getMonthValue() - 1, dt.getDayOfMonth());

        System.out.println(symbol.symbol + ": from " + dt);

        try {
            Stock stock = YahooFinance.get(symbol.symbol.replace('.', '-'), c, DEFAULT_TO, Interval.DAILY);
            Database.saveData(symbol, stock);
            Database.updateLastCheck(symbol);
        } catch (SocketTimeoutException | FileNotFoundException e) {
            e.printStackTrace();
            if (!secondRun) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                updateData(symbol, true);
            } else
                Database.updateLastCheck(symbol);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private class Result {
        double slope;
        double dollarVolume;
        String symbol;
        String exchange;
        String category;
        // dates
        LocalDate d0;
        LocalDate d7;
        LocalDate d14;
        LocalDate d21;
        // profit/loss
        double pl7;
        double pl14;
        double pl21;

        @Override
        public String toString() {
            return symbol +
                    "," + exchange +
                    "," + category +
                    "," + d0 +
                    "," + d7 +
                    "," + d14 +
                    "," + d21 +
                    String.format(",%.4f", pl7) +
                    String.format(",%.4f", pl14) +
                    String.format(",%.4f", pl21) +
                    String.format(",%.4f", slope) +
                    String.format(",%.4f", dollarVolume);
        }
    }
}
