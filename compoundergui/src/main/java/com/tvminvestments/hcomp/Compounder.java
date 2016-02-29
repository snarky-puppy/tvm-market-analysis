package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by horse on 11/02/2016.
 */
public class Compounder {

    private static final Logger logger = LogManager.getLogger(Compounder.class);
    private ArrayList<Row> data;

    public double totalBank = 200000.0;
    public int investPercent = 10;
    public int spread = 5;

    public double balanceCash;
    public double balanceTrades;
    public double balanceTotal;

    private int order = 0;

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

        public Double realTransact;
        public Double bankBalance;
        public Double roi;
        public Double compoundTally;
        public Integer period;

        public int order;

        public String getSymbol() {
            return symbol;
        }

        public Date getDate() {
            return date;
        }

        public void reset() {
            realTransact = null;
            bankBalance = null;
            roi = null;
            compoundTally = null;
        }

        public String toString() {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/YYYY");

            return String.format("%s\t%.2f\t%s\t%d", symbol, transact, f.format(date), period);
        }

        public boolean isInvestment() {
            return transact > 0.0;
        }

        public Integer getPeriod() {
            return period;
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

    public void loadFile(File file) throws IOException, ParseException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        clear();

        int lineNumber = 1;
        while((line = br.readLine()) != null) {
            String[] data = line.split(",");
            addLine(data, lineNumber);
            lineNumber ++;
        }
    }

    private void addLine(String[] line, int lineNumber) throws ParseException {
        if(line == null || line.length < 4) {
            logger.warn(String.format("Line %d: Ignoring source line, not enough elements (should be 4)", lineNumber));
            return; // ignore row
        }

        /* NB: be more forgiving
        for(int i = 0; i < 3; i++)
            if(line[i] == null || line[i].length() == 0) {
                logger.debug(String.format("Line %d: Empty field %d, ignoring", lineNumber, i));
                return; // ignore silently
            }
            */

        Row r = new Row();
        if(line[0] != null && line[0].length() > 0)
            r.symbol = line[0];

        if(line[1] != null && line[1].length() > 0) {
            try {
                r.transact = Double.parseDouble(line[1]);
            } catch (NumberFormatException e) {
                logger.warn(String.format("Line %d: Could not parse double (%s): ", lineNumber, line[1]), e);
            }
        }

        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        if(line[2] != null && line[2].length() > 0) {
            try {
                r.date = sdf.parse(line[2]);
            } catch(ParseException e) {
                logger.warn(String.format("Line %d: Could not parse date (%s): ", lineNumber, line[1]), e);
            }
        }

        if(line[3] != null && line[3].length() > 0) {
            try {
                r.period = Integer.parseInt(line[3]);
            } catch(NumberFormatException e) {
                logger.warn(String.format("Line %d: Could not parse rollover period (%s): ", lineNumber, line[1]), e);
            }
        }

        r.order = order++;
        data.add(r);
    }

    private CompounderLogRow newLogRow() {
        CompounderLogRow r = new CompounderLogRow();
        r.percent = investPercent;
        r.spread = spread;
        return r;
    }

