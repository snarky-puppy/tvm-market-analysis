package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by horse on 4/06/15.
 */
public class Ticker {

    private static final Logger logger = LogManager.getLogger(Ticker.class);

    private final Database database;
    private int totalSymbols;
    private int symbolsProcessed;

    private final Object lock = new Object();

    public Ticker(Database database) {
        this.database = database;
        resetTicker();
    }

    public void resetTicker() {
        try {
            totalSymbols = database.listSymbols().size();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        symbolsProcessed = 0;
    }

    public void incrementTicker() {
        synchronized (lock) {
            symbolsProcessed++;
            logger.info(String.format("progress: %f%%", ((float) symbolsProcessed / (float) totalSymbols) * 100));
        }
    }

}
