package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;


import java.sql.*;

/**
 * Database interface
 *
 * Created by horse on 19/03/2016.
 */
public class Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    private final static DataSource dataSource;

    static {
        dataSource = new DataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost/lotus?useSSL=false");
        dataSource.setUsername("lotus");
        dataSource.setPassword("lotus");
        dataSource.setInitialSize(10);
        dataSource.setMaxActive(50);
        dataSource.setMaxIdle(20);
        dataSource.setMinIdle(10);
        dataSource.setLogAbandoned(true);
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(10);
    }

    public static Connection connection() {

        try {
            Connection connection = dataSource.getConnection();
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        }

        /*
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost/lotus?useSSL=false", "lotus", "lotus");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Cannot connect to DB", e);
            throw new LotusException(e);
        }
        */
    }

    public static String generateParams(int nParams) {
        StringBuilder builder = new StringBuilder();
        while(nParams > 0) {
            builder.append("?");
            nParams --;
            if(nParams > 0)
                builder.append(',');
        }
        return builder.toString();
    }

    public static void close(Connection connection) {
        try {
            if(connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close(PreparedStatement stmt) {
        try {
            if(stmt != null)
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static void close(ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close(PreparedStatement stmt, Connection connection) {
        close(stmt);
        close(connection);
    }

    public static void close(ResultSet rs, PreparedStatement stmt, Connection connection) {
        close(rs);
        close(stmt);
        close(connection);
    }

    public static void close(ResultSet rs, PreparedStatement stmt) {
        close(rs);
        close(stmt);
    }
}
