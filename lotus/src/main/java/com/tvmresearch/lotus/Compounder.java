package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.CompounderState;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.Trigger;
import com.tvmresearch.lotus.db.model.TriggerDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;


/**
 * Compounder code
 *
 * Created by horse on 23/03/2016.
 */
public class Compounder {

    private double cashAUD;
    private double cashUSD;
    private final CompounderState state;
    private double fxRate;

    public Compounder(double cash, double fxRate) {
        this.cashAUD = cash;
        setFxRate(fxRate);
        state = new CompounderState(cash);
    }

    public double nextInvestmentAmount() {
        return state.minInvest + (state.compoundTally > 0 ? state.tallySlice : 0);
    }
    public boolean fundsAvailable() { return nextInvestmentAmount() <= cashUSD; }


    public boolean checkout(Investment investment) {

        state.load();

        investment.cmpMin = state.minInvest;
        if(state.compoundTally > 0) {
            investment.cmpVal = state.tallySlice;
            state.tallySliceCnt++;
            state.compoundTally -= state.tallySlice;

            if(state.tallySliceCnt == state.spread) {
                state.compoundTally = 0.0;
                state.tallySliceCnt = 0;
                state.tallySlice = 0.0;
            }

        }
        investment.cmpVal = calculateCompoundAmount();
        investment.cmpTotal = investment.cmpMin + investment.cmpVal;

        if(investment.cmpTotal < 0) {
            investment.trigger.rejectReason = Trigger.RejectReason.NOFUNDS;
            investment.trigger.rejectData = investment.cmpTotal;
            return false;
        }

        investment.buyLimit = round(investment.trigger.price * Configuration.BUY_LIMIT_FACTOR);
        investment.buyDate = LocalDate.now();

        investment.qty = (int)Math.floor(investment.cmpTotal / investment.buyLimit);
        investment.qtyValue = investment.qty * investment.buyLimit;

        investment.sellLimit = round(investment.trigger.price * Configuration.SELL_LIMIT_FACTOR);

        investment.sellDateLimit = investment.buyDate.plusDays(Configuration.SELL_LIMIT_DAYS);

        cashUSD -= investment.cmpTotal;

        return true;
    }

    public void cancelCheckout(Investment investment) {
        state.load();

        state.compoundTally += investment.cmpVal;
        cashUSD += investment.cmpMin;
    }

    private double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void setFxRate(double fxRate) {
        this.fxRate = fxRate;
        cashUSD = cashAUD / fxRate;
    }

    public double getFxRate() {
        return fxRate;
    }

    public void addProfit(double profit) {
        state.addProfit(profit);
    }

    //public void onPartialFill
}
