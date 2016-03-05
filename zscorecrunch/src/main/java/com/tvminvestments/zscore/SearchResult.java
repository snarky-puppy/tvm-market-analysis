package com.tvminvestments.zscore;

import java.util.List;

/**
 * Structure to hold search results for a single sub-scenario
 *
 * Created by horse on 26/11/14.
 */
public class SearchResult {
    public String symbol;
    public String scenario;
    public int subScenario;

    public int sampleStartDate;
    public int trackingStartDate;
    public int trackingEndDate;

    public List<EntryExitPair> pairs;

    public String toString() {
        StringBuilder str = new StringBuilder();

        for(EntryExitPair pair : pairs) {
            str.append(symbol); str.append(",");
            str.append(scenario); str.append(",");
            str.append(subScenario); str.append(",");
            str.append(sampleStartDate); str.append(",");
            str.append(trackingStartDate); str.append(",");
            str.append(trackingEndDate); str.append(",");
            str.append(pair.resultCode); str.append(",");
            str.append(pair.entryDate); str.append(",");
            str.append(pair.entryZScore); str.append(",");
            str.append(pair.entryClosePrice); str.append(",");
            str.append(pair.exitDate); str.append(",");
            str.append(pair.exitZScore); str.append(",");
            str.append(pair.exitPrice);

            str.append("\n");
        }

        return str.toString();
    }
}
