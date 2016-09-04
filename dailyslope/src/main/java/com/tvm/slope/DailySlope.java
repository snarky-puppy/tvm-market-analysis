package com.tvm.slope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Created by horse on 4/09/2016.
 */
public class DailySlope {

    private static final Logger logger = LogManager.getLogger(DailySlope.class);

    public static void main(String[] args) {
        DailySlope dailySlope = new DailySlope();
        dailySlope.run();
    }

    private void run() {
        List<Database.ActiveSymbol> activeSymbols = Database.getActiveSymbols();
        activeSymbols.forEach(x -> System.out.println(x));

    }
}
