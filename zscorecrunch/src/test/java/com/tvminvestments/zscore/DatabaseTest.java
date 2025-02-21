package com.tvminvestments.zscore;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertEquals;

public class DatabaseTest {

    private static final String symbol = "DatabaseTest";
    private static Database database;

    @BeforeClass
    public static void init() throws Exception {
        database = DatabaseFactory.createDatabase("test");

        // make sure we are clean
        database.dropData(symbol);
        database.dropZScore(symbol);

        Map<Integer, ZScoreEntry> entries = new HashMap<Integer, ZScoreEntry>();

        for(int dataDate = 1; dataDate < 6; dataDate++) {
            database.insertData(symbol, dataDate, dataDate, 0, 0);
            ZScoreEntry entry = new ZScoreEntry(5);
            for(int startDate = 1; startDate < 6; startDate++) {
                entry.date[startDate-1] = startDate;
                entry.zscore[startDate-1] = dataDate;
            }
            entries.put(dataDate, entry);
        }

        database.commitDataTransaction();

        assertEquals(5, entries.size());

        database.insertZScores(symbol, entries);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        database.dropData(symbol);
        database.dropZScore(symbol);
    }

    @Test
    public void testFindSymbolBounds() throws Exception {
        RangeBounds bounds = database.findDataBounds(symbol);
        assertEquals(1, bounds.getMin());
        assertEquals(5, bounds.getMax());
    }

    @Test
    public void testLoadZScores() throws Exception {
        Map<Integer, ZScoreEntry> zscore = database.loadZScores(symbol);
        assertEquals(5, zscore.keySet().size());
        for(ZScoreEntry entry : zscore.values()) {
            entry.sanity();
            assertEquals(5, entry.date.length);
            // assert list is in order
            int lastEntry = -1;
            for(int date : entry.date) {
                if(lastEntry == -1) {
                    lastEntry = date;
                } else {
                    assertThat(lastEntry, is(lessThan(date)));
                }
            }
        }
    }
}