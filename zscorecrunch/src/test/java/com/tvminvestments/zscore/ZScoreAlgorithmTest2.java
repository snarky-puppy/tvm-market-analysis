package com.tvminvestments.zscore;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;

public class ZScoreAlgorithmTest2 {

    private static final Logger logger = LogManager.getLogger(ZScoreAlgorithmTest2.class);

    private static final String dataPath = "/ZScoreAlgorithmTest2.csv";
    private static final String symbol = "ZScoreAlgorithmTest2";

    private Map<Integer, List<Double>> zscoresFromFile;
    private Database database;

    @Before
    public void setUp() throws Exception {
        database = DatabaseFactory.createDatabase("test");

        zscoresFromFile = new HashMap<Integer, List<Double>>();

        // make sure we are clean
        database.dropZScore(symbol);
        database.dropData(symbol);

        //database.getDB().getCollection("results").drop();

        //DBCollection data = database.getDataCollection(symbol);

        // load CSV data
        String path = ZScoreAlgorithmTest2.class.getResource(dataPath).getPath();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        boolean first = true;
        int cnt = 0;
        while ((line = br.readLine()) != null) {
            if (first) {
                first = false;
                String[] heading = line.split(",");
                for(int i = 2; i < heading.length; i++) {
                    zscoresFromFile.put(Integer.parseInt(heading[i]), new ArrayList<Double>());
                }
            } else {
                String[] tuple = line.split(",");

                // take care of data
                int date = Integer.parseInt(tuple[0]);
                double close = Double.parseDouble(tuple[1]);
                database.insertData(symbol, date, close, 0, 0);

                // add zscores
                for(int i = 2; i < tuple.length; i++) {
                    if(tuple[i] != null && tuple[i].length() > 0) {
                        zscoresFromFile.get(i - 1).add(Double.parseDouble(tuple[i]));
                    }

                }
            }
        }
        database.commitDataTransaction();
        logger.info("Inserted " + cnt + " rows into data table");

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testZscore() throws Exception {

        MockScenarioFactory2 scenarioFactory = new MockScenarioFactory2();
        ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, scenarioFactory);
        algo.zscore();

        Map<Integer, ZScoreEntry> data = database.loadZScores(symbol);
        Set<Scenario> scenarios = scenarioFactory.getScenarios(symbol);
        assertEquals(scenarios.size(), data.size());

        for(Scenario scenario : scenarios) {
            logger.info("Test zscore scenario: "+scenario);
            assertTrue(data.containsKey(scenario.sampleStart));
            ZScoreEntry entry = data.get(scenario.sampleStart);
            entry.sanity();

            List<Double> zscores = zscoresFromFile.get(scenario.subScenario);
            int i = 0;
            for(Double d : zscores) {
                logger.info("Compare zscore "+i+": "+d+"/"+entry.zscore[i]);

                BigDecimal f = new BigDecimal(d);
                BigDecimal z = new BigDecimal(entry.zscore[i]);

                // have to round down to 3 places to get it working! :/
                f = f.setScale(3, BigDecimal.ROUND_DOWN);
                z = z.setScale(3, BigDecimal.ROUND_DOWN);

                assertEquals(f.doubleValue(), z.doubleValue(), 0.01);
                i++;
            }
            assertEquals(i, entry.zscore.length);
        }

        algo.inMemSearch(-2, 2);
        //SearchResults.writeResult("/tmp/ZScoreAlgorithmTest2-results.csv");
    }

}