package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.CompounderState;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.Trigger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;


/**
 * Compounder code
 *
 * Created by horse on 23/03/2016.
 */
public class Compounder {

    private double cashBalance;
    private final CompounderState state;

    public Compounder(double cash) {
        this.cashBalance = cash;
        state = new CompounderState(cash);
    }

    public double nextInvestmentAmount() {
        return state.minInvest + (state.compoundTally > 0 ? state.tallySlice : 0);
    }
    public boolean fundsAvailable() { return nextInvestmentAmount() <= cashBalance; }

    /**
     * Calculate compounded amount. Can be negative!
     * Also updates the various counters so that the amount available for compounding decreases.
     *
     * @return compounded value
     */
    private double calculateCompoundAmount() {
        double rv = 0.0;

        if(state.compoundTally > 0) {
            rv = state.tallySlice;
            state.tallySliceCnt++;
            state.compoundTally -= state.tallySlice;

            if(state.tallySliceCnt == state.spread) {
                state.compoundTally = 0.0;
                state.tallySliceCnt = 0;
                state.tallySlice = 0.0;
            }

            state.updateCompoundTally(state.compoundTally);
        } else {
            rv = 0;
        }

        double total = state.minInvest + rv;
        double breach = cashBalance - total;
        if(breach < 0) {
            // this will give us a negative compound amount, but should mean that we still get a trade in.
            rv -= breach;
        }
        return rv;
    }

    public Investment createInvestment(Trigger trigger) {
        Investment investment = new Investment(trigger);

        investment.cmpMin = state.minInvest;
        investment.cmpVal = calculateCompoundAmount();
        investment.cmpTotal = investment.cmpMin + investment.cmpVal;

        if(investment.cmpTotal < 0) {
            trigger.rejectReason = Trigger.RejectReason.NOFUNDS;
            trigger.rejectData = investment.cmpTotal;
            trigger.serialise();
            return null;
        }

        investment.buyLimit = round(trigger.price * Configuration.BUY_LIMIT_FACTOR);
        investment.buyDate = LocalDate.now();

        investment.qty = (int)Math.floor(investment.cmpTotal / investment.buyLimit);
        investment.qtyValue = investment.qty * investment.buyLimit;

        investment.sellLimit = round(trigger.price * Configuration.SELL_LIMIT_FACTOR);

        investment.sellDateLimit = investment.buyDate.plusDays(Configuration.SELL_LIMIT_DAYS);

        cashBalance -= investment.cmpTotal;

        return investment;
    }

    private double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void releaseInvestmentFunds(Investment investment) {
        cashBalance += investment.cmpTotal;
    }

    //public void onPartialFill
}
