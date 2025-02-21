package com.tvminvestments.zscore.app.reports;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.app.Conf;
import com.tvminvestments.zscore.app.Util;
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
 * Created by horse on 17/07/15.
 */
public class DataGaps {
    private static final Logger logger = LogManager.getLogger(DataGaps.class);


    class Result {
        private final int distance;
        public String exchange;
        public String symbol;
        public String preDate;
        public String prePrice;
        public String postDate;
        public String postPrice;
        public String ratio;

        @Override
        public String toString() {
            return  exchange + "," +
                    symbol + "," +
                    preDate + "," +
                    prePrice + "," +
                    postDate + "," +
                    postPrice + "," +
                    ratio + ","+Integer.toString(distance);
        }

        Result(String ex, String sy, String preDate, String prePrice, String postDate, String postPrice, String ratio, int distance) {
            this.exchange = ex;
            this.symbol = sy;
            this.preDate = preDate;
            this.prePrice = prePrice;
            this.postDate = postDate;
            this.postPrice = postPrice;
            this.ratio = ratio;
            this.distance = distance;
        }
    }


    private void output(String market, List<Result> results) throws Exception {
        String fname = Util.getOutFile(market, "DataGaps");

        BufferedWriter bw = new BufferedWriter(new FileWriter(fname));
        bw.write("Exchange,Symbol,Pre Date,Pre Price,Post Date,Post Price,Ratio,Gap Days\n");
        for(Result r : results) {
            bw.write(r.toString()+"\n");
        }
        bw.flush();
        bw.close();
    }

    public void scanMarket(String market) throws Exception {
        NumberFormat formatter = new DecimalFormat("#0.000");
        Database database = DatabaseFactory.createDatabase(market);
        List<Result> results = new ArrayList<>();
        for (String symbol : database.listSymbols()) {
            logger.info(String.format("%s/%s", market, symbol));
            int nresults = 0;
            CloseData data = database.loadData(symbol);
            double lastPrice = data.close[0];
            int lastDate = data.date[0];

            for (int i = 1; i < data.close.length; i++) {
                double thisPrice = data.close[i];
                int thisDate = data.date[i];

                double ratio = thisPrice / lastPrice;

                int distance = DateUtil.distance(lastDate, thisDate);

                if (distance >= Conf.DATA_GAP_DAYS) {

                    results.add(new Result(market, symbol,
                            Integer.toString(lastDate),
                            formatter.format(lastPrice),
                            Integer.toString(thisDate),
                            formatter.format(thisPrice),
                            formatter.format(ratio),
                            distance
                    ));
                    nresults++;
                }

                lastPrice = thisPrice;
                lastDate = thisDate;

            }
            logger.info("results=" + nresults);
        }
        if (results.size() > 0)
            output(market, results);

    }

    public void scan() throws Exception {
        for(String market : Conf.listAllMarkets()) {
            scanMarket(market);
        }
    }
}
