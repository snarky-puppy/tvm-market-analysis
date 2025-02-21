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
 * Report on what would happen during an adjustment.
 *
 * Created by horse on 24/02/15.
 */
public class SplitScreener {



    // use tvm adjusted data
    private boolean useAdjusted = false;

    class Result {
        public String exchange;
        public String symbol;
        public String type;
        public String preDate;
        public String prePrice;
        public String postDate;
        public String postPrice;
        public String ratio;
        public String tvmPrice;

        @Override
        public String toString() {
            return  exchange + "," +
                    symbol + "," +
                    type + "," +
                    preDate + "," +
                    prePrice + "," +
                    postDate + "," +
                    postPrice + "," +
                    ratio + "," +
                    tvmPrice
                    ;
        }

        Result(String ex, String sy, String ty, String preDate, String prePrice, String postDate, String postPrice, String ratio, String tvmPrice) {
            this.exchange = ex;
            this.symbol = sy;
            this.type = ty;
            this.preDate = preDate;
            this.prePrice = prePrice;
            this.postDate = postDate;
            this.postPrice = postPrice;
            this.ratio = ratio;
            this.tvmPrice = tvmPrice;
        }
    }



    private static final Logger logger = LogManager.getLogger(SplitScreener.class);

    public static void main(String[] args) throws Exception {
        SplitScreener splitScreener = new SplitScreener();
        splitScreener.scan();
    }


    public SplitScreener() {}

    public SplitScreener(boolean useAdjusted) {
        this.useAdjusted = useAdjusted;
    }

    private void output(String market, List<Result> results) throws Exception {
        String fname = Util.getOutFile(market, "SplitScreener");
        BufferedWriter bw = new BufferedWriter(new FileWriter(fname));
        bw.write("Exchange,Symbol,Type,Pre Date,Pre Price,Post Date,Post Price,Ratio,TVM Canary\n");
        for(Result r : results) {
            bw.write(r.toString()+"\n");
        }
        bw.flush();
        bw.close();
    }

    public double getClose(CloseData data, int idx) {
        if(useAdjusted)
            return data.adjustedClose[idx];
        else
            return data.close[idx];
    }

    public void scanMarket(String market) throws Exception {
        NumberFormat formatter = new DecimalFormat("#0.000");
        Database database = DatabaseFactory.createDatabase(market);
        List<Result> results = new ArrayList<>();
        for(String symbol : database.listSymbols()) {
            logger.info(String.format("%s/%s", market, symbol));
            CloseData data = database.loadData(symbol);
            double maxPrice = getClose(data, 0);
            boolean maxBreach;
            double lastPrice = getClose(data, 0);
            int lastDate = data.date[0];

            /*
            // premium data close, first and last in the series
            double pdStartPrice, pdEndPrice;
            pdStartPrice = getClose(data, 0);
            pdEndPrice = getClose(data, data.close.length-1);
            double pdRatio = pdStartPrice / pdEndPrice;
            results.add(new Result(
                    market,
                    symbol,
                    "PDSCAN",
                    Integer.toString(data.date[0]),
                    formatter.format(pdStartPrice),
                    Integer.toString(data.date[data.date.length-1]),
                    formatter.format(pdEndPrice),
                    formatter.format(pdRatio),
                    ""
            ));
*/
            List<Result> tempResult = new ArrayList<>();

            // start the scan, for real yo
            for(int i = 1; i < data.close.length; i++) {
                double thisPrice = data.close[i];
                int thisDate = data.date[i];

                maxPrice = Math.max(maxPrice, thisPrice);
                maxBreach = maxPrice >= Conf.MAX_ADJUSTMENTS;

                if(maxBreach) {

                    // check gaps
                    if(DateUtil.distance(lastDate, thisDate) >= Conf.DATA_GAP_DAYS) {
                        results.add(new Result(
                                market,
                                symbol,
                                "GAP",
                                String.format("%d", lastDate), "",
                                String.format("%d", thisDate), "",
                                "", ""));
                        break;
                    }

                    double ratio = thisPrice / lastPrice;
                    if ((ratio <= Conf.MIN_ADJ_RATIO) || (ratio >= Conf.MAX_ADJ_RATIO)) {
                        data.adjustClose(i - 1, ratio);
                        tempResult.add(new Result(
                                market,
                                symbol,
                                "SPLIT",
                                Integer.toString(lastDate),
                                formatter.format(lastPrice),
                                Integer.toString(thisDate),
                                formatter.format(thisPrice),
                                formatter.format(ratio),
                                formatter.format(data.adjustedClose[0])));
                    }
                }

                lastPrice = thisPrice;
                lastDate = thisDate;
            }



            if(tempResult.size() > Conf.MAX_ADJUSTMENTS) {
                results.add(new Result(
                        market,
                        symbol,
                        "TOOMANY",
                        String.format("%d", tempResult.size()), "",
                        "", "",
                        "", ""));
            } else {
                results.addAll(tempResult);
                database.rewrite(data);
            }

            logger.info("results=" + tempResult.size());
        }
        if(results.size() > 0)
            output(market, results);
    }

    public void scan() throws Exception {
        for(String market : Conf.listAllMarkets()) {
            if(!market.startsWith("test"))
                scanMarket(market);
        }
    }
}
