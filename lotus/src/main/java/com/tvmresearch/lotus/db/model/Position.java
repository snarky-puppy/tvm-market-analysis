package com.tvmresearch.lotus.db.model;

import com.ib.controller.*;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;

import java.sql.*;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Position record
 *
 * Created by matt on 24/03/16.
 */
public class Position {
    public Trigger trigger;

    public Integer id = null;

    // compounder accounting
    public double cmpMin;
    public double cmpVal;
    public double cmpTotal;

    /* ib */
    public int orderId;

    /* buying */

    // 0.1% higher than trigger close price
    public double buyLimit;
    public LocalDate buyDate;

    // quantity stocks needed to fill cmpTotal
    public int qty;
    // price of qty stocks
    public double qtyValue = 0.0;
    // qty stocks actually filled
    public Integer qtyFilled;
    // value of stocsk actually filled
    public Double qtyFilledValue;

    /* selling */

    // sell when value reaches this limit (>10%)
    public double sellLimit;
    // sell when date reaches this limit (84 days)
    public LocalDate sellDateLimit;

    // actual sell price
    public Double sellPrice;

    // date of selling
    public LocalDate sellDateStart;
    public LocalDate sellDateEnd;
    public Integer errorCode;
    public String errorMsg;


    public Position(Trigger trigger) {
        this.trigger = trigger;
        this.buyDate = LocalDate.now();
    }

    public void serialise() {
        Connection connection = Database.connection();
        serialise(connection);
        Database.close(connection);
    }

    public static Position load(String symbol) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT * FROM positions p WHERE trigger_id = " +
                    "(SELECT id FROM triggers WHERE symbol = ? AND p.buy_dt = trigger_date)");
            rs = stmt.executeQuery();

            if(!rs.next())
                return null;

            int positionId = rs.getInt("id");
            int triggerId = rs.getInt("trigger_id");

            Position position = new Position(Trigger.load(triggerId));
            position.id = positionId;

            position.cmpMin = rs.getDouble("cmp_min");
            position.cmpVal = rs.getDouble("cmp_val");
            position.cmpTotal = rs.getDouble("cmp_total");

            position.orderId = rs.getInt("order_id");

            /* buying */
            position.buyLimit = rs.getDouble("buy_limit");
            position.buyDate = rs.getDate("buy_dt").toLocalDate();
            position.qty = rs.getInt("qty");
            position.qtyValue = rs.getDouble("qty_val");

            position.qtyFilled = rs.getInt("qty_filled");
            if(rs.wasNull())
                position.qtyFilled = null;

            position.qtyFilledValue = rs.getDouble("qty_filled_val");
            if(rs.wasNull())
                position.qtyFilledValue = null;

            /* selling */
            position.sellLimit = rs.getDouble("sell_limit");
            position.sellDateLimit = rs.getDate("sell_dt_limit").toLocalDate();

            position.sellPrice = rs.getDouble("sell_price");
            if(rs.wasNull())
                position.sellPrice = null;

            position.sellDateStart = rs.getDate("sell_dt_start") == null ? null : rs.getDate("sell_dt_start").toLocalDate();
            position.sellDateEnd = rs.getDate("sell_dt_end") == null ? null : rs.getDate("sell_dt_end").toLocalDate();

            position.errorCode = rs.getInt("error_code");
            if(rs.wasNull())
                position.errorCode = null;
            position.errorMsg = rs.getString("error_msg");

            return position;

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    public void serialise(Connection connection) {
        PreparedStatement stmt = null;

        try {
            if (id == null) {
                final String sql = "INSERT INTO positions " +
                        "(trigger_id, cmp_min, cmp_val, cmp_total, order_id, buy_limit, buy_dt, " +
                        "qty, qty_val, qty_filled, qty_filled_val, sell_limit, sell_dt_limit, " +
                        "sell_price, sell_dt_start, sell_dt_end, error_code, error_msg) " +
                        "VALUES("+Database.generateParams(18)+")";
                stmt = connection.prepareStatement(sql);
            } else {
                final String sql = "UPDATE positions " +
                        "SET    trigger_id=?, cmp_min=?, cmp_val=?, cmp_total=?, buy_limit=?, buy_dt=?, " +
                        "qty=?, qty_val=?, qty_filled=?, qty_filled_val=?, sell_limit=?, sell_dt_limit=?, " +
                        "sell_price=?, sell_dt_start=?, sell_dt_end=?, error_code=?, error_msg=? " +
                        "WHERE  id = ?";
                stmt = connection.prepareStatement(sql);
            }

            stmt.setInt(1, trigger.id);

            stmt.setDouble(2, cmpMin);
            stmt.setDouble(3, cmpVal);
            stmt.setDouble(4, cmpTotal);

            stmt.setInt(5, orderId);

            stmt.setDouble(6, buyLimit);

            stmt.setDate(7, java.sql.Date.valueOf(buyDate));
            stmt.setInt(8, qty);
            stmt.setDouble(9, qtyValue);

            if(qtyFilled == null)
                stmt.setNull(10, Types.NUMERIC);
            else
                stmt.setInt(10, qtyFilled);

            if(qtyFilledValue == null)
                stmt.setNull(11, Types.NUMERIC);
            else
                stmt.setDouble(11, qtyFilledValue);

            /* selling */
            stmt.setDouble(12, sellLimit);
            stmt.setDate(13, java.sql.Date.valueOf(sellDateLimit));
            if(sellPrice == null)
                stmt.setNull(14, Types.NUMERIC);
            else
                stmt.setDouble(14, sellPrice);

            if(sellDateStart == null)
                stmt.setNull(15, Types.DATE);
            else
                stmt.setDate(16, java.sql.Date.valueOf(sellDateStart));

            if(sellDateEnd == null)
                stmt.setNull(16, Types.DATE);
            else
                stmt.setDate(16, java.sql.Date.valueOf(sellDateEnd));

            if(errorCode == null)
                stmt.setNull(17, Types.NUMERIC);
            else
                stmt.setInt(17, errorCode);

            if(errorMsg == null)
                stmt.setNull(18, Types.VARCHAR);
            else
                stmt.setString(18, errorMsg);

            if (id != null)
                stmt.setInt(19, id);

            stmt.execute();

        } catch (SQLException e) {
            throw new LotusException(e);

        } finally {
            Database.close(stmt);
        }
    }

    public NewContract createNewContract() {
        NewContract rv = new NewContract();
        if(trigger.exchange.compareTo("ASX") == 0)
            rv.currency("AUD");
        else
            rv.currency("USD");

        if(trigger.exchange.compareTo("NYSE_Arca") == 0)
            trigger.exchange = "ARCA";

        rv.exchange("SMART");
        rv.symbol(trigger.symbol);

        //rv.secId(trigger.symbol);
        rv.primaryExch(trigger.exchange);
        rv.secType(com.ib.controller.Types.SecType.STK);
        return rv;
    }

    public NewOrder createNewOrder(String account) {
        NewOrder order = new NewOrder();
        order.account(account);
        order.action(com.ib.controller.Types.Action.BUY);
        order.totalQuantity(qty);
        order.orderType(OrderType.LMT);
        order.lmtPrice(buyLimit);
        order.tif(com.ib.controller.Types.TimeInForce.DAY);
        order.transmit(true);
        return order;
    }
}
