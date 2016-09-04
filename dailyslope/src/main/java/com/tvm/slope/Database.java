package com.tvm.slope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
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

    public static void updateLastCheck(ActiveSymbol symbol) {
        //stock.getHistory().forEach(h -> System.out.println(h));
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement("UPDATE active_symbols SET last_check = ? WHERE id = ?");

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setInt(2, symbol.id);

            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
    }

    static class ActiveSymbol {
        public int id;
        public String symbol;
        public String exchange;
        public String sector;
        public LocalDate lastCheck;

        public ActiveSymbol(int id, String symbol, String exchange, String sector, LocalDate lastCheck) {
            this.id = id;
            this.symbol = symbol;
            this.exchange = exchange;
            this.sector = sector;
            this.lastCheck = lastCheck;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ActiveSymbol{");
            sb.append("id=").append(id);
            sb.append(", symbol='").append(symbol).append('\'');
            sb.append(", exchange='").append(exchange).append('\'');
            sb.append(", sector='").append(sector).append('\'');
            sb.append(", lastCheck=").append(lastCheck);
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
            stmt = connection.prepareStatement("SELECT * FROM active_symbols " +
                    "WHERE last_check < CURDATE()" +
                    "   OR last_check IS NULL");
            rs = stmt.executeQuery();
            while (rs.next()) {
                Date dt = rs.getDate("last_check");
                LocalDate ldt = null;
                if(dt != null)
                    ldt = dt.toLocalDate();

                rv.add(new ActiveSymbol(
                        rs.getInt("id"),
                        rs.getString("symbol"),
                        rs.getString("exchange"),
                        rs.getString("sector"),
                        ldt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return rv;
    }

    public static LocalDate getLastDate(ActiveSymbol activeSymbol) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement("SELECT IFNULL(MAX(dt), DATE_SUB(CURDATE(),INTERVAL 2 MONTH))" +
                    " FROM yahoo_data WHERE symbol_id = ?");
            stmt.setInt(1, activeSymbol.id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDate(1).toLocalDate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return null;
    }

    static class YahooData {
        public double open[] = new double[21];
        public double close[] = new double[21];
        public int volume[] = new int[21];
        public LocalDate date[] = new LocalDate[21];
    }

    public static YahooData getYahooData(ActiveSymbol activeSymbol) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement("SELECT * FROM yahoo_data" +
                    " WHERE symbol_id = ? ORDER BY dt ASC LIMIT 21;");

            stmt.setInt(1, activeSymbol.id);
            rs = stmt.executeQuery();
            YahooData data = new YahooData();
            int i = 0;
            while (rs.next()) {
                data.open[i] = rs.getDouble("open");
                data.close[i] = rs.getDouble("close");
                data.volume[i] = rs.getInt("volume");
                data.date[i] = rs.getDate("dt").toLocalDate();
                i++;
            }
            return data;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return null;
    }

    public static void saveData(ActiveSymbol symbol, Stock stock) {
        //stock.getHistory().forEach(h -> System.out.println(h));
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("INSERT INTO yahoo_data VALUES(NULL,?,?,?,?,?);");

            for (HistoricalQuote quote : stock.getHistory()) {

                LocalDate date = quote.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                stmt.setInt(1, symbol.id);
                stmt.setDate(2, Date.valueOf(date));
                stmt.setDouble(3, quote.getClose().doubleValue());
                stmt.setDouble(4, quote.getOpen().doubleValue());
                stmt.setLong(5, quote.getVolume());
                stmt.execute();
            }
        } catch(SQLIntegrityConstraintViolationException e) {
            // ignore

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
            close(rs, stmt, connection);
        }
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
