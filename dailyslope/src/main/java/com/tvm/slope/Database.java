package com.tvm.slope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database interface
 * <p>
 * Created by horse on 19/03/2016.
 */
public class Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    public static Connection connection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost/slope?" +
                    "useSSL=false&autoReconnect=true&serverTimezone=Australia/Sydney", "slope", "slope");

        } catch (SQLException /*|IllegalAccessException|InstantiationException|ClassNotFoundException*/ ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    static class ActiveSymbol {
        public String symbol;
        public String exchange;
        public String sector;

        public ActiveSymbol(String symbol, String exchange, String sector) {
            this.symbol = symbol;
            this.exchange = exchange;
            this.sector = sector;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ActiveSymbol{");
            sb.append("symbol='").append(symbol).append('\'');
            sb.append(", exchange='").append(exchange).append('\'');
            sb.append(", sector='").append(sector).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    static List<ActiveSymbol> getActiveSymbols() {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<ActiveSymbol> rv = new ArrayList<>();
        try {
            stmt = connection.prepareStatement("SELECT * FROM active_symbols");
            rs = stmt.executeQuery();
            while (rs.next()) {
                rv.add(new ActiveSymbol(
                        rs.getString("symbol"),
                        rs.getString("exchange"),
                        rs.getString("sector")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return rv;
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
