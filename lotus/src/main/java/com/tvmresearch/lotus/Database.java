package com.tvmresearch.lotus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Database interface
 * <p>
 * Created by horse on 19/03/2016.
 */
public class Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    private static DataSource dataSource;

    static {
        dataSource = new DataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost/lotus2?useSSL=false&autoReconnect=true");
        dataSource.setUsername("lotus2");
        dataSource.setPassword("lotus2");
        dataSource.setInitialSize(10);
        dataSource.setMaxActive(50);
        dataSource.setMaxIdle(20);
        dataSource.setMinIdle(10);
        dataSource.setLogAbandoned(true);
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(60);
    }

    public static Connection connection() {

        try {
            return dataSource.getConnection();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        }
    }

    public static void setDataSource(DataSource ds) {
        dataSource = ds;
    }

    public static String generateParams(int nParams) {
        StringBuilder builder = new StringBuilder();
        while (nParams > 0) {
            builder.append("?");
            nParams--;
            if (nParams > 0)
                builder.append(',');
        }
        return builder.toString();
    }

    public static void close(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Closing Connection", e);
            e.printStackTrace();
        }
    }

    public static void close(PreparedStatement stmt) {
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException e) {
            logger.error("Closing PreparedStatement", e);
            e.printStackTrace();
        }

    }

    public static void close(ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            logger.error("Closing ResultSet", e);
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

    public static String generateInsertParams(String[] fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(String.join(",", fields));
        sb.append(") VALUES(");
        sb.append(generateParams(fields.length));
        sb.append(")");
        return sb.toString();
    }

    public static String generateInsertSQL(String table, String[] fields) {
        return "INSERT INTO " + table + generateInsertParams(fields);
    }

    public static String generateUpdateSQL(String table, String idField, String[] fields) {
        return "UPDATE " + table + " SET " + generateUpdateParams(fields) + " WHERE " + idField + " = ?";
    }

    private static String generateUpdateParams(String[] fields) {
        return Arrays.stream(fields)
                .map(s -> s + "=?")
                .collect(Collectors.joining(", "));
    }
}
