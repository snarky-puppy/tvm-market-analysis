package com.tvm.crunch.database;



import com.tvm.crunch.Data;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by horse on 21/07/2016.
 */
public class FileDatabase implements Database {
    private static final Logger logger = LogManager.getLogger(FileDatabase.class);

    public static final Path PD_DIR = Paths.get("/Users/horse/Projects/data");

    public static Path getDataDir(String market) {
        return PD_DIR.resolve(market);
    }

    private static Path dataFile(String market, String symbol) {
        return getDataDir(market).resolve(symbol + ".csv");
    }

    public Set<String> listSymbols(String market) {
        Set<String> rv = new HashSet<>();
        try {
            Files.walk(getDataDir(market), FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(p -> rv.add(p.getFileName().toString().replace(".csv", "")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return rv;
    }

    private void parseDataLine(String market, String symbol, Data d, int idx, String line) {
        StringTokenizer tok = new StringTokenizer(line, ",", false);


        d.date[idx] = Integer.parseInt(tok.nextToken());
        d.open[idx] = Double.parseDouble(tok.nextToken());
        tok.nextToken(); //d.high[idx] = Double.parseDouble(tok.nextToken());
        tok.nextToken(); //d.low[idx] = Double.parseDouble(tok.nextToken());
        d.close[idx] = Double.parseDouble(tok.nextToken());
        d.volume[idx] = Long.parseLong(tok.nextToken());
        tok.nextToken(); //d.openInterest[idx] = Double.parseDouble(tok.nextToken());
        tok.nextToken(); // symbol

        if(tok.hasMoreTokens()) {
            System.out.println("parse data error: " + market + "/" + symbol + ": too many fields: " + tok.nextToken());
            System.exit(1);
        }
    }

    public Data loadData(String market, String symbol) {
        logger.info("Loading data "+market+"/"+symbol);
        try {
            List<String> lines = FileUtils.readLines(dataFile(market, symbol).toFile());
            Data rv = new Data(symbol, market, lines.size() - 1);
            int i = 0;
            boolean first = true;
            for(String line : lines) {
                if(first) {
                    first = false;
                } else {
                    parseDataLine(market, symbol, rv, i, line);
                    i++;
                }
            }
            return rv;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> listMarkets() {
        List<String> rv = new ArrayList<String>();

        try {
            Files.list(PD_DIR)
                    .filter(p -> Files.isDirectory(p))
                    .forEach(p -> rv.add(p.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return rv;
    }

}
