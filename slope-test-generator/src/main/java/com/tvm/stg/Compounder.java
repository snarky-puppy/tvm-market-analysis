package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by matt on 10/02/17.
 */
public class Compounder {

    private static final Logger logger = LogManager.getLogger(Compounder.class);

    private ArrayList<Row> data;

    public double startBank = 200000.0;
    public double investPercent = 10.0;
    public int spread = 5;
    public int profitRollover = 20000;

    public double balanceCash;
    public double balanceTrades;
    public double balanceTotal;

    private int order = 0;
    public double maxDolVolPc = 1;

    public int size() {
        return data.size();
    }

    public Row get(int rowIndex) {
        return data.get(rowIndex);
    }


    public class Row {
        public String symbol;
        public Double transact;
        public Date date;

        public Double compTransact;
        public Double bankBalance;
        public Double roi;
        public Double compoundTally;

        public Double totalAssets;

        public int order;
        public double preCompoundInvestAmt;
        public double compoundInvestAmt;
        public double weight = 1.0;
        public double liquidity;
        public String note;

        public String getSymbol() {
            return symbol;
        }

        public Date getDate() {
            return date;
        }

        public void reset() {
            compTransact = null;
            bankBalance = null;
            roi = null;
            compoundTally = null;
            preCompoundInvestAmt = 0.0;
            compoundInvestAmt = 0.0;
            note = null;
        }

        public String toString() {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/YYYY");

            return String.format("%s\t%.2f\t%s", symbol, transact, f.format(date));
        }

        public boolean isInvestment() {
            return transact > 0.0;
        }
    }


    public Compounder() {
        data = new ArrayList<Row>();
        order = 0;
    }


    public void clear() {
        data.clear();
        order = 0;
    }


    private void log(String msg, int iteration) {
        logger.info(String.format("[p=%.2f,s=%d,i=%d]: %s", investPercent, spread, iteration, msg));
    }

