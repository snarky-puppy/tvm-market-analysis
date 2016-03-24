package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.CompounderState;
import com.tvmresearch.lotus.db.model.Trigger;


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

    public Position createBuyOrder(Trigger trigger) {
        Position position = new Position(trigger);
        position.minInvest = state.minInvest;
        position.limit = trigger.price * Configuration.BUY_LIMIT_FACTOR;

        if(state.compoundTally > 0) {
            position.compoundAmount = state.tallySlice;
            state.tallySliceCnt++;
            state.compoundTally -= state.tallySlice;

            if(state.tallySliceCnt == state.spread) {
                state.compoundTally = 0.0;
                state.tallySliceCnt = 0;
                state.tallySlice = 0.0;
            }

            state.updateCompoundTally(state.compoundTally);
        } else {
            position.compoundAmount = 0;
        }

        position.totalInvest = position.minInvest + position.compoundAmount;

        double breach = broker.getAvailableFunds() - position.totalInvest;
        if(breach < 0) {
            position.minInvest -= breach;
            position.totalInvest -= breach;
        }

        // final sanity check
        if(position.totalInvest < 0) {
            trigger.rejectReason = Trigger.RejectReason.NOFUNDS;
            trigger.rejectData = position.totalInvest;
            trigger.serialise();
            return null;
        } else {
            position.qty = (int)Math.floor(position.totalInvest / position.limit);
            return position;
        }
    }

    //public void onPartialFill
}
