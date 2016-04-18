package com.tvminvestments.zscore;

import com.tvminvestments.zscore.app.Util;
import com.tvminvestments.zscore.scenario.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by horse on 18/09/15.
 */
public class SearchResults {

    private static final Logger logger = LogManager.getLogger(SearchResults.class);

    public static ArrayList<Result> results = new ArrayList<>();

    public static synchronized void addResult(String market, String symbol, Scenario scenario, EntryExitPair entryExitPair) throws Exception {
        if(entryExitPair.resultCode != ResultCode.NO_ENTRY) {
            logger.info(String.format("add result: %s/%s: %s [%d]", market, symbol, entryExitPair.resultCode, results.size()));
            results.add(new Result(market, symbol, scenario, entryExitPair));
        }
    }

    private static void writeHeader(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("Exchange,Symbol,Scenario ID,Sub-scenario" +
                ",Sample Start Date,Tracking Start Date,Tracking End Date" +
                ",Result Code,Entry Date,Entry ZScore,Entry Close Price,Entry Open Price,Exit Date,Exit ZScore,Exit Close Price" +
                ",Max Price(post entry),Max Price Date,Max Price ZScore" +
                ",EMA 50,EMA 100, EMA 200" +
                ",Entry Prev Date,Entry Prev Price,Max Prev Date,Max Prev Price,");

        for(int i = 100; i > 0; i -= 10) {
            bufferedWriter.write(String.format("%d%% Date, %d%% Price,", i, i));
        }
        for(int i = 1; i <= 12; i++) {
            bufferedWriter.write(String.format("%d Month Exit Date, %d Month Exit Price,", i, i));
        }
        bufferedWriter.write("Two days later date,Two days later price, Two days later 10% Date,Two days later 10% Price,");

        bufferedWriter.write("Entry 1 Month Min Date,Entry 1 Month Min Price,Entry 1 Month Max Date,Entry 1 Month Max Price,");

        for(int i = 5; i <= 30; i+=5) {
            bufferedWriter.write(String.format("%d%% Stop Date, %d%% Stop Price,", i, i));
        }

        bufferedWriter.write("Avg Volume prev 30 days,");
        bufferedWriter.write("Avg Price prev 30 days,");
        bufferedWriter.write("Avg Volume post 30 days,");
        bufferedWriter.write("Avg Price post 30 days,");
        bufferedWriter.write("Total Volume prev 30 days,");
        bufferedWriter.write("Total Price prev 30 days,");
        bufferedWriter.write("Total Volume post 30 days,");
        bufferedWriter.write("Total Price post 30 days,");
        bufferedWriter.write("SLOPE prev 30 days,");
        bufferedWriter.write("SLOPE prev 3 days,");

        bufferedWriter.write("Entry next day open date,");
        bufferedWriter.write("Entry next day open price,");
        bufferedWriter.write("Entry next day close price,");
        bufferedWriter.write("10% date,");
        bufferedWriter.write("10% price,");
        bufferedWriter.write("10% next day date,");
        bufferedWriter.write("10% next day open price,");
        bufferedWriter.write("10% next day close price,");
        bufferedWriter.write("11% date,");
        bufferedWriter.write("11% price,");
        bufferedWriter.write("11% next day date,");
        bufferedWriter.write("11% next day open price,");
        bufferedWriter.write("11% next day close price,");
        bufferedWriter.write("12% date,");
        bufferedWriter.write("12% price,");
        bufferedWriter.write("12% next day date,");
        bufferedWriter.write("12% next day open price,");
        bufferedWriter.write("12% next day close price,");
        bufferedWriter.write("13% date,");
        bufferedWriter.write("13% price,");
        bufferedWriter.write("13% next day date,");
        bufferedWriter.write("13% next day open price,");
        bufferedWriter.write("13% next day close price,");
        bufferedWriter.write("14% date,");
        bufferedWriter.write("14% price,");
        bufferedWriter.write("14% next day date,");
        bufferedWriter.write("14% next day open price,");
        bufferedWriter.write("14% next day close price,");
        bufferedWriter.write("15% date,");
        bufferedWriter.write("15% price,");
        bufferedWriter.write("15% next day date,");
        bufferedWriter.write("15% next day open price,");
        bufferedWriter.write("15% next day close price,");
        bufferedWriter.write("16% date,");
        bufferedWriter.write("16% price,");
        bufferedWriter.write("16% next day date,");
        bufferedWriter.write("16% next day open price,");
        bufferedWriter.write("16% next day close price,");
        bufferedWriter.write("17% date,");
        bufferedWriter.write("17% price,");
        bufferedWriter.write("17% next day date,");
        bufferedWriter.write("17% next day open price,");
        bufferedWriter.write("17% next day close price,");
        bufferedWriter.write("18% date,");
        bufferedWriter.write("18% price,");
        bufferedWriter.write("18% next day date,");
        bufferedWriter.write("18% next day open price,");
        bufferedWriter.write("18% next day close price,");
        bufferedWriter.write("19% date,");
        bufferedWriter.write("19% price,");
        bufferedWriter.write("19% next day date,");
        bufferedWriter.write("19% next day open price,");
        bufferedWriter.write("19% next day close price,");

        bufferedWriter.write("1week date,");
        bufferedWriter.write("1week price,");
        bufferedWriter.write("1week next day date,");
        bufferedWriter.write("1week next day open price,");
        bufferedWriter.write("1week next day close price,");
        bufferedWriter.write("2week date,");
        bufferedWriter.write("2week price,");
        bufferedWriter.write("2week next day date,");
        bufferedWriter.write("2week next day open price,");
        bufferedWriter.write("2week next day close price,");
        bufferedWriter.write("3week date,");
        bufferedWriter.write("3week price,");
        bufferedWriter.write("3week next day date,");
        bufferedWriter.write("3week next day open price,");
        bufferedWriter.write("3week next day close price,");
        bufferedWriter.write("4week date,");
        bufferedWriter.write("4week price,");
        bufferedWriter.write("4week next day date,");
        bufferedWriter.write("4week next day open price,");
        bufferedWriter.write("4week next day close price,");
        bufferedWriter.write("end of year date,");
        bufferedWriter.write("end of year price,");

        bufferedWriter.write("\n");
    }


