package com.tvminvestments.zscore.app;

import com.tvminvestments.zscore.DateUtil;

import java.io.File;

/**
 * Created by horse on 21/02/15.
 */
public class Util {

    public static String OUTPUT_DIR = "/Users/horse/Google Drive/Stuff from Matt";

    public static String getOutFile(String market, String name) {
        return getOutFile(market, name, "NOSCENARIO");
    }

    public static String getOutFile(String market, String name, String scenario) {
        int rev = 1;
        String outFilePath;

        File file;
        do {
            outFilePath = String.format("%s/%s-%s[%s]-%d-%d.csv", OUTPUT_DIR, name, market, scenario, DateUtil.today(), rev);
            file = new File(outFilePath);
            rev++;
        } while(file.isFile());

        return outFilePath;
    }

    public static String getDailyOutFile() {
        int rev = 1;
        String outFilePath;

        File file;
        do {
            outFilePath = String.format("%s/daily_archive/DailyTriggerReport-%d-%d.csv", OUTPUT_DIR, DateUtil.today(), rev);
            file = new File(outFilePath);
            rev++;
        } while(file.isFile());

        return outFilePath;
    }


}
