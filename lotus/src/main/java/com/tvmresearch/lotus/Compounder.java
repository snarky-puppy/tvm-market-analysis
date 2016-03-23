package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.CompounderState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

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

    public void onInvestment(Investment investment) {

    }

    public void onWithdrawal(Withdrawal withdrawal) {

    }

    public double nextInvestmentAmount() {
        return state.minInvest;
    }
}