    private static void writeRestrictedHeader(BufferedWriter bw) throws IOException {
        bw.write("Exchange,Symbol,Scenario ID,Sub-scenario");
        bw.write(",Entry Date");
        bw.write(",Entry Price");
        bw.write(",Entry ZScore");
        bw.write(",Average Volume");
        bw.write(",Average Price");
        bw.write("\n");

    }

    public static void writeResults(String market, String name, boolean useRestrictedOutput) throws IOException {
        Map<String, BufferedWriter> writers = new HashMap<>();
        for(Result r : results) {
            if(!writers.containsKey(r.getScenario().name)) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(Util.getOutFile(market, name, r.getScenario().name)));
                writers.put(r.getScenario().name, bw);
                if(useRestrictedOutput)
                    writeRestrictedHeader(bw);
                else
                    writeHeader(bw);
            }
            BufferedWriter bw = writers.get(r.getScenario().name);
            if(useRestrictedOutput)
                bw.write(r.toRestrictedString());
            else
                bw.write(r.toString());
        }

        for(BufferedWriter bw : writers.values()) {
            bw.close();
        }

        writers.clear();

        if(results.size() > 0)
            results.clear();
    }


    public static void writeResults(BufferedWriter bufferedWriter, boolean useRestrictedOutput) throws IOException {
        if(useRestrictedOutput)
            writeRestrictedHeader(bufferedWriter);
        else
            writeHeader(bufferedWriter);

        for(Result r : results) {
            if(r.isEntry()) {
                if (useRestrictedOutput)
                    bufferedWriter.write(r.toRestrictedString());
                else
                    bufferedWriter.write(r.toString());
            }
        }
        bufferedWriter.close();
        results.clear();
    }
}
