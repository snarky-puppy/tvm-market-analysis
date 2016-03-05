package com.tvminvestments.zscore.app;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by horse on 15/02/15.
 */
public class Conf {
    public static final Path DB_DIR = Paths.get("/Users/horse/Projects/filedb");
    public static final Path PD_DIR = Paths.get("/Users/horse/Projects/data");
    public static final String dataDir = "data";
    public static final String zscoreDir = "zscore";
    public static final String indexFile = "index.txt";


    public static final double MIN_ADJ_RATIO = 0.7;
    public static final double MAX_ADJ_RATIO = 1.4;

    // if we adjust more than MAX_ADJUSTMENTS adjustments, we don't do any adjustments
    public static final double MAX_ADJUSTMENTS = 20;
    public static final double MIN_ADJ_VALUE = 0.25;

    public static final double DATA_GAP_DAYS = 30;


    public static List<String> listAllMarkets() {
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


    public static Path getBaseDir(String market) {
        return DB_DIR.resolve(market);
    }

    public static Path getDataDir(String market, String symbol) {
        if(symbol != null && symbol.length() == 0)
            throw new IndexOutOfBoundsException("Symbol must be at least 1 character long");
        Path p = getBaseDir(market).resolve(dataDir);
        /*if(symbol != null) {
            p = p.resolve(String.valueOf(symbol.charAt(0)));
        }*/
        return p;
    }

    public static Path getZScoreDir(String market, String symbol) {
        if(symbol != null && symbol.length() == 0)
            throw new IndexOutOfBoundsException("Symbol must be at least 1 character long");
        Path p = getBaseDir(market).resolve(zscoreDir);
        if(symbol != null) {
            p = p.resolve(String.valueOf(symbol.charAt(0))).resolve(symbol);
            if (!Files.exists(p)) {
                try {
                    Files.createDirectories(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return p;

    }

    public static Path getImportDir(String market) {
        return PD_DIR.resolve(market);
    }
}
