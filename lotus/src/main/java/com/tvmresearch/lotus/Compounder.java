package com.tvmresearch.lotus;

import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.db.model.CompounderState;
import com.tvmresearch.lotus.db.model.Position;
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

    private final Broker broker;
    private final CompounderState state;

    public Compounder(Broker broker) {
        this.broker = broker;
        state = new CompounderState(broker);
    }

    public double nextInvestmentAmount() {
        return state.minInvest + (state.compoundTally > 0 ? state.tallySlice : 0);
    }

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
        double breach = broker.getAvailableFunds() - total;
        if(breach < 0) {
            // this will give us a negatice compound amount, but should mean that we still get a trade in.
            rv -= breach;
        }
        return rv;
    }

    public Position createPosition(Trigger trigger) {
        Position position = new Position(trigger);

        position.cmpMin = state.minInvest;
        position.cmpVal = calculateCompoundAmount();
        position.cmpTotal = position.cmpMin + position.cmpVal;

        if(position.cmpTotal < 0) {
            trigger.rejectReason = Trigger.RejectReason.NOFUNDS;
            trigger.rejectData = position.cmpTotal;
            trigger.serialise();
            return null;
        }

        position.buyLimit = round(trigger.price * Configuration.BUY_LIMIT_FACTOR);
        position.buyDate = LocalDate.now();

        position.qty = (int)Math.floor(position.cmpTotal / position.buyLimit);
        position.qtyValue = position.qty * position.buyLimit;

        position.sellLimit = round(trigger.price * Configuration.SELL_LIMIT_FACTOR);

        position.sellDateLimit = position.buyDate.plusDays(Configuration.SELL_LIMIT_DAYS);

        return position;
    }

    private double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    //public void onPartialFill
}
