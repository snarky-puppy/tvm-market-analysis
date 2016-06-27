package com.tvmresearch.lotus.db.model;

import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Data Access Object - Investment
 * <p>
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
                "buy_fill_val",
                "buy_commission",
                "sell_limit",
                "sell_dt_limit",
                "avg_sell_price",
                "sell_fill_val",
                "sell_dt_start",
                "sell_dt_end",
                "sell_commission",
                "market_price",
                "market_value",
                "avg_cost",
                "real_pnl",
                "error_code",
                "error_msg"
        };

        insertSQL = Database.generateInsertSQL("investments", fields);
        updateSQL = Database.generateUpdateSQL("investments", "id", fields);
    }


    @Override
    public List<Investment> getPositions() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT * FROM investments WHERE state = ?");
            stmt.setString(1, String.valueOf(Investment.State.BUYFILLED));
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

        logger.debug("serialise: " + investment);

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

            setInt(idx++, investment.qtyFilled, stmt);

            setDouble(idx++, investment.buyFillValue, stmt);
            setDouble(idx++, investment.buyCommission, stmt);

            //* selling
            stmt.setDouble(idx++, investment.sellLimit);
            stmt.setDate(idx++, java.sql.Date.valueOf(investment.sellDateLimit));

            setDouble(idx++, investment.avgSellPrice, stmt);
            setDouble(idx++, investment.sellFillVal, stmt);

            setDate(idx++, investment.sellDateStart, stmt);
            setDate(idx++, investment.sellDateEnd, stmt);
            setDouble(idx++, investment.sellCommission, stmt);

            setDouble(idx++, investment.marketPrice, stmt);
            setDouble(idx++, investment.marketValue, stmt);
            setDouble(idx++, investment.avgCost, stmt);
            setDouble(idx++, investment.realPnL, stmt);

            setInt(idx++, investment.errorCode, stmt);
            setString(idx++, investment.errorMsg, stmt);

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

    private void setString(int idx, String value, PreparedStatement stmt) throws SQLException {
        if (value == null)
            stmt.setNull(idx, Types.VARCHAR);
        else
            stmt.setString(idx, value);
    }

    private void setDate(int idx, LocalDate value, PreparedStatement stmt) throws SQLException {
        if (value == null)
            stmt.setNull(idx, Types.DATE);
        else
            stmt.setDate(idx, java.sql.Date.valueOf(value));
    }

    private void setInt(int idx, Integer value, PreparedStatement stmt) throws SQLException {
        if (value == null)
            stmt.setNull(idx, Types.NUMERIC);
        else
            stmt.setInt(idx, value);
    }

    private void setDouble(int idx, Double value, PreparedStatement stmt) throws SQLException {
        if (value == null)
            stmt.setNull(idx, Types.NUMERIC);
        else
            stmt.setDouble(idx, value);
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
                            "WHERE state = ? AND trigger_id IN " +
                            "(SELECT id FROM triggers WHERE symbol = ? AND reject_reason = 'OK')");
            stmt.setString(1, String.valueOf(Investment.State.BUYUNCONFIRMED));
            stmt.setString(2, symbol);
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

    @Override
    public Investment findOrder(int orderId) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT * FROM investments " +
                            "WHERE buy_order_id = ? OR sell_order_id = ?");
            stmt.setLong(1, orderId);
            stmt.setLong(2, orderId);
            rs = stmt.executeQuery();

            if (rs.next())
                return populate(rs);
            else {
                logger.warn(String.format("findOrder: orderId %d not found", orderId));
                return null;
            }


        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }

    }

    @Override
    public Investment findConId(int conid) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT * FROM investments " +
                            "WHERE con_id = ? ORDER BY id DESC LIMIT 1");
            stmt.setLong(1, conid);
            rs = stmt.executeQuery();

            if (rs.next())
                return populate(rs);
            else {
                logger.warn(String.format("findOrder: conId %d not found", conid));
                return null;
            }


        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public Map<LocalDate, Double> getHistory(Investment investment) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT * FROM investment_history " +
                            "WHERE investment_id = ? ORDER BY dt");
            stmt.setLong(1, investment.id);
            rs = stmt.executeQuery();

            Map<LocalDate, Double> rv = new HashMap<>();

            while (rs.next()) {
                rv.put(rs.getDate("dt").toLocalDate(), rs.getDouble("close"));
            }

            if (rv.size() == 0)
                return null;
            else
                return rv;

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public int getHistoricalMissingDays(Investment investment) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT MAX(dt) AS dt FROM investment_history " +
                            "WHERE investment_id = ?");
            stmt.setLong(1, investment.id);
            rs = stmt.executeQuery();

            LocalDate historyStart = investment.trigger.date;

            if (rs.next()) {
                if (rs.getDate("dt") != null)
                    historyStart = rs.getDate("dt").toLocalDate();
            }

            return (int) DAYS.between(historyStart, LocalDate.now());

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public double getLastHistoricalClose(Investment investment) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT close FROM investment_history " +
                            "WHERE investment_id = ? ORDER BY dt DESC LIMIT 1");
            stmt.setLong(1, investment.id);
            rs = stmt.executeQuery();

            LocalDate historyStart = investment.trigger.date;

            if (rs.next()) {
                return rs.getDouble("close");
            }
            return -1;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public int outstandingBuyOrders() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "SELECT COUNT(*) AS cnt FROM investments " +
                            "WHERE state IN (?,?,?);");
            stmt.setString(1, String.valueOf(Investment.State.BUYUNCONFIRMED));
            stmt.setString(2, String.valueOf(Investment.State.BUYPRESUBMITTED));
            stmt.setString(3, String.valueOf(Investment.State.BUYOPEN));

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("cnt");
            }

            return 0;
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

        investment.buyFillValue = rs.getDouble("buy_fill_val");
        if (rs.wasNull())
            investment.buyFillValue = null;

        investment.buyCommission = rs.getDouble("buy_commission");
        if (rs.wasNull())
            investment.buyCommission = null;


        //* selling
        investment.sellLimit = rs.getDouble("sell_limit");
        investment.sellDateLimit = rs.getDate("sell_dt_limit").toLocalDate();

        investment.avgSellPrice = rs.getDouble("avg_sell_price");
        if (rs.wasNull())
            investment.avgSellPrice = null;

        investment.sellFillVal = rs.getDouble("sell_fill_val");
        if (rs.wasNull())
            investment.sellFillVal = null;

        investment.sellDateStart = rs.getDate("sell_dt_start") == null ? null : rs.getDate("sell_dt_start").toLocalDate();
        investment.sellDateEnd = rs.getDate("sell_dt_end") == null ? null : rs.getDate("sell_dt_end").toLocalDate();

        investment.sellCommission = rs.getDouble("sell_commission");
        if (rs.wasNull())
            investment.sellCommission = null;

        investment.marketPrice = rs.getDouble("market_price");
        if (rs.wasNull())
            investment.marketPrice = null;

        investment.marketValue = rs.getDouble("market_value");
        if (rs.wasNull())
            investment.marketValue = null;

        investment.avgCost = rs.getDouble("avg_cost");
        if (rs.wasNull())
            investment.avgCost = null;

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
