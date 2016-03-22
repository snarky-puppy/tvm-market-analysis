package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.sql.*;

/**
 * Database interface
 *
 * Created by horse on 19/03/2016.
 */
public class Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    public static Connection connection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost/lotus", "lotus", "lotus");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Cannot connect to DB", e);
            throw new LotusException(e);
        }
    }

    private static String generateParams(int nParams) {
        StringBuilder builder = new StringBuilder();
        while(nParams > 0) {
            builder.append("?");
            nParams --;
            if(nParams > 0)
                builder.append(',');
        }
        return builder.toString();
    }

    public static void serialise(Connection connection, Trigger trigger) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO triggers VALUES(NULL,"+generateParams(10)+")");
        try {
            stmt.setString(1, trigger.exchange);
            stmt.setString(2, trigger.symbol);
            stmt.setDate(3, new Date(trigger.date.getTime()));
            stmt.setDouble(4, trigger.price);
            stmt.setDouble(5, trigger.zscore);
            stmt.setDouble(6, trigger.avgVolume);
            stmt.setDouble(7, trigger.avgPrice);
            stmt.setBoolean(8, trigger.seen);
            stmt.setBoolean(9, trigger.actioned);
            stmt.setBoolean(10, trigger.ignored);

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    public static void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
