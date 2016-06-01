package com.tvmresearch.lotus.db.model;

import com.mysql.fabric.xmlrpc.base.Data;
import com.mysql.jdbc.*;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object - Investment
 *
 * Created by horse on 7/04/2016.
 */
public class InvestmentDaoImpl implements InvestmentDao {
    private static final Logger logger = LogManager.getLogger(InvestmentDaoImpl.class);

    private static String insertSQL;
    private static String updateSQL;

    static {
        final String[] fields = {
                "trigger_id",
                "cmp_min",
                "cmp_val",
                "cmp_total",
                "buy_order_id",
                "sell_order_id",
                "buy_perm_id",
                "sell_perm_id",
                "con_id",
                "state",
                "buy_limit",
                "buy_dt",
                "qty",
                "qty_val",
                "qty_filled",
                "qty_filled_val",
                "sell_limit",
                "sell_dt_limit",
                "sell_price",
                "sell_dt_start",
                "sell_dt_end",
                "real_pnl",
                "error_code",
                "error_msg"
        };

        insertSQL = Database.generateInsertSQL("investments", fields);
        updateSQL = Database.generateUpdateSQL("investments", "id", fields);
    }

    @Override
    public List<Investment> getTradesInProgress(int conId) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT * FROM investments p " +
                    "WHERE con_id = ? AND (state = 'BUY' OR state = 'SELL')");
            stmt.setLong(1, conId);
            rs = stmt.executeQuery();

            ArrayList<Investment> rv = new ArrayList<>();
            while (rs.next()) {
                rv.add(populate(rs));
            }
            return rv;

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public List<Investment> getFilledInvestments() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT * FROM investments p " +
                    "WHERE state = 'FILLED'");
            rs = stmt.executeQuery();

            ArrayList<Investment> rv = new ArrayList<>();
            while (rs.next()) {
                rv.add(populate(rs));
            }
            return rv;

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public void serialise(List<Investment> investments) {
        investments.forEach(i -> serialise(i));
    }

    @Override
    public void serialise(Investment investment) {


        ResultSet rs = null;
        PreparedStatement stmt = null;
        Connection connection = Database.connection();

        logger.debug(String.format("symbol=%s exchange=%s: conId=%d", investment.trigger.symbol, investment.trigger.exchange,
                investment.conId));

        try {
            if (investment.id == null) {
                stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            } else {
                stmt = connection.prepareStatement(updateSQL, Statement.RETURN_GENERATED_KEYS);
            }

            int idx = 1;

            stmt.setInt(idx++, investment.trigger.id);

            stmt.setDouble(idx++, investment.cmpMin);
            stmt.setDouble(idx++, investment.cmpVal);
            stmt.setDouble(idx++, investment.cmpTotal);

            stmt.setLong(idx++, investment.buyOrderId);
            stmt.setLong(idx++, investment.sellOrderId);
            stmt.setLong(idx++, investment.buyPermId);
            stmt.setLong(idx++, investment.sellPermId);
            stmt.setLong(idx++, investment.conId);

            stmt.setString(idx++, investment.state.name());

            stmt.setDouble(idx++, investment.buyLimit);

            stmt.setDate(idx++, java.sql.Date.valueOf(investment.buyDate));
            stmt.setInt(idx++, investment.qty);
            stmt.setDouble(idx++, investment.qtyValue);

            if (investment.qtyFilled == null)
                stmt.setNull(idx++, Types.NUMERIC);
            else
                stmt.setInt(idx++, investment.qtyFilled);

            if (investment.qtyFilledValue == null)
                stmt.setNull(idx++, Types.NUMERIC);
            else
                stmt.setDouble(idx++, investment.qtyFilledValue);

            //* selling
            stmt.setDouble(idx++, investment.sellLimit);
            stmt.setDate(idx++, java.sql.Date.valueOf(investment.sellDateLimit));
            if (investment.sellPrice == null)
                stmt.setNull(idx++, Types.NUMERIC);
            else
                stmt.setDouble(idx++, investment.sellPrice);

            if (investment.sellDateStart == null)
                stmt.setNull(idx++, Types.DATE);
            else
                stmt.setDate(idx++, java.sql.Date.valueOf(investment.sellDateStart));

            if (investment.sellDateEnd == null)
                stmt.setNull(idx++, Types.DATE);
            else
                stmt.setDate(idx++, java.sql.Date.valueOf(investment.sellDateEnd));

            if (investment.realPnL == null)
                stmt.setNull(idx++, Types.NUMERIC);
            else
                stmt.setDouble(idx++, investment.realPnL);

            if (investment.errorCode == null)
                stmt.setNull(idx++, Types.NUMERIC);
            else
                stmt.setInt(idx++, investment.errorCode);

            if (investment.errorMsg == null)
                stmt.setNull(idx++, Types.VARCHAR);
            else
                stmt.setString(idx++, investment.errorMsg);

            if (investment.id != null)
                stmt.setInt(idx, investment.id);

            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                investment.id = rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new LotusException(e);

        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public void addHistory(Investment investment, LocalDate date, double close) {
        Connection connection = Database.connection();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("INSERT INTO investment_history VALUES(NULL, ?, ?, ?)");
            stmt.setInt(1, investment.id);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setDouble(3, close);
            stmt.execute();
        } catch (MySQLIntegrityConstraintViolationException e) {
            //logger.info("Ignoring: "+e.getMessage());
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt, connection);
        }
    }

    @Override
    public Investment findUnconfirmed(String symbol) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT * FROM investments " +
                    "WHERE state = 'UNCONFIRMED' AND trigger_id IN " +
                    "(SELECT id FROM triggers WHERE symbol = ? AND reject_reason = 'OK')");
            stmt.setString(1, symbol);
            rs = stmt.executeQuery();

            if (rs.next())
                return populate(rs);
            else
                return null;


        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }


    private Investment populate(ResultSet rs) throws SQLException {
        int investmentId = rs.getInt("id");
        int triggerId = rs.getInt("trigger_id");

        Investment investment = new Investment(new TriggerDaoImpl().load(triggerId));
        investment.id = investmentId;

        investment.cmpMin = rs.getDouble("cmp_min");
        investment.cmpVal = rs.getDouble("cmp_val");
        investment.cmpTotal = rs.getDouble("cmp_total");

        investment.buyOrderId = rs.getLong("buy_order_id");
        investment.sellOrderId = rs.getLong("sell_order_id");
        investment.buyPermId = rs.getLong("buy_perm_id");
        investment.sellPermId = rs.getLong("sell_perm_id");
        investment.conId = rs.getLong("con_id");

        investment.state = Investment.State.valueOf(rs.getString("state"));

        //* buying
        investment.buyLimit = rs.getDouble("buy_limit");
        investment.buyDate = rs.getDate("buy_dt").toLocalDate();
        investment.qty = rs.getInt("qty");
        investment.qtyValue = rs.getDouble("qty_val");

        investment.qtyFilled = rs.getInt("qty_filled");
        if (rs.wasNull())
            investment.qtyFilled = null;

        investment.qtyFilledValue = rs.getDouble("qty_filled_val");
        if (rs.wasNull())
            investment.qtyFilledValue = null;

        //* selling
        investment.sellLimit = rs.getDouble("sell_limit");
        investment.sellDateLimit = rs.getDate("sell_dt_limit").toLocalDate();

        investment.sellPrice = rs.getDouble("sell_price");
        if (rs.wasNull())
            investment.sellPrice = null;

        investment.sellDateStart = rs.getDate("sell_dt_start") == null ? null : rs.getDate("sell_dt_start").toLocalDate();
        investment.sellDateEnd = rs.getDate("sell_dt_end") == null ? null : rs.getDate("sell_dt_end").toLocalDate();

        investment.realPnL = rs.getDouble("real_pnl");
        if (rs.wasNull())
            investment.realPnL = null;

        investment.errorCode = rs.getInt("error_code");
        if (rs.wasNull())
            investment.errorCode = null;
        investment.errorMsg = rs.getString("error_msg");

        return investment;
    }

}
