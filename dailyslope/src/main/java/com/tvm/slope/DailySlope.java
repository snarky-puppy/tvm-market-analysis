package com.tvm.slope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static yahoofinance.histquotes.HistQuotesRequest.DEFAULT_TO;

/**
 * Created by horse on 4/09/2016.
 */
public class DailySlope {

    private static final Logger logger = LogManager.getLogger(DailySlope.class);

    public static void main(String[] args) {
        DailySlope dailySlope = new DailySlope();
        dailySlope.run();
    }

    private void run() {
        List<Database.ActiveSymbol> activeSymbols = Database.getActiveSymbols();
        activeSymbols.forEach(this::processSymbol);
    }

    private void processSymbol(Database.ActiveSymbol symbol) {
        updateData(symbol);
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
