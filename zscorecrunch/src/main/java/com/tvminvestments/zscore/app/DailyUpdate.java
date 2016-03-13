package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calculate our adjustments on daily data
 *
 * Created by horse on 10/09/15.
 */
public class DailyUpdate {
    private static final Logger logger = LogManager.getLogger(DailyUpdate.class);

    public static final int closeIndex = 4;
    public static final int dateIndex = 0;
    public static final int volumeIndex = 5;
    public static final int openIndex = 1;

    public static void main(String[] args) {
        DailyUpdate update = new DailyUpdate();
        update.updateAll();
    }

    public void updateAll() {
        try {
            Conf.listAllMarkets().forEach(s -> updateMarket(s));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateMarket(String market) {
        logger.info("Market: "+market);
        try {
            Database database = DatabaseFactory.createDatabase(market);
            Map<String, Integer> updates = new HashMap<>();

            Set<String> symbols = new HashSet<>();

            Files.walk(Conf.getImportDir(market), FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(p -> symbols.add(p.getFileName().toString().replace(".csv", "")));

            int cnt = 0;
            for(String sym : symbols) {
                if(sym.startsWith("^"))
                    continue;
                logger.info(market+"/"+sym);
                updateData(database, market, sym, updates);
                if(cnt++ >= 100) {
                    database.commitDataTransaction();
                    cnt = 0;
                }
            }
            database.commitDataTransaction();

            Adjustment adjustment = new Adjustment();
            for(String symbol : updates.keySet()) {
                adjustment.scanSymbol(database, symbol, updates.get(symbol));
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void updateData(Database database, String market, String symbol, Map<String, Integer> updates) throws Exception {

        String importPath = String.format("%s/%s/%s.csv", Conf.PD_DIR, market, symbol);
        ReversedLinesFileReader importReader = new ReversedLinesFileReader(new File(importPath));
        String lastImportLine = importReader.readLine();
        if(lastImportLine == null) {
            logger.error("updateData: ["+market+"/"+symbol+"] import file empty");
            return;
        }
        String liArr[] = lastImportLine.split(",");
        // new current date
        int importDate = Integer.parseInt(liArr[dateIndex]);


        // existing current date
        int dataDate = -1;
        File f = new File(database.dataFile(symbol).toString());
        if(f.exists()) {
            ReversedLinesFileReader dataReader = new ReversedLinesFileReader(f);
            String dataLine = dataReader.readLine();
            if (dataLine != null) {
                String arr[] = dataLine.split(",");
                dataDate = Integer.parseInt(arr[0]);
            }
            dataReader.close();
        }

        if(dataDate > importDate) {
            logger.error("updateData: ["+market+"/"+symbol+"] dataDate > importDate (" + dataDate +" > " + importDate +")");
            return;
        }

        if(importDate > dataDate) {
            logger.error("updateData: ["+market+"/"+symbol+"] importing new range: " + dataDate +"-" + importDate +")");

            updates.put(symbol, dataDate);

            // 1. read import data backwards while importDate > dataDate

            while(importDate > dataDate) {

                double close = Double.parseDouble(liArr[closeIndex]);
                double volume = Double.parseDouble(liArr[volumeIndex]);
                double open = Double.parseDouble(liArr[openIndex]);
                database.insertData(symbol, importDate, close, volume, open);

                lastImportLine = importReader.readLine();
                if(lastImportLine == null)
                    break;
                liArr = lastImportLine.split(",");
                if(liArr[dateIndex].compareTo("Date") == 0)
                    break;
                importDate = Integer.parseInt(liArr[dateIndex]);
            }

            // 2. Adjustment on the fresh data range
        }

        importReader.close();
    }
}
