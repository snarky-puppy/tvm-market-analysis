package com.tvm;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Functions for dealing with .csv files
 *
 * Created by horse on 21/07/2016.
 */
public class FileDatabase {
    private static final Logger logger = LogManager.getLogger(FileDatabase.class);

    private void parseDataLine(String symbol, Data d, int idx, String line, int fromDate, int toDate) {

    }

    public Data loadData(File file, int fromDate, int toDate) {
        //logger.info("Loading data: "+file.getName());
        try {
            List<String> lines = FileUtils.readLines(file);
            String symbol = file.getName();
            Data rv = new Data(file.getName(), lines.size() - 1);
            int i = 0;
            boolean first = true;
            for(String line : lines) {
                if(first) {
                    first = false;
                } else {

                    StringTokenizer tok = new StringTokenizer(line, ",", false);

                    int date = Integer.parseInt(tok.nextToken());
                    if(date < fromDate || date > toDate)
                        continue;

                    rv.date[i] = date;
                    rv.open[i] = Double.parseDouble(tok.nextToken());
                    rv.high[i] = Double.parseDouble(tok.nextToken());
                    rv.low[i] = Double.parseDouble(tok.nextToken());
                    rv.close[i] = Double.parseDouble(tok.nextToken());
                    rv.volume[i] = Long.parseLong(tok.nextToken());
                    tok.nextToken(); //d.openInterest[idx] = Double.parseDouble(tok.nextToken());
                    tok.nextToken(); // symbol
                    i++;

                    if(tok.hasMoreTokens()) {
                        logger.error("parse data error: " + symbol + ":"+i+": too many fields: " + tok.nextToken());
                    }
                }
            }
            return rv;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Data loadData(File file, ConfigBean bean) {
        int fromDate = DateUtil.toInteger(bean.fromDate);
        int toDate = DateUtil.toInteger(bean.toDate);
        return loadData(file, fromDate, toDate);
    }
}
