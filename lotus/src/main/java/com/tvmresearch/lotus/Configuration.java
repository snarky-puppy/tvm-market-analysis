package com.tvmresearch.lotus;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Static configuration
 *
 * Created by horse on 21/03/2016.
 */
public class Configuration {
    public static final Path INPUT_DIR = Paths.get("/Users", "horse", "Google Drive", "Stuff from Matt", "daily_archive");
    public static final double MIN_ZSCORE = -2.0;
    public static final double MIN_VOLUME = 10000000;
    public static final int RETRIGGER_MIN_DAYS = 28;
    public static final int SPREAD = 21;
    public static final int MIN_INVEST_PC = 7;

    // buy limit is close price + 0.1 %
    public static final double BUY_LIMIT_FACTOR = 1.01;
    public static final double SELL_LIMIT_FACTOR = 1.10;
    public static int SELL_LIMIT_DAYS = 84;
}
