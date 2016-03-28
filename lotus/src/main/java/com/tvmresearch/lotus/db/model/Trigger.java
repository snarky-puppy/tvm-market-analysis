package com.tvmresearch.lotus.db.model;


import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.Configuration;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Triggers bean
 *
 * Created by horse on 19/03/2016.
 */

public class Trigger {

    private static final Logger logger = LogManager.getLogger(Trigger.class);

    public Trigger() {}

    public Integer id = null;

    public String exchange;

    public String symbol;

    public LocalDate date;

    public double price;

    public double zscore;

    public double avgVolume;

    public double avgPrice;

    public boolean event = false;

    public RejectReason rejectReason = RejectReason.NOTPROCESSED;
    public Double rejectData = null;


    public enum RejectReason {
        NOTEVENT,
        NOTPROCESSED,
        ZSCORE,
        CATEGORY,
        VOLUME,
        INVESTAMT,
        NOFUNDS,
        OK
    }


    public void serialise() {
        Connection connection = Database.connection();
        serialise(connection);
        Database.close(connection);
    }

    public void serialise(Connection connection) {
        PreparedStatement stmt = null;

        try {

            if (id == null) {
                int elapsedDays = elapsedDays(connection);
                if (elapsedDays == -1 || elapsedDays > Configuration.RETRIGGER_MIN_DAYS) {
                    event = true;
                    rejectReason = RejectReason.NOTPROCESSED;
                } else {
                    rejectReason = RejectReason.NOTEVENT;
                }
                stmt = connection.prepareStatement("INSERT INTO triggers VALUES(NULL," + Database.generateParams(10) + ")");
            } else {
                final String sql = "UPDATE triggers " +
                        "SET    exchange=?, symbol=?, trigger_date=?, price=?, " +
                        "       zscore=?, avg_volume=?, avg_price=?, event=?, " +
                        "       reject_reason=?, reject_data=? " +
                        "WHERE  id = ?";
                stmt = connection.prepareStatement(sql);
            }

            stmt.setString(1, exchange);
            stmt.setString(2, symbol);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setDouble(4, price);
            stmt.setDouble(5, zscore);
            stmt.setDouble(6, avgVolume);
            stmt.setDouble(7, avgPrice);
            stmt.setBoolean(8, event);
            stmt.setString(9, rejectReason.name());
            if (rejectData == null)
                stmt.setNull(10, Types.NUMERIC);
            else
                stmt.setDouble(10, rejectData);

            if (id != null)
                stmt.setInt(11, id);

            stmt.execute();
        } catch(MySQLIntegrityConstraintViolationException e) {
            // ignore
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt);
        }
    }

    public static List<Trigger> getTodaysTriggers() {
        List<Trigger> rv = new ArrayList<>();
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        final String sql = "SELECT id, exchange, symbol, trigger_date, price, zscore, avg_volume, avg_price, event, reject_reason, reject_data"
                         + " FROM triggers"
                         + " WHERE trigger_date > ? AND event = TRUE AND reject_reason = ?";

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);
            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now().minusDays(3)));
            stmt.setString(2, RejectReason.NOTPROCESSED.name());
            rs = stmt.executeQuery();
            while(rs.next()) {
                Trigger trigger = new Trigger();

                trigger.id = rs.getInt(1);
                trigger.exchange = rs.getString(2);
                trigger.symbol = rs.getString(3);
                trigger.date = rs.getDate(4) == null ? null : rs.getDate(4).toLocalDate();
                trigger.price = rs.getDouble(5);
                trigger.zscore = rs.getDouble(6);
                trigger.avgVolume = rs.getDouble(7);
                trigger.avgPrice = rs.getDouble(8);
                trigger.event = rs.getBoolean(9);
                trigger.rejectReason = RejectReason.valueOf(rs.getString(10));

                trigger.rejectData = rs.getDouble(11);
                if(rs.wasNull())
                    trigger.rejectData = null;

                rv.add(trigger);
            }
            return rv;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    public static Trigger load(int id) {
        Trigger rv = null;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        final String sql = "SELECT id, exchange, symbol, trigger_date, price, zscore, " +
                "avg_volume, avg_price, event, reject_reason, reject_data"
                + " FROM triggers"
                + " WHERE id = ?";

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(sql);
            stmt.setDate(1, new java.sql.Date(new Date().getTime()));
            rs = stmt.executeQuery();
            while(rs.next()) {
                Trigger trigger = new Trigger();

                trigger.id = rs.getInt(1);
                trigger.exchange = rs.getString(2);
                trigger.symbol = rs.getString(3);
                trigger.date = rs.getDate(4) == null ? null : rs.getDate(4).toLocalDate();
                trigger.price = rs.getDouble(5);
                trigger.zscore = rs.getDouble(6);
                trigger.avgVolume = rs.getDouble(7);
                trigger.avgPrice = rs.getDouble(8);
                trigger.event = rs.getBoolean(9);
                trigger.rejectReason = RejectReason.valueOf(rs.getString(10));
                trigger.rejectData = rs.getDouble(11);
                if(rs.wasNull())
                    trigger.rejectData = null;

                return trigger;
            }
            return null;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "id=" + id +
                ", exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                ", date=" + date +
                ", price=" + price +
                ", zscore=" + zscore +
                ", avgVolume=" + avgVolume +
                ", avgPrice=" + avgPrice +
                ", event=" + event +
                ", rejectReason=" + rejectReason +
                ", rejectData=" + rejectData +
                '}';
    }

    public int elapsedDays(Connection connection) {
        StringBuffer sb = new StringBuffer("SELECT trigger_date ");
        sb.append(" FROM triggers ");
        sb.append(" WHERE symbol = ? AND exchange = ? AND trigger_date < ?");
        sb.append(" ORDER BY trigger_date DESC LIMIT 1");

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sb.toString());
            stmt.setString(1, symbol);
            stmt.setString(2, exchange);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            int nDays = -1;
            if(rs.next()) {
                LocalDate lastDate = rs.getDate(1).toLocalDate();
                nDays = (int) ChronoUnit.DAYS.between(lastDate, date);
            }
            return nDays;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt);
        }
    }
}
