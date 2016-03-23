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
    public static final double MIN_ZSCORE = -2;
    public static final double MIN_VOLUME = 10000000;
    public static final int RETRIGGER_MIN_DAYS = 28;
    public static final int SPREAD = 5;
    public static final int MIN_INVEST_PC = 10;

}
