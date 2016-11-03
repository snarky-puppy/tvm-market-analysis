package com.tvm.crunch.database;

import com.tvm.crunch.Data;
import com.tvm.crunch.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.*;
import java.net.SocketTimeoutException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;


/**
 * Database interface
 * <p>
 * Created by horse on 19/03/2016.
 */
class YahooDatabase implements Database {

    private static final Logger logger = LogManager.getLogger(Database.class);

    static List<ActiveSymbol> symbols;
    static Map<String, Set<String>> marketSymbols;

    static {
        try {
            loadSymbols();
        } catch(IOException e) {
            logger.error(e);
            System.exit(1);
        }
    }

    public YahooDatabase() {
    }

    public void update() throws IOException {
        updateActiveSymbolsTable();
        updateYahooData();
    }

    private void updateActiveSymbolsTable() {
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {

            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("INSERT INTO active_symbols(exchange, symbol, sector)" +
                    " VALUES(?,?,?);");

            for (ActiveSymbol activeSymbol : symbols) {
                try {
                    //System.out.printf("%s:%d %s:%d\n", activeSymbol.exchange, activeSymbol.exchange.length(), activeSymbol.symbol, activeSymbol.symbol.length());

                    stmt.setString(1, activeSymbol.exchange);
                    stmt.setString(2, activeSymbol.symbol);
                    stmt.setString(3, "");

                    stmt.execute();
                } catch (SQLIntegrityConstraintViolationException e) {
                    // do nothing
                }
            }

            connection.commit();

        } catch (SQLException e) {
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

    private void updateYahooData() {

        ExecutorService executorService = Executors.newFixedThreadPool(16);

        for(Row r : getActiveSymbols()) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    updateData(r);
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void updateData(Row row) {

        LocalDate now = LocalDate.now();

        //LocalDate first = getDate(row, true);
        LocalDate last = getDate(row, false);

        //if(first == null)
        //    first = now;
        if(last == null)
            last = now.minusYears(100);

        /*
        LocalDate goalFirst = now.minusYears(years).minusMonths(months);
        if(goalFirst.isBefore(first)) {
            System.out.println("1st: "+row.symbol + ": from " + goalFirst + " to "+ first);
            Calendar from = Calendar.getInstance();
            from.set(goalFirst.getYear(), goalFirst.getMonthValue() - 1, goalFirst.getDayOfMonth());
            Calendar to = Calendar.getInstance();
            to.set(first.getYear(), first.getMonthValue() - 1, first.getDayOfMonth());
            updateData(row, from, to, false);
        }
        */

        if(now.isAfter(last)) {
            System.out.println("2nd: "+row.symbol + ": from " + last + " to "+ now);
            Calendar from = Calendar.getInstance();
            from.set(last.getYear(), last.getMonthValue() - 1, last.getDayOfMonth());
            Calendar to = Calendar.getInstance();
            to.set(now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
            updateData(row, from, to, false);
        }
    }

    private void updateData(Row row, Calendar from, Calendar to, boolean secondRun) {

        if (row.lastCheck != null && LocalDate.now().isEqual(row.lastCheck))
            return;

        try {
            Stock stock = YahooFinance.get(row.symbol.replace('.', '-'), from, to, Interval.DAILY);
            saveData(row, stock);
            updateLastCheck(row);
        } catch (IOException e) {
            e.printStackTrace();
            if (!secondRun) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                updateData(row, from, to, true);
            } else
                updateLastCheck(row);
        }
    }
    private static void loadSymbols() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File("active_symbols.csv")));
        String line;

        symbols = new ArrayList<>();
        marketSymbols = new HashMap<>();
        while((line = br.readLine()) != null) {
            String[] data = line.split("[,\\t]");
            if(data.length == 2) {
                String symbol = data[0];
                String market = data[1];
                symbols.add(new ActiveSymbol(symbol, market));
                Set<String> l = marketSymbols.get(market);
                if(l == null) {
                    l = new HashSet<>();
                    marketSymbols.put(market, l);
                }
                l.add(symbol);
            }
        }
    }

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

    static void updateLastCheck(Row symbol) {
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

    static List<Row> getActiveSymbols() {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<Row> rv = new ArrayList<>();
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

                rv.add(new Row(
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

    static LocalDate getDate(Row activeSymbol, boolean first) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement("SELECT "+(first?"MIN":"MAX")+"(dt) " +
                    " FROM yahoo_data WHERE symbol_id = ?");
            stmt.setInt(1, activeSymbol.id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                Date dt = rs.getDate(1);
                if(dt != null)
                    return dt.toLocalDate();
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return LocalDate.now();
    }

    static void saveData(Row symbol, Stock stock) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {
            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("INSERT INTO yahoo_data VALUES(NULL,?,?,?,?,?);");

            for (HistoricalQuote quote : stock.getHistory()) {

                LocalDate date = quote.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                try {
                    System.out.println(String.format("save: id=%d, date=%s, close=%.2f, vol=%d", symbol.id, date, quote.getAdjClose().doubleValue(), quote.getVolume()));

                    stmt.setInt(1, symbol.id);
                    stmt.setDate(2, Date.valueOf(date));
                    stmt.setDouble(3, quote.getAdjClose().doubleValue());
                    stmt.setDouble(4, quote.getOpen().doubleValue());
                    stmt.setLong(5, quote.getVolume());
                    stmt.execute();
                }catch (SQLIntegrityConstraintViolationException e) {
                    // ignore
                }
            }
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

    @Override
    public Set<String> listSymbols(String market) {
        return marketSymbols.get(market);
    }

    @Override
    public Data loadData(String market, String symbol) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {

            stmt = connection.prepareStatement(
                    "SELECT * FROM yahoo_data" +
                    " WHERE symbol_id = (SELECT id FROM active_symbols WHERE exchange = ? AND symbol = ?)" +
                    " ORDER BY dt");


            stmt.setString(1, market);
            stmt.setString(2, symbol);
            rs = stmt.executeQuery();

            if(rs.last()) {
                int nRows = rs.getRow();
                Data data = new Data(symbol, market, nRows);
                rs.beforeFirst();
                int i = 0;
                while (rs.next()) {
                    data.open[i] = rs.getDouble("open");
                    data.close[i] = rs.getDouble("close");
                    data.volume[i] = rs.getLong("volume");
                    data.date[i] = DateUtil.toInteger(rs.getDate("dt"));
                    i++;
                }
                return data;
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
        return null;

    }

    @Override
    public List<String> listMarkets() {
        return new ArrayList<>(marketSymbols.keySet());
    }

    static class ActiveSymbol {
        final String symbol;
        final String exchange;

        ActiveSymbol(String symbol, String exchange) {
            this.symbol = symbol;
            this.exchange = exchange;
        }
    }

    static class Row {
        final int id;
        final String symbol;
        final String exchange;
        final String sector;
        final LocalDate lastCheck;

        Row(int id, String symbol, String exchange, String sector, LocalDate lastCheck) {
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

    public static void main(String[] args) {
        Row r = new Row(801, "CECO", "NASDAQ", "Consumer", null);
        YahooDatabase db = new YahooDatabase();
        db.updateData(r);
    }
}

