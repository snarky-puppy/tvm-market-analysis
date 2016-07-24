package com.tvm.crunch;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        String[] fields = line.split(",");
        if(fields.length != 8)
            logger.error("parse data error: "+market+"/"+symbol+": not enough fields: "+fields.length);
        d.date[idx] = Integer.parseInt(fields[0]);
        d.open[idx] = Double.parseDouble(fields[1]);
        //d.high[idx] = Double.parseDouble(fields[2]);
        //d.low[idx] = Double.parseDouble(fields[3]);
        d.close[idx] = Double.parseDouble(fields[4]);
        d.volume[idx] = Long.parseLong(fields[5]);
        //d.openInterest[idx] = Double.parseDouble(fields[6]);
    }

    public List<String> readAllLines(Path p) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(p.toFile()));

        try {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    public Data loadData(String market, String symbol) {
        logger.info("Loading data "+market+"/"+symbol);
        try {
            List<String> lines = readAllLines(dataFile(market, symbol));
            Data rv = new Data(lines.size() - 1);
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