    public void calculate(int iteration) {

        //List<Row> withdrawals =  data.stream().filter(r -> r.transact < 0).collect(Collectors.toList());



        CompounderLogRow logRow = newLogRow();

        balanceTotal = 0;
        balanceCash = totalBank;
        balanceTrades = 0.0;

        if(spread == 0 || investPercent == 0) {
            logger.info("Parameters failed sanity check. Zero action means zero result.");
            return;
        }

        // total profit from last group of trades
        double compoundTally = 0.0;

        // tallyslice = compoundTally / spread ... after use, compoundTally -= tallySlice
        double tallySlice = 0.0;

        // number of times slice has been applied
        int sliceSpreadCount = 0;

        boolean lastWithdrawal = false;

        int iter = 0;

        int period = -1;
        double minInvestment = 0.0;

        data.stream().forEach(Row::reset);

        // calculate ROI for each withdrawal
        // Formula is: -(i + w)
        while(iter < data.size()) {
            Row w = data.get(iter);
            if(w != null && w.transact != null && w.transact < 0) {

                // have w, find i
                Row i = findIfromW(w.symbol, iter - 1);

                if(i != null) {
                    //logger.debug(String.format("ROI: %s: matching I: %f", w.symbol, i.transact));
                    double roiAmount = -(i.transact + w.transact);
                    w.roi = roiAmount / i.transact;

                } //else logger.debug(String.format("ROI: %s: no I found", w.symbol));
            } // else logger.debug(String.format("ROI: empty or I row"));
            iter++;
        }

        // now calculate what the actual investment would have been
        iter = 0;
        while(iter < data.size()) {
            Row r = data.get(iter);
            if(r == null) {
                iter++;
                continue;
            }

            if(period == -1 || period != r.period) {
                if(period != -1) {
                    logRow.cash = balanceCash;
                    logRow.trades = balanceTrades;
                    logRow.total = balanceCash + balanceTrades;
                    CompounderLogResults.add(logRow);
                    logRow = newLogRow();
                }
                // recalculate minInvestment
                logger.info(String.format("---------------- New period detected: %d, balances: cash=%.2f, trade=%.2f, total=%.2f", r.period, balanceCash, balanceTrades, balanceCash+balanceTrades));
                totalBank = balanceCash + balanceTrades;
                balanceCash = totalBank;
                balanceTrades = 0.0;
                minInvestment = ((totalBank/100)*investPercent);
                logger.info(String.format("New period minInvestment=%.2f, totalBank=%.2f", minInvestment, totalBank));
                period = r.period;

                logRow.iteration = iteration;
                logRow.period = period;
                logRow.minInvest = minInvestment;
                logRow.startBank = totalBank;
            }


            // investment
            if(r.transact != null && r.transact > 0) {

                double investAmt = minInvestment;

                // last row was a withdrawal, so update spread counters
                if(lastWithdrawal) {
                    sliceSpreadCount = 0;
                    //logger.debug(String.format("pre-tally slice: %.2f (spread=%d)", tallySlice, spread));
                    tallySlice = compoundTally / spread;
                    //logger.debug(String.format("post-tally slice: %.2f (spread=%d)", tallySlice, spread));
                    lastWithdrawal = false;
                }

                if((totalBank - investAmt) < 0) {
                    //logger.info(String.format("%s: I: not enough funds[%.2f] to cover investment[%.2f], skipping", r.symbol, totalBank, investAmt));
                    r.bankBalance = totalBank;
                    iter++;
                    continue;
                }

                if(compoundTally > 0) {
                    // use compound tally.
                    //logger.info(String.format("%s: I: Compounding, add %.2f to investment amount of %.2f", r.symbol, tallySlice, investAmt));
                    investAmt += tallySlice;
                    sliceSpreadCount ++;
                    compoundTally -= tallySlice;
                    if(sliceSpreadCount == spread || (totalBank - tallySlice + investAmt) < 0) {
                        assert compoundTally == 0;
                        sliceSpreadCount = 0;
                        tallySlice = 0;
                    }
                }

                //logger.info(String.format("%s: I: final_amount=%.2f balanceTrades=%.2f", r.symbol, investAmt, balanceTrades + investAmt));

                totalBank -= investAmt;
                balanceTrades += investAmt;
                balanceCash -= investAmt;
                r.bankBalance = totalBank;
                if(investAmt > 0)
                    r.realTransact = investAmt;
            }

            // withdrawal
            if(r.transact != null && r.transact < 0) {

                // calculate actual ROI
                Row i = findIfromW(r.symbol, iter - 1);
                if(i != null && i.realTransact != null)  {
                    double profit = i.realTransact * r.roi;
                    double withdrawal = i.realTransact + profit;

                    double roiAmt = profit;

                    // redfinnition of the term "profit", since it goes into a counter for distribution
                    if(withdrawal > minInvestment)
                        profit = withdrawal - minInvestment;

                    if(r.roi > 0) {
                        compoundTally += profit;
                    }

                    //logger.info(String.format("%s: W: roi=%.2f, profit=%.2f, compoundTally=%.2f", r.symbol, roiAmt, profit, compoundTally));

                    r.realTransact = -withdrawal;

                    totalBank += withdrawal;
                    r.bankBalance = totalBank;
                    r.compoundTally = compoundTally;

                    balanceTrades -= i.realTransact;
                    balanceCash += withdrawal;
                    lastWithdrawal = true;
                } //else logger.warn(String.format("%s: W: no valid I found", r.symbol));


            }
            iter ++;
        }

        logRow.cash = balanceCash;
        logRow.trades = balanceTrades;
        logRow.total = balanceCash + balanceTrades;
        CompounderLogResults.add(logRow);

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

        Map<Integer, List<Row>> periods = data.stream().collect(Collectors.groupingBy(Row::getPeriod));

        periods.forEach((periodK, periodList) -> {

            Map<Date, List<Row>> daily = periodList.stream().collect(Collectors.groupingBy(Row::getDate));

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

            periodList.clear();
            SortedSet<Date> keys = new TreeSet<Date>(daily.keySet());
            for (Date key : keys) {
                List<Row> value = daily.get(key);
                // do something
                periodList.addAll(value);
            }

            /*
            daily.forEach((k, v) -> Collections.shuffle(v));

            // now somehow make sure that any W with a matching I in this list are in the correct order
            daily.forEach((key, list) -> {
                int i, j;
                for (i = 0; i < list.size(); i++) {
                    Row r = list.get(i);

                    for (j = i + 1; j < list.size(); j++) {
                        Row r2 = list.get(j);
                        if (r2.symbol.compareTo(r.symbol) == 0) {
                            if (r.order > r2.order) {
                                Collections.swap(list, i, j);
                            }
                        }
                    }
                }
            });
            */
        });

        // re-insert into main array
        data.clear();

        SortedSet<Integer> keys = new TreeSet<Integer>(periods.keySet());
        for (Integer key : keys) {
            List<Row> value = periods.get(key);
            // do something
            data.addAll(value);
        }

/*

        logger.debug("Post-shuffle:");
        for(Row r : data) {
            logger.debug(r);
        }

*/

    }
}
