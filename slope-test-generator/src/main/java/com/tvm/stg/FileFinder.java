package com.tvm.stg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by horse on 11/11/16.
 */
public class FileFinder {

    private static final Logger logger = LogManager.getLogger(FileFinder.class);

    static Path baseDir = null;
    static Map<String, Path> files;
    static final Object monitor = new Object();
    static Thread runner;

    public static void setBaseDir(Path _baseDir) {
        baseDir = _baseDir;
        update();
    }

    public static void setSymbols(List<String> _symbols) {
        if(files != null)
            files.clear();

        files = new HashMap<>();

        // convert into map straight away so our lookups are optimised in #findFiles()
        for(String s : _symbols) {
            files.put(s, null);
        }
        update();
    }

    public static Map<String, Path> getFiles() {
        while(runner != null) {
            logger.info("Waiting for FindFiles to complete");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    private static void update() {
        synchronized (monitor) {
            if(runner != null) {
                runner.interrupt();
            }
            if(baseDir != null && files != null && files.keySet().size() > 0) {
                runner = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        findFiles();
                    }
                });
                logger.info("Starting FindFiles negotiations");
                runner.start();
            }
        }
    }

    private static void findFiles() {
        boolean bloodshed = false;
        final int[] cnt = {0};
        try {
            Files.walk(baseDir, FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(p -> {
                        String symbol = p.getFileName().toString().replace(".csv", "");
                        if(files.containsKey(symbol)) {
                            if(files.get(symbol) != null)
                                logger.warn("Already found file "+symbol+
                                        "("+files.get(symbol)+"). Now it gets a new value: "+p);
                            files.put(symbol, p);
                            cnt[0]++;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("FindFiles ended with bloodshed", e);
            bloodshed = true;
        } finally {
            runner = null;
        }
        if(!bloodshed)
            logger.info("FindFiles ended peacefully with "+ cnt[0] +" of "+files.size()+" files found");
    }
}