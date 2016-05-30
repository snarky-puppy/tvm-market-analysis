package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.*;
import com.tvmresearch.lotus.broker.Broker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Compounder state
 *
 * Created by horse on 23/03/2016.
 */
public class CompounderState {
    private static final Logger logger = LogManager.getLogger(CompounderState.class);

    public double startBank;
    public double minInvest;
    public double cash;
    public double compoundTally;
    public double tallySlice;
    public int tallySliceCnt;
    public int spread;
    public int investPercent;

    public CompounderState(double brokerCash) {
        this.startBank = brokerCash;
        this.cash = brokerCash;
        if(!load()) {
            initState();
        }

        logger.info(String.format("start_bank=%.2f cash=%.2f min_invest=%.2f compound_tally=%.2f tally_slice=%.2f, tally_slice_cnt=%d",
                startBank, cash, minInvest, compoundTally, tallySlice, tallySliceCnt));
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
            stmt.setDate(1, java.sql.Date.valueOf(firstOfLastMonth()));
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

    public void save() {
        if(isStateSaved())
            update();
        else {
            startBank = cash;
            initState();
        }

    }

    private void update() {
        final String sql = "UPDATE compounder_state " +
                "SET cash = ?, " +
                "    compound_tally = compound_tally + ?, " +
                "    tally_slice = 0, " +
                "    tally_slice_cnt = 0 " +
                "WHERE dt = ?";
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);
            int idx = 1;
            stmt.setDouble(idx++, cash);
            stmt.setDouble(idx++, compoundTally);
            stmt.setDouble(idx++, tallySlice);
            stmt.setInt(idx++, tallySliceCnt);
            stmt.setDate(idx, java.sql.Date.valueOf(firstOfThisMonth()));
            stmt.execute();

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt, connection);
        }
    }

    private void initState() {
        investPercent = Configuration.MIN_INVEST_PC;
        spread = Configuration.SPREAD;
        minInvest = ((startBank/100)*investPercent);
        compoundTally = previousCompoundTally();
        tallySlice = compoundTally / spread;
        tallySliceCnt = 0;

        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            final String sql = Database.generateInsertSQL("compounder_state", new String[] {
                            "dt",
                            "start_bank",
                            "min_invest",
                            "cash",
                            "compound_tally",
                            "tally_slice",
                            "tally_slice_cnt",
                            "spread",
                            "invest_pc"});


            connection = Database.connection();
            stmt = connection.prepareStatement(sql);

            int idx = 1;
            stmt.setDate(idx++, java.sql.Date.valueOf(firstOfThisMonth()));
            stmt.setDouble(idx++, startBank);
            stmt.setDouble(idx++, minInvest);
            stmt.setDouble(idx++, cash);
            stmt.setDouble(idx++, compoundTally);
            stmt.setDouble(idx++, tallySlice);
            stmt.setInt(idx++, tallySliceCnt);
            stmt.setInt(idx++, spread);
            stmt.setInt(idx, investPercent);

            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        } finally {
            Database.close(stmt, connection);
        }
    }

    private boolean isStateSaved() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM compounder_state WHERE dt = ?");
            stmt.setDate(1, java.sql.Date.valueOf(firstOfThisMonth()));
            rs = stmt.executeQuery();
            if(rs.next()) {
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

    private boolean load() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT start_bank, " +
                            " min_invest," +
                            " cash," +
                            " compound_tally," +
                            " tally_slice," +
                            " tally_slice_cnt," +
                            " spread," +
                            " invest_pc " +
                    "FROM compounder_state WHERE dt = ?");
            stmt.setDate(1, java.sql.Date.valueOf(firstOfThisMonth()));
            rs = stmt.executeQuery();
            if(rs.next()) {
                int idx = 1;
                startBank = rs.getDouble(idx++);
                minInvest = rs.getDouble(idx++);
                cash = rs.getDouble(idx++);
                compoundTally = rs.getDouble(idx++);
                tallySlice = rs.getDouble(idx++);
                tallySliceCnt = rs.getInt(idx++);
                spread = rs.getInt(idx++);
                investPercent = rs.getInt(idx);
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

    private LocalDate firstOfThisMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate firstOfLastMonth() {
        return LocalDate.now().withDayOfMonth(1).minusMonths(1);
    }
}
