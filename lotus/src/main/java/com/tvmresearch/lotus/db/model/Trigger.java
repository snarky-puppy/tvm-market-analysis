package com.tvmresearch.lotus.db.model;


import com.tvmresearch.lotus.Configuration;
import com.tvmresearch.lotus.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Triggers bean
 *
 * Created by horse on 19/03/2016.
 */

public class Trigger {

    private static final Logger logger = LogManager.getLogger(Trigger.class);

    public Trigger() {}

    public String exchange;

    public String symbol;

    public Date date;

    public double price;

    public double zscore;

    public double avgVolume;

    public double avgPrice;

    public boolean event = false;
    public boolean expired = false;

    public void serialise(Connection connection) throws SQLException {

        int elapsedDays = elapsedDays(connection);
        if(elapsedDays == -1 || elapsedDays > Configuration.RETRIGGER_MIN_DAYS)
            event = true;

        PreparedStatement stmt = connection.prepareStatement("INSERT INTO triggers VALUES(NULL,"+ Database.generateParams(9)+")");
        try {
            stmt.setString(1, exchange);
            stmt.setString(2, symbol);
            stmt.setDate(3, new java.sql.Date(date.getTime()));
            stmt.setDouble(4, price);
            stmt.setDouble(5, zscore);
            stmt.setDouble(6, avgVolume);
            stmt.setDouble(7, avgPrice);
            stmt.setBoolean(8, event);
            stmt.setBoolean(9, expired);

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trigger{");
        sb.append("exchange='").append(exchange).append('\'');
        sb.append(", symbol='").append(symbol).append('\'');
        sb.append(", date=").append(date);
        sb.append(", price=").append(price);
        sb.append(", zscore=").append(zscore);
        sb.append(", avgVolume=").append(avgVolume);
        sb.append(", avgPrice=").append(avgPrice);
        sb.append(", event=").append(event);
        sb.append(", expired=").append(expired);
        sb.append('}');
        return sb.toString();
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
            stmt.setDate(3, new java.sql.Date(date.getTime()));
            ResultSet rs = stmt.executeQuery();
            int nDays = -1;
            if(rs.next()) {
                Date lastDate = rs.getDate(1);
                long diff = date.getTime() - lastDate.getTime();
                nDays = (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            }
            return nDays;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if(stmt != null)
                Database.close(stmt);
        }
        return -1;
    }
}
