package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adjust our database
 *
 * Created by horse on 24/02/15.
 */
public class Adjustment {

    private static final Logger logger = LogManager.getLogger(Adjustment.class);

    public static void main(String[] args) throws Exception {
        Adjustment adjustment = new Adjustment();
        adjustment.scanMarket("test");
    }

    public Adjustment() {
    }

    public void scanSymbol(Database database, String symbol, int afterDate) throws Exception {
        logger.info(String.format("%s/%s", database.getMarket(), symbol));
        boolean adjusted = false;
        CloseData data = database.loadData(symbol);
        data.undoAdjustments();
        double maxPrice = data.close[0];
        boolean maxBreach;
        double lastPrice = data.close[0];
        int adjCount = 0;

        for (int i = 1; i < data.close.length; i++) {
            double thisPrice = data.close[i];
            int thisDate = data.date[i];

            maxPrice = Math.max(maxPrice, thisPrice);
            maxBreach = maxPrice >= Conf.MIN_ADJ_VALUE;
            double ratio = thisPrice / lastPrice;

            if (maxBreach) {
                if ((ratio <= Conf.MIN_ADJ_RATIO) || (ratio >= Conf.MAX_ADJ_RATIO)) {
                    if(afterDate == -1 || thisDate > afterDate) {
                        logger.info(String.format("Adjusting %s from %d by %f", symbol, thisDate, ratio));
                        data.adjustClose(i - 1, ratio);
                        adjusted = true;
                    }
                    adjCount ++;
                }
            }

            if(adjCount > Conf.MAX_ADJUSTMENTS) {
                logger.info(String.format("[%s]: Too many adjustments [%d] max=", symbol, adjCount, Conf.MAX_ADJUSTMENTS));
                data.undoAdjustments();
                break;
            }

            lastPrice = thisPrice;
        }
        if (adjusted) {
            logger.info("Writing file...");
            database.rewrite(data);
        }
    }

    public void scanMarket(String market) throws Exception {
        Database database = DatabaseFactory.createDatabase(market);
        for (String symbol : database.listSymbols()) {
            scanSymbol(database, symbol, -1);

        }
    }

    public void scan() throws Exception {
        for(String market : Conf.listAllMarkets()) {
            scanMarket(market);
        }
    }
}
