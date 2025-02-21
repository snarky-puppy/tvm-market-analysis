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
import java.util.List;

/**
 * Database interface
 * <p>
 * Created by horse on 19/03/2016.
 */
class Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    private static Connection connection() {
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
        // never executes
        throw new RuntimeException();
    }

    static void updateLastCheck(ActiveSymbol symbol) {
        //stock.getHistory().forEach(h -> System.out.println(h));
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("UPDATE active_symbols SET last_check = ? WHERE id = ?");

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setInt(2, symbol.id);

            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(null, stmt, connection);
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
                if (dt != null)
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

    static LocalDate getLastDate(ActiveSymbol activeSymbol) {
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
        return LocalDate.now();
    }

    static YahooData getYahooData(ActiveSymbol activeSymbol) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement("SELECT * FROM (" +
                    "SELECT * FROM yahoo_data" +
                    " WHERE symbol_id = ? ORDER BY dt DESC LIMIT 21) AS t" +
                    " ORDER BY t.dt ASC;");

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

    static void saveData(ActiveSymbol symbol, Stock stock) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {
            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("INSERT INTO yahoo_data VALUES(NULL,?,?,?,?,?);");

            for (HistoricalQuote quote : stock.getHistory()) {

                LocalDate date = quote.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                System.out.println(String.format("save: id=%d, date=%s, close=%.2f", symbol.id, date, quote.getAdjClose().doubleValue()));

                stmt.setInt(1, symbol.id);
                stmt.setDate(2, Date.valueOf(date));
                stmt.setDouble(3, quote.getAdjClose().doubleValue());
                stmt.setDouble(4, quote.getOpen().doubleValue());
                stmt.setLong(5, quote.getVolume());
                stmt.execute();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
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
            close(null, stmt, connection);
        }
    }

    private static String generateParams(int nParams) {
        StringBuilder builder = new StringBuilder();
        while (nParams > 0) {
            builder.append("?");
            nParams--;
            if (nParams > 0)
                builder.append(',');
        }
        return builder.toString();
    }

    private static void close(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Closing Connection", e);
            e.printStackTrace();
        }
    }

    private static void close(PreparedStatement stmt) {
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException e) {
            logger.error("Closing PreparedStatement", e);
            e.printStackTrace();
        }

    }

    private static void close(ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            logger.error("Closing ResultSet", e);
            e.printStackTrace();
        }
    }

    private static void close(ResultSet rs, PreparedStatement stmt, Connection connection) {
        close(rs);
        close(stmt);
        close(connection);
    }

    static class ActiveSymbol {
        final int id;
        final String symbol;
        final String exchange;
        final String sector;
        final LocalDate lastCheck;

        ActiveSymbol(int id, String symbol, String exchange, String sector, LocalDate lastCheck) {
            this.id = id;
            this.symbol = symbol;
            this.exchange = exchange;
            this.sector = sector;
            this.lastCheck = lastCheck;
        }

        @Override
        public String toString() {
            return "ActiveSymbol{" + "id=" + id +
                    ", symbol='" + symbol + '\'' +
                    ", exchange='" + exchange + '\'' +
                    ", sector='" + sector + '\'' +
                    ", lastCheck=" + lastCheck +
                    '}';
        }
    }

    static class YahooData {
        final double[] open = new double[21];
        final double[] close = new double[21];
        final int[] volume = new int[21];
        final LocalDate[] date = new LocalDate[21];
    }
}
