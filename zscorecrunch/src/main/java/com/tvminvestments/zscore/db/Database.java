package com.tvminvestments.zscore.db;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.ZScoreEntry;
import com.tvminvestments.zscore.RangeBounds;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Created by horse on 29/01/15.
 */
public interface Database {

    void init(String market) throws Exception;

    RangeBounds findDataBounds(String symbol) throws Exception;

    int findMaxZScoreDataDate(String symbol, int startDate) throws Exception;

    CloseData loadData(String symbol) throws Exception;

    void insertZScores(String symbol, Map<Integer, ZScoreEntry> zscores) throws Exception;

    ZScoreEntry loadZScores(String symbol, int sampleStart) throws Exception;
    Map<Integer,ZScoreEntry> loadZScores(String symbol) throws Exception;

    //double findClosePrice(String symbol, int entryDate) throws Exception;

    Set<String> listSymbols() throws Exception;

    void dropData(String symbol) throws Exception;

    void dropZScore(String symbol) throws Exception;

    void freshImport();


    Object beginInsertDataTransaction();
    void insertData(String symbol, int date, double close, double volume, double open) throws Exception;
    void commitDataTransaction();


    void rewrite(CloseData data);

    void dropAllZScore();

    Path dataFile(String symbol);

    String getMarket();
}