    public void calculate(int iteration) {

        //List<Row> withdrawals =  data.stream().filter(r -> r.transact < 0).collect(Collectors.toList());

        balanceTotal = 0;
        balanceCash = startBank;
        balanceTrades = 0.0;

        if(spread == 0) {
            log("Parameters failed sanity check. Zero action means zero result.", iteration);
            return;
        }

        // total profit from last group of trades
        double compoundTally = 0.0;

        // tallyslice = compoundTally / spread ... after use, compoundTally -= tallySlice
        double tallySlice = 0.0;

        // number of times slice has been applied
        int sliceCount = 0;

        int iter = 0;

        double minInvestment = (startBank / 100) * investPercent;
        double trueProfit = 0.0;

        data.forEach(Row::reset);


        double maxVolPc = maxDolVolPc / 100;

        // now calculate what the actual investment would have been
        iter = 0;
        while(iter < data.size()) {
            Row r = data.get(iter);
            if(r == null) {
                iter++;
                continue;
            }

            // investment
            if(r.transact != null && r.transact > 0) {

                double investAmt = (minInvestment == 0 ? r.transact : minInvestment) * r.weight;

                if((balanceCash - investAmt) < 0) {
                    //logger.info(String.format("%s: I: not enough funds[%.2f] to cover investment[%.2f], skipping", r.symbol, totalBank, investAmt));
                    r.bankBalance = balanceCash;
                    r.totalAssets = balanceCash + balanceTrades;
                    iter++;
                    r.note = "SKIPPED - no cash";
                    continue;
                }

                r.preCompoundInvestAmt = investAmt;


                double oldCompoundTally = compoundTally;
                int oldSliceCount = sliceCount;
                double oldTallySlice = tallySlice;

                if(compoundTally > 0) {
                    // use compound tally.
                    //logger.info(String.format("%s: I: Compounding, add %.2f to investment amount of %.2f", r.symbol, tallySlice, investAmt));
                    r.compoundInvestAmt = tallySlice;
                    investAmt += tallySlice;

                    sliceCount ++;
                    compoundTally -= tallySlice;
                    if(sliceCount == spread) {
                        compoundTally = 0;
                        sliceCount = 0;
                        tallySlice = 0;
                    }
                }

                if((balanceCash - investAmt) < 0) {
                    investAmt = balanceCash;
                }

                double liqPc = investAmt / r.liquidity;
                if(liqPc > maxVolPc) {
                    // volume check failed
                    //r.compTransact = -1.0;
                    r.note = String.format("SKIPPED - volume %.2f%%", (liqPc*100));
                    r.bankBalance = balanceCash;

                    // reset any compounding
                    compoundTally = oldCompoundTally;
                    sliceCount = oldSliceCount;
                    tallySlice = oldTallySlice;

                } else {
                    //logger.info(String.format("%s: I: final_amount=%.2f balanceTrades=%.2f", r.symbol, investAmt, balanceTrades + investAmt));

                    balanceTrades += investAmt;
                    balanceCash -= investAmt;
                    r.bankBalance = balanceCash;
                    r.compTransact = investAmt;
                }
            }

            // withdrawal
            if(r.transact != null && r.transact < 0) {

                // calculate actual ROI
                Row i = findIfromW(r.symbol, iter - 1);
                if(!(i != null && i.compTransact != null)) {
                    // no I or I didn't go through due to lack of funds
                    iter++;
                    r.bankBalance = balanceCash;
                    r.totalAssets = balanceCash + balanceTrades;
                    continue;
                }

                double roi = -(i.transact + r.transact) / i.transact;
                double profit = i.compTransact * roi;
                double withdrawal = i.compTransact + profit;

                if(roi > 0) {
                    // if minInvest is 0 just use simulated transaction
                    //compoundTally += minInvestment + profit;
                    compoundTally += ((minInvestment == 0 ? i.transact : minInvestment) * i.weight) + profit;

                    sliceCount = 0;
                    tallySlice = compoundTally / spread;
                }


                //log(String.format("%s: W: roi=%.2f, profit=%.2f, withdrawal=%.2f, compoundTally=%.2f", r.symbol, roiAmt, profit, withdrawal, compoundTally), iteration);

                r.roi = roi;
                r.compTransact = -withdrawal;
                balanceCash += withdrawal;
                r.compoundTally = compoundTally;
                balanceTrades -= i.compTransact;
                r.bankBalance = balanceCash;

                if(withdrawal - i.compTransact > 0) {
                    // a profit was made
                    startBank += withdrawal - i.compTransact;
                    trueProfit += withdrawal - i.compTransact;
                    if(trueProfit >= profitRollover) {
                        // recalc minInvest

                        //balanceCash += ((trueProfit / profitRollover) * profitRollover);
                        if(profitRollover == 0)
                            trueProfit = 0;
                        else
                            trueProfit %= profitRollover;
                        minInvestment = (startBank / 100) * investPercent;

                        logger.info(String.format("new period: trueProfit=%.2f, minInvest=%.2f", trueProfit, minInvestment));
                    }
                }
            }

            r.totalAssets = balanceCash + balanceTrades;
            iter ++;
        }

        balanceTotal = balanceCash + balanceTrades;
        logger.info(String.format("End of compound run: cash=%.2f trade=%.2f total=%.2f", balanceCash, balanceTrades, balanceTotal));
    }

    private Row findIfromW(String symbol, int iter) {
        while(iter >= 0) {
            Row r = data.get(iter);
            if(r != null && r.transact != null && r.symbol != null && r.symbol.compareTo(symbol) == 0) {
                return r;
            }
            iter--;
        }
        return null;
    }

    public void shuffle() {

        Map<Date, List<Row>> daily = data.stream().collect(Collectors.groupingBy(Row::getDate));

        /*
        logger.debug("Pre-shuffle:");
        for(Row r : data) {
            logger.debug(r);
        }*/


        daily.forEach((k, v) -> {
            Map<Boolean, List<Row>> investmentsAndWithdrawals = v.stream().collect(Collectors.groupingBy(Row::isInvestment));

            v.clear();
            List<Row> invest = investmentsAndWithdrawals.get(true);
            if(invest != null) {
                Collections.shuffle(invest);
                v.addAll(invest);
            }

            List<Row> withd = investmentsAndWithdrawals.get(false);
            if(withd != null) {
                Collections.shuffle(withd);
                v.addAll(withd);
            }
        });

        // re-insert into period list

        data.clear();
        SortedSet<Date> keys = new TreeSet<Date>(daily.keySet());
        for (Date key : keys) {
            List<Row> value = daily.get(key);
            // do something
            data.addAll(value);
        }
    }
}
