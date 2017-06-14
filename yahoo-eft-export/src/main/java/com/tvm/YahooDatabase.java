package com.tvm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.*;
import java.net.SocketTimeoutException;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;


/**
 * Database interface
 * <p>
 * Created by horse on 19/03/2016.
 */
class YahooDatabase {

    private static final Logger logger = LogManager.getLogger(YahooDatabase.class);
    private static final int NTHREADS = 32;

    static int MAX_RETRIES = 5;

    static List<ActiveSymbol> symbols;

    //private final Lock writeMux = new ReentrantLock(true);


    static {
        try {
            loadSymbols();
        } catch(IOException e) {
            logger.error(e);
            System.exit(1);
        }
    }

    public YahooDatabase() throws SQLException {
        System.setProperty("yahoofinance.baseurl.histquotes", "https://ichart.yahoo.com/table.csv");

        connection().prepareStatement("CREATE TABLE IF NOT EXISTS yahoo_data (\n" +
                "    symbol_id INTEGER NOT NULL,\n" +
                "    dt DATE NOT NULL,\n" +
                "    open DOUBLE(18,6) NOT NULL,\n" +
                "    high DOUBLE(18,6) NOT NULL,\n" +
                "    low DOUBLE(18,6) NOT NULL,\n" +
                "    close DOUBLE(18,6) NOT NULL,\n" +
                "    volume bigint NOT NULL,\n" +
                "    openInterest DOUBLE(18,6) NOT NULL\n" +
                ");").execute();
        connection().prepareStatement("CREATE UNIQUE INDEX IF NOT EXISTS yahoo_uniq ON yahoo_data(symbol_id, dt);").execute();

        connection().prepareStatement("CREATE TABLE IF NOT EXISTS active_symbols (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    symbol CHAR(64) NOT NULL,\n" +
                "    sector CHAR(32) NOT NULL,\n" +
                "    last_check DATE\n" +
                ");").execute();
        connection().prepareStatement("CREATE UNIQUE INDEX IF NOT EXISTS mapping_uniq ON active_symbols(symbol);").execute();
    }

    public void update() throws IOException {
        updateActiveSymbolsTable();
        updateYahooData();
    }

