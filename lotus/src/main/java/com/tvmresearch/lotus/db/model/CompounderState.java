package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Compounder state
 *
 * Created by horse on 23/03/2016.
 */
public class CompounderState {
    private final Broker broker;
    private CompounderState instance;

    public double startBank;
    public double minInvest;
    public double compoundTally;
    public int spread;
    public int investPercent;

    public CompounderState(Broker broker) {
        this.broker = broker;
        if(!load() || DateUtil.isFirstOfMonth()) {
            initState();
        }
    }

    private void initState() {
        startBank = broker.getAvailableFunds();
        investPercent = Configuration.MIN_INVEST_PC;
        spread = Configuration.SPREAD;
        minInvest = ((startBank/100)*investPercent);
        compoundTally = 0.0;

        Connection connection = null;
        PreparedStatement stmt = null;
        Date dt = DateUtil.firstOfThisMonth();
        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("INSERT INTO compounder_state VALUES(NULL, "+Database.generateParams(6)+")");

            stmt.setDate(1, new java.sql.Date(dt.getTime()));
            stmt.setDouble(2, startBank);
            stmt.setDouble(3, minInvest);
            stmt.setDouble(4, compoundTally);
            stmt.setInt(5, spread);
            stmt.setInt(6, investPercent);

            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        } finally {
            Database.close(stmt, connection);
        }
    }

    private boolean load() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Date dt = DateUtil.firstOfThisMonth();
        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT start_bank, min_invest, compound_tally, spread, invest_pc FROM compounder_state WHERE dt = ?");
            stmt.setDate(1, new java.sql.Date(dt.getTime()));
            rs = stmt.executeQuery();
            if(rs.next()) {
                startBank = rs.getDouble(1);
                minInvest = rs.getDouble(2);
                compoundTally = rs.getDouble(3);
                spread = rs.getInt(4);
                investPercent = rs.getInt(5);
                return true;
            } else
                return false;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }
}
