package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.CompoundState;
import com.tvmresearch.lotus.db.model.Investment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Compounder code
 *
 * Created by horse on 23/03/2016.
 */
public class Compounder {

    private static final Logger logger = LogManager.getLogger(Compounder.class);

    private final CompoundState state;
    private double fxRate;

    public Compounder(double cash, double fxRate) {
        this.fxRate = fxRate;

        // initialise things if they don't exist
        state = new CompoundState(cash / fxRate);
    }

    public double nextInvestmentAmount() {
        return state.minInvest + (state.compoundTally > 0 ? state.tallySlice : 0);
    }
    public boolean fundsAvailable() { return nextInvestmentAmount() <= state.cash; }

    public boolean apply(Investment investment) {

        logger.info(String.format("apply: min=%.2f cmp=%.2f slice=%.2f cnt=%d cash=%.2f",
                state.minInvest, state.compoundTally, state.tallySlice, state.tallySliceCnt, state.cash));

        investment.cmpMin = state.minInvest;
        if(state.cash < state.minInvest) {
            String msg = String.format("apply: not enough cash for minInvest: cash=%.2f minInvest=%.2f",
                    state.cash, state.minInvest);
            logger.error(msg);
            investment.errorCode = 1;
            investment.errorMsg = msg;
            return false;
        }

        if(state.compoundTally > 0) {
            if(state.cash < (investment.cmpMin + state.tallySlice)) {
                logger.error(String.format("apply: investment tally but not enough funds to cover it: " +
                                "cash=%.2f investAmt=%.2f (tallySlice=%.2f + cmpMin=%.2f)",
                        state.cash, (investment.cmpMin + state.tallySlice), state.tallySlice, investment.cmpMin));
                state.resetTally();
            } else {
                investment.cmpVal = state.tallySlice;
                state.tallySliceCnt++;
                state.compoundTally -= state.tallySlice;

                if (state.tallySliceCnt == state.spread) {
                    state.resetTally();
                }
            }
        } else
            investment.cmpVal = 0.0;
        investment.cmpTotal = investment.cmpMin + investment.cmpVal;

        state.cash -= investment.cmpTotal;

        if(state.cash < 0) {
            logger.error("apply: REAL BAD: cash < 0! resetting to 0!");
            state.cash = 0;
            state.save();
            return false;
        } else {
            state.save();
            return true;
        }
    }

    public void cancel(Investment investment) {
        state.compoundTally += investment.cmpVal;
        state.cash += investment.cmpMin;

        // reverse tally
        if(investment.cmpVal > 0) {
            if(state.tallySliceCnt == 0) {
                state.tallySliceCnt = state.spread - 1;
                state.tallySlice = investment.cmpVal;
            } else {
                state.tallySliceCnt --;
                // assume tallySlice still has a value
            }
        }
        state.save();
    }


    public void processWithdrawal(Investment investment) {
        double withdrawal = investment.sellPrice;
        double profit = withdrawal - investment.qtyFilledValue;

        state.compoundTally += profit;
        state.cash += withdrawal;
        state.tallySliceCnt = 0;
        state.tallySlice = state.compoundTally / state.spread;

        state.save();
    }

    public void updateCashAndRate(double cash, double fx) {
        this.fxRate = fx;
        state.cash = cash / fx;
        state.save();
    }
}
