package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.*;
import com.tvmresearch.lotus.broker.Broker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Compounder state
 *
 * Created by horse on 23/03/2016.
 */
public class CompounderState {
    private static final Logger logger = LogManager.getLogger(CompounderState.class);

    public double startBank;
    public double minInvest;
    public double compoundTally;
    public double tallySlice;
    public int tallySliceCnt;
    public int spread;
    public int investPercent;

    public CompounderState(double cashBalance) {
        if(!load()) {
            initState(cashBalance);
        }
        // Tally slice is recalculated daily, since I(nvestments) always before W(ithdrawals)
        tallySlice = compoundTally / spread;
        tallySliceCnt = 0;

        logger.info(String.format("start_bank=%.2f min_invest=%.2f compound_tally=%.2f tally_slice=%.2f",
                startBank, minInvest, compoundTally, tallySlice));
    }

    /**
     * Previous months compound tally value
     * @return Previous months compound tally value
     */
    private double previousCompoundTally() {
        double rv = 0.0;
        final String sql = "SELECT compound_tally FROM compounder_state WHERE dt = ?";
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);
            stmt.setDate(1, java.sql.Date.valueOf(DateUtil.firstOfLastMonth()));
            rs = stmt.executeQuery();
            if(rs.next()) {
                rv = rs.getDouble(1);
            }
            return rv;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    /**
     * Update compound tally value after processing Withdrawals.
     *
     * @param compoundTally
     */
    public void updateCompoundTally(double compoundTally) {
        final String sql = "UPDATE compounder_state SET compound_tally = ? WHERE dt = ?";
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);
            stmt.setDouble(1, compoundTally);
            stmt.setDate(2, java.sql.Date.valueOf(DateUtil.firstOfThisMonth()));
            stmt.execute();
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt, connection);
        }
    }

    private void initState(double todaysFunds) {
        startBank = todaysFunds;
        investPercent = Configuration.MIN_INVEST_PC;
        spread = Configuration.SPREAD;
        minInvest = ((startBank/100)*investPercent);
        compoundTally = previousCompoundTally();

        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            final String sql = "INSERT INTO compounder_state(dt, start_bank, min_invest, compound_tally, spread, invest_pc) " +
                    "VALUES(?, ?, ?, ?, ?, ?)";
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);

            stmt.setDate(1, java.sql.Date.valueOf(DateUtil.firstOfThisMonth()));
            stmt.setDouble(2, startBank);
            stmt.setDouble(3, minInvest);
            stmt.setDouble(4, compoundTally);
            stmt.setInt(5, spread);
            stmt.setInt(6, investPercent);

            stmt.execute();

            System.out.println("INIT STATE");

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

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT start_bank, min_invest, compound_tally, spread, invest_pc FROM compounder_state WHERE dt = ?");
            stmt.setDate(1, java.sql.Date.valueOf(DateUtil.firstOfThisMonth()));
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