    private synchronized void updateActiveSymbolsTable() {
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {

            //writeMux.lock();

            connection.setAutoCommit(false);


            for (ActiveSymbol activeSymbol : symbols) {
                try {
                    //System.out.printf("%s:%d %s:%d\n", activeSymbol.exchange, activeSymbol.exchange.length(), activeSymbol.symbol, activeSymbol.symbol.length());
                    stmt = connection.prepareStatement("INSERT INTO active_symbols(symbol, sector)" +
                            " VALUES(?,?);");
                    stmt.setString(1, activeSymbol.symbol);
                    stmt.setString(2, "");

                    stmt.execute();
                } catch (SQLiteException e) {
                    if(e.getErrorCode() != SQLiteErrorCode.SQLITE_CONSTRAINT.code)
                        throw e;
                }
            }

            connection.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            //writeMux.unlock();
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

        ExecutorService executorService = Executors.newFixedThreadPool(NTHREADS);

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
        System.out.println("Updated Yahoo data");

    }

    public void updateData(Row row) {

        LocalDate now = LocalDate.now();

        // don't update if it was already checked today
        //if (row.lastCheck != null && now.isEqual(row.lastCheck))
        //    return;

        //LocalDate first = getDate(row, true);
        LocalDate last = getDate(row, false);

        //if(first == null)
        //    first = now;
        if(last == null)
            last = now.minusYears(100);

        /*
        // update *behind* the existing data
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
            updateData(row, last, now, 1);
        }
    }

    private Calendar toCal(LocalDate d) {
        Calendar c = Calendar.getInstance();
        c.set(d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth());
        return c;
    }

    private void updateData(Row row, LocalDate from, LocalDate to, int runNum) {

        if(to.equals(from))
            return;

        try {
            //Stock stock = YahooFinance.get(row.symbol.replace('.', '-'), from, to, Interval.DAILY);
            Stock stock = YahooFinance.get(row.symbol, toCal(from), toCal(to), Interval.DAILY);
            List<HistoricalQuote> data = stock.getHistory();

            System.out.println(row.symbol + ": " + row.symbol + ": from " + from + " to " + to + ": " + data.size() + " rows");


            try {
                //writeMux.lock();
                saveData(row, data);
                //updateLastCheck(row);
            } finally {
                //writeMux.unlock();
            }

        } catch (FileNotFoundException e) {
            System.out.println(row.symbol + ": " + row.symbol + ": from " + from + " to " + to + ": not found");
        } catch (IOException e) {
            e.printStackTrace();
            if (runNum <= MAX_RETRIES) {
                try {
                    Thread.sleep(runNum * 5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                updateData(row, from, to, runNum + 1);
            }
        }
    }
    private static void loadSymbols() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File("symbols.csv")));
        String line;

        symbols = new ArrayList<>();
        while((line = br.readLine()) != null) {
            String[] data = line.split("[,\\t]");
            if(data.length >= 1) {
                String symbol = data[0];
                symbols.add(new ActiveSymbol(symbol));
            }
        }
    }

    private static Connection connection() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:yahoo.db");

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

    /*
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
    */

    static List<Row> getActiveSymbols() {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<Row> rv = new ArrayList<>();
        try {
            stmt = connection.prepareStatement("SELECT * FROM active_symbols ");/* +
                    "WHERE last_check < date('now')" +
                    "   OR last_check IS NULL");*/
            rs = stmt.executeQuery();
            while (rs.next()) {
                Date dt = rs.getDate("last_check");
                LocalDate ldt = null;
                if (dt != null)
                    ldt = dt.toLocalDate();

                rv.add(new Row(
                        rs.getInt("id"),
                        rs.getString("symbol"),
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

    static synchronized void saveData(Row symbol, List<HistoricalQuote> history) {
        Connection connection = connection();
        PreparedStatement stmt = null;
        try {
            connection.setAutoCommit(false);
            // symbol_id, date, open, high, low, close, volume, openInterest

            for (HistoricalQuote quote : history) {

                LocalDate date = quote.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                try {
                    stmt = connection.prepareStatement("INSERT INTO yahoo_data VALUES("+generateParams(8)+");");
                    // date, open, high, low, close, volume, openInterest
                    stmt.setInt(1, symbol.id);
                    stmt.setDate(2, Date.valueOf(date));
                    stmt.setDouble(3, quote.getOpen().doubleValue());
                    stmt.setDouble(4, quote.getHigh().doubleValue());
                    stmt.setDouble(5, quote.getLow().doubleValue());
                    stmt.setDouble(6, quote.getAdjClose().doubleValue());
                    stmt.setLong(7, quote.getVolume());
                    stmt.setDouble(8, 0);
                    stmt.execute();
                } catch (SQLiteException e) {
                    // ignore overruns. NB: this us why using SQL db, save us messy manual coding!
                    if(e.getErrorCode() != SQLiteErrorCode.SQLITE_CONSTRAINT.code)
                        throw e;
                }
            }
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


    public void exportFile(String symbol) throws IOException {
        Connection connection = connection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        try {

            stmt = connection.prepareStatement(
                    "SELECT * FROM yahoo_data" +
                    " WHERE symbol_id = (SELECT id FROM active_symbols WHERE symbol = ?)" +
                    " ORDER BY dt");


            stmt.setString(1, symbol);
            rs = stmt.executeQuery();

            if(rs.isBeforeFirst()) {
                FileWriter fw = new FileWriter(new File("data/"+symbol+".csv"), false);
                String hdr = "Date,Open,High,Low,Close,Volume,Open Interest,Ticker\n";
                fw.write(hdr);

                while (rs.next()) {
                    // date, open, high, low, close, volume, openInterest, symbol
                    String line = String.format("%d,%.5f,%.5f,%.5f,%.5f,%d,%.5f,%s\n",
                        Long.parseLong(sdf.format(rs.getDate("dt"))),
                            rs.getDouble("open"),
                            rs.getDouble("high"),
                            rs.getDouble("low"),
                            rs.getDouble("close"),
                            rs.getLong("volume"),
                            rs.getDouble("openInterest"),
                            symbol
                    );
                    fw.write(line);
                }

                fw.flush();
                fw.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs, stmt, connection);
        }
    }


    public void export() throws IOException {
        File dir = new File("data");
        if(!dir.exists())
            dir.mkdir();

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        for(Row r : getActiveSymbols()) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        exportFile(r.symbol);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
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

    static class ActiveSymbol {
        final String symbol;

        ActiveSymbol(String symbol) {
            this.symbol = symbol;
        }
    }

    static class Row {
        final int id;
        final String symbol;
        final String sector;
        //final LocalDate lastCheck;

        Row(int id, String symbol, String sector, LocalDate lastCheck) {
            this.id = id;
            this.symbol = symbol;
            this.sector = sector;
            //this.lastCheck = lastCheck;
        }

        @Override
        public String toString() {
            return "ActiveSymbol{" + "id=" + id +
                    ", symbol='" + symbol + '\'' +
                    ", sector='" + sector + '\'' +
                    //", lastCheck=" + lastCheck +
                    '}';
        }
    }
}

