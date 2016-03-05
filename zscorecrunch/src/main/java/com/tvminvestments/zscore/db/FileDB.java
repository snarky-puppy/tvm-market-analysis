package com.tvminvestments.zscore.db;


import com.tvminvestments.zscore.*;
import com.tvminvestments.zscore.db.file.FileDBImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Created by horse on 6/02/15.
 */
public class FileDB implements Database {

    private static final Logger logger = LogManager.getLogger(FileDB.class);

    FileDBImpl impl;

    @Override
    public void init(String market) throws Exception {
        impl = new FileDBImpl(market);
    }

    @Override
    public RangeBounds findDataBounds(String symbol) throws Exception {
        return impl.findDataBounds(symbol);
    }

    @Override
    public int findMaxZScoreDataDate(String symbol, int startDate) throws Exception {
        return impl.findMaxZScoreDate(symbol, startDate);
    }

    @Override
    public CloseData loadData(String symbol) throws Exception {
        return impl.loadData(symbol);
    }

    @Override
    public void insertZScores(String symbol, Map<Integer, ZScoreEntry> zscores) throws Exception {
        impl.insertZScores(symbol, zscores);
    }

    @Override
    public ZScoreEntry loadZScores(String symbol, int sampleStart) throws Exception {
        return impl.loadZScores(symbol, sampleStart);
    }

    @Override
    public Map<Integer, ZScoreEntry> loadZScores(String symbol) throws Exception {
        return impl.loadZScores(symbol);
    }

    @Override
    public Set<String> listSymbols() throws Exception {
        return impl.listSymbols();
    }

    @Override
    public void dropData(String symbol) throws Exception {
        impl.dropDataFile(symbol);
    }

    @Override
    public void dropZScore(String symbol) throws Exception {
        impl.dropZScoreFile(symbol);
    }

    @Override
    public void freshImport() {
        impl.freshImport();
    }

    @Override
    public Object beginInsertDataTransaction() {
        impl.closeAllReads();
        return null;
    }

    @Override
    public Path dataFile(String symbol) { return impl.dataFile(symbol); }

    @Override
    public String getMarket() {
        return impl.market;
    }

    @Override
    public void insertData(String symbol, int date, double close, double volume, double open) throws Exception {
        impl.insertData(symbol, date, close, volume, open);
    }

    @Override
    public void commitDataTransaction() {
        impl.closeAllWrites();
    }

    @Override
    public void rewrite(CloseData data) {
        impl.rewrite(data);
    }



    @Override
    public void dropAllZScore() {
        impl.dropAllZScore();
    }
}
