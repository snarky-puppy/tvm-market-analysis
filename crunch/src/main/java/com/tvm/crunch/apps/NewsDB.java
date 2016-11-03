package com.tvm.crunch.apps;

import com.tvm.crunch.Result;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by horse on 23/08/2016.
 */
public class NewsDB {

    class NewsRow {
        public int date;
        public String symbol;
        public String news;
        public String category;

        public NewsRow(int date, String symbol, String news, String category) {
            this.date = date;
            this.symbol = symbol;
            this.news = news;
            this.category = category;
        }

        public NewsRow() {
        }
    }

    public static void main(String[] args) {
        NewsDB newsDB = new NewsDB();
        newsDB.updateNews();
    }

    List<NewsRow> findNews(int date, String symbol) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<NewsRow> rv = null;
        try {
            connection = getConnection();
            stmt = connection.prepareStatement("SELECT CATEGORY, NEWS FROM NEWS WHERE DATE = ? AND SYMBOL = ?");
            stmt.setInt(1, date);
            stmt.setString(2, symbol);
            rs = stmt.executeQuery();
            while (rs.next()) {
                if(rv == null)
                    rv = new ArrayList<>();
                rv.add(new NewsRow(date, symbol, rs.getString("NEWS"), rs.getString("CATEGORY")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            close(rs);
            close(stmt);
            close(connection);
        }
        return rv;
    }

    void updateNews() {
        try {
            Files.delete(Paths.get("news.db"));
        } catch(NoSuchFileException e) {

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        PreparedStatement stmt = null;
        Connection connection = getConnection();
        createTable(connection);

        File newsDir = new File("/Users/horse/Google Drive/Stuff from Matt/scrapes");
        final File[] files = newsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches(".*\\.zip");
            }
        });

        try {

            connection.setAutoCommit(false);
            int cnt = 0;
            for (File file : files) {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry zipEntry;
                while((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String basename = FilenameUtils.getBaseName(zipEntry.getName());
                    if(basename.indexOf('-') == -1)
                        continue;
                    String category = basename.substring(0, basename.indexOf('-'));
                    List<NewsRow> data = loadNews(zipInputStream);
                    System.out.printf("%s/%s: %d\n", file.getName(), category, data.size());
                    for (NewsRow r : data) {
                        stmt = connection.prepareStatement("INSERT INTO NEWS VALUES(?,?,?,?)");
                        stmt.setInt(1, r.date);
                        stmt.setString(2, r.symbol);
                        stmt.setString(3, category);
                        stmt.setString(4, r.news);
                        try {
                            stmt.execute();
                            stmt.close();
                        } catch (SQLException e) {
                            if (e.getErrorCode() != 19)
                                throw e;
                            //e.printStackTrace();
                            //System.out.println(e.getErrorCode());
                        }

                        if (cnt++ > 10000) {
                            connection.commit();
                            cnt = 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(stmt);
            close(connection);
        }
    }

    private List<NewsRow> loadNews(InputStream inputStream) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");

            List<NewsRow> rv = new ArrayList<>();
            List<String> lines = new BufferedReader(new InputStreamReader(inputStream,
                    StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
            for(String line : lines){
                // "date","SYMBOL","News blah blah"
                String[] fields = line.split("\"");

                //System.out.println(String.format("date=%s, sym=%s, news=%s",
                //        fields[1], fields[3], fields[5]));

                NewsRow newsRow = new NewsRow();
                newsRow.date = Integer.parseInt(sdf2.format(sdf.parse(fields[1])));
                newsRow.symbol = fields[3];
                newsRow.news = fields[5];

                rv.add(newsRow);
            }
            return rv;

        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void createTable(Connection connection) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            String sql = "CREATE TABLE NEWS( " +
                    " DATE     INT      NOT NULL, " +
                    " SYMBOL   CHAR(10) NOT NULL, " +
                    " CATEGORY CHAR(50)         , " +
                    " NEWS     TEXT     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.executeUpdate("CREATE UNIQUE INDEX newsIndex on NEWS(DATE,SYMBOL,NEWS);");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:news.db");
        } catch(SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return connection;
    }

    void close(ResultSet rs) {
        if(rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    void close(Statement stmt) {
        if(stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    void close(Connection connection) {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
 }
