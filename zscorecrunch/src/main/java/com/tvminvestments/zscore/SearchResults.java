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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by horse on 18/09/15.
 */
public class SearchResults implements Runnable {

    private static final Logger logger = LogManager.getLogger(SearchResults.class);
    private final boolean restricted;
    private final String name;
    private final String market;

    ArrayList<Result> results = new ArrayList<>();
    ArrayBlockingQueue<ArrayList<Result>> resultQueue = new ArrayBlockingQueue<ArrayList<Result>>(1024);
    final Object lock = new Object();
    private final int maxResults = 10000;
    private boolean finalised = false;
    Thread queueThread;
    Map<String, BufferedWriter> writers = new HashMap<>();
    public String fileName;
    public int count;

    public SearchResults(String market, String name, boolean restricted) {
        this.market = market;
        this.name = name;
        this.restricted = restricted;
        queueThread = new Thread(this);
        queueThread.start();
    }

    public void addResult(String market, String symbol, Scenario scenario, EntryExitPair entryExitPair) throws Exception {
        if(entryExitPair.resultCode != ResultCode.NO_ENTRY) {
            //logger.info(String.format("add result: %s/%s: %s [%d]", market, symbol, entryExitPair.resultCode, results.size()));
            Result r = new Result(market, symbol, scenario, entryExitPair);
            synchronized (lock) {
                if(results.size() > maxResults) {
                    ArrayList<Result> results2 = results;
                    results = new ArrayList<>();
                    resultQueue.add(results2);
                }
                results.add(r);
            }
        }
    }

    private void writeHeader(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("Exchange,Symbol,Scenario ID,Sub-scenario" +
                ",Sample Start Date,Tracking Start Date,Tracking End Date" +
                ",Entry ZScore,Avg Vol Prev 30, Avg Price Prev 30,");


        for(int i = 1; i <= 10; i ++) {
            bufferedWriter.write(String.format("Next %d%% Open Date, Next %d%% Open Price ,", i, i));
        }
        for(int i = 15; i <= 25; i += 5) {
            bufferedWriter.write(String.format("Next %d%% Open Date, Next %d%% Open Price,", i, i));
        }

        for(int i = 1994; i <= 2015; i++) {
            bufferedWriter.write(String.format("End Yr Price %d,", i));
        }

        for(int i = 0; i <= 14; i++) {
            bufferedWriter.write(String.format("RSI 7 %d days,", i));
        }

        for(int i = 0; i <= 14; i++) {
            bufferedWriter.write(String.format("RSI 14 %d days,", i));
        }

        for(int i = 0; i <= 14; i++) {
            bufferedWriter.write(String.format("Next %d Day Open Date, Next %d Day Open Price,", i, i));
        }


        bufferedWriter.write("\n");
    }


    private void writeRestrictedHeader(BufferedWriter bw) throws IOException {
        bw.write("Exchange,Symbol,Scenario ID,Sub-scenario");
        bw.write(",Entry Date");
        bw.write(",Entry Price");
        bw.write(",Entry ZScore");
        bw.write(",Average Volume");
        bw.write(",Average Price");
        bw.write("\n");
    }

    public void finalise()  {
        resultQueue.add(results);
        logger.error("Finalise with queue: "+resultQueue.size());
        try {
            Thread.sleep(2000);
            finalised = true;
            queueThread.join();

            for(BufferedWriter bw : writers.values()) {
                bw.flush();
                bw.close();
            }

            writers.clear();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.error("Finalised ["+market+"] results: "+count);
    }

    private BufferedWriter getWriter(String scenario) throws IOException {
        if(!writers.containsKey(scenario)) {
            BufferedWriter bw;
            if(restricted) {
                fileName = Util.getDailyOutFile();
                bw = new BufferedWriter(new FileWriter(fileName));
            } else
                bw = new BufferedWriter(new FileWriter(Util.getOutFile(market, name, scenario)));
            writers.put(scenario, bw);
            if(restricted)
                writeRestrictedHeader(bw);
            else
                writeHeader(bw);
        }
        return writers.get(scenario);
    }

    private void writeResults(ArrayList<Result> list) throws IOException {

        for(Result r : list) {
            BufferedWriter bw = getWriter(r.getScenario().name);
            if(restricted)
                bw.write(r.toRestrictedString());
            else
                bw.write(r.toString());
        }

        for(BufferedWriter bw : writers.values()) {
            bw.flush();
        }

        list.clear();
    }

    @Override
    public void run() {
        try {
            ArrayList<Result> arr = null;
            do {
                arr = resultQueue.poll(1000, TimeUnit.MILLISECONDS);
                if(arr != null) {
                    try {
                        count += arr.size();
                        logger.error("Writing results: "+arr.size()+"/"+resultQueue.size());
                        writeResults(arr);
                    } catch (IOException e) {
                        logger.error(e);
                        finalised = true;
                    }
                } else
                    logger.error("Nothing in queue");

            } while(arr != null || finalised == false);

        } catch (InterruptedException e) {

        }
    }


    public int queueSize() {
        return resultQueue.size();
    }
}
