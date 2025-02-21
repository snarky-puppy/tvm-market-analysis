package com.tvm.stg;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Functions for dealing with .csv files
 *
 * Created by horse on 21/07/2016.
 */
public class FileDatabase {
    private static final Logger logger = LogManager.getLogger(FileDatabase.class);

    private void parseDataLine(String symbol, Data d, int idx, String line) {
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
            logger.error("parse data error: " + symbol + ":"+idx+": too many fields: " + tok.nextToken());
        }
    }

    public Data loadData(File file) {
        //logger.info("Loading data: "+file.getName());
        try {
            List<String> lines = FileUtils.readLines(file);
            String symbol = file.getName();
            Data rv = new Data(file.getName().replace(".csv", ""), lines.size() - 1);
            int i = 0;
            boolean first = true;
            for(String line : lines) {
                if(first) {
                    first = false;
                } else {
                    parseDataLine(symbol, rv, i, line);
                    i++;
                }
            }
            return rv;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
