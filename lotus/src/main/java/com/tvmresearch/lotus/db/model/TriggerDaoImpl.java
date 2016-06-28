package com.tvmresearch.lotus.db.model;

import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.Configuration;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CRUD for Trigger objects
 * <p>
 * Created by horse on 7/04/2016.
 */
public class TriggerDaoImpl implements TriggerDao {

    private void serialise(Trigger trigger, Connection connection) {

        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            if (trigger.id == null) {
                int elapsedDays = elapsedDays(trigger, connection);
                if (elapsedDays == -1 || elapsedDays > Configuration.RETRIGGER_MIN_DAYS) {
                    trigger.event = true;
                    trigger.rejectReason = Trigger.RejectReason.NOTPROCESSED;
                } else {
                    trigger.rejectReason = Trigger.RejectReason.NOTEVENT;
                }
                stmt = connection.prepareStatement(
                        "INSERT INTO triggers VALUES(NULL," + Database.generateParams(10) + ")",
                        Statement.RETURN_GENERATED_KEYS);
            } else {
                final String sql = "UPDATE triggers " +
                        "SET    exchange=?, symbol=?, trigger_date=?, price=?, " +
                        "       zscore=?, avg_volume=?, avg_price=?, event=?, " +
                        "       reject_reason=?, reject_data=? " +
                        "WHERE  id = ?";
                stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }

            stmt.setString(1, trigger.exchange);
            stmt.setString(2, trigger.symbol);
            stmt.setDate(3, java.sql.Date.valueOf(trigger.date));
            stmt.setDouble(4, trigger.price);
            stmt.setDouble(5, trigger.zscore);
            stmt.setDouble(6, trigger.avgVolume);
            stmt.setDouble(7, trigger.avgPrice);
            stmt.setBoolean(8, trigger.event);
            stmt.setString(9, trigger.rejectReason.name());
            if (trigger.rejectData == null)
                stmt.setNull(10, Types.NUMERIC);
            else
                stmt.setDouble(10, trigger.rejectData);

            if (trigger.id != null)
                stmt.setInt(11, trigger.id);

            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                trigger.id = rs.getInt(1);
            }
        } catch (MySQLIntegrityConstraintViolationException e) {
            // ignore
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt);
        }
    }

    @Override
    public List<Trigger> getTodaysTriggers() {
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
            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now().minusDays(5)));
            stmt.setString(2, Trigger.RejectReason.NOTPROCESSED.name());
            rs = stmt.executeQuery();
            while (rs.next()) {
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
                trigger.rejectReason = Trigger.RejectReason.valueOf(rs.getString(10));

                trigger.rejectData = rs.getDouble(11);
                if (rs.wasNull())
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

    @Override
    public Trigger load(int id) {
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
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
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
                trigger.rejectReason = Trigger.RejectReason.valueOf(rs.getString(10));
                trigger.rejectData = rs.getDouble(11);
                if (rs.wasNull())
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
    public int elapsedDays(Trigger trigger) {
        Connection connection = null;
        try {
            connection = Database.connection();
            return elapsedDays(trigger, connection);
        } finally {
            Database.close(connection);
        }

    }


    private int elapsedDays(Trigger trigger, Connection connection) {
        PreparedStatement stmt = null;

        String sql = "SELECT trigger_date" +
                "     FROM triggers" +
                "     WHERE symbol = ? " +
                "       AND exchange = ? " +
                "       AND trigger_date < ? " +
                "     ORDER BY trigger_date DESC LIMIT 1";

        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, trigger.symbol);
            stmt.setString(2, trigger.exchange);
            stmt.setDate(3, java.sql.Date.valueOf(trigger.date));
            ResultSet rs = stmt.executeQuery();
            int nDays = -1;
            if (rs.next()) {
                LocalDate lastDate = rs.getDate(1).toLocalDate();
                nDays = (int) ChronoUnit.DAYS.between(lastDate, trigger.date);
            }
            return nDays;
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(stmt);
        }
    }


    @Override
    public void serialise(List<Trigger> list) {
        final Connection connection = Database.connection();

        try {
            connection.setAutoCommit(false);
            list.stream().forEach(x -> serialise(x, connection));
            connection.commit();
        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new LotusException(e);
            }
            Database.close(connection);
        }
    }

    @Override
    public void serialise(Trigger trigger) {
        serialise(Arrays.asList(trigger));
    }
}
