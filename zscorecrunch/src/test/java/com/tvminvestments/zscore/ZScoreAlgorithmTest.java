package com.tvminvestments.zscore;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import static com.tvminvestments.zscore.ResultCode.ENTRY_EXIT;
import static com.tvminvestments.zscore.ResultCode.ENTRY_NO_EXIT;
import static org.junit.Assert.*;

public class ZScoreAlgorithmTest {

    private static final Logger logger = LogManager.getLogger(ZScoreAlgorithmTest.class);

    private static final String dataPath = "/zscores-a.csv";
    private static final String symbol = "ZScoreAlgorithmTest";

    private ArrayList<Double> zscoresFromFile;
    private Database database;

    @Before
    public void setUp() throws Exception {
        database = DatabaseFactory.createDatabase("test");

        zscoresFromFile = new ArrayList<Double>();

        // make sure we are clean
        database.dropZScore(symbol);
        database.dropData(symbol);

        // load CSV data
        String path = ZScoreAlgorithmTest.class.getResource(dataPath).getPath();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        boolean first = true;
        int cnt = 0;
        while ((line = br.readLine()) != null) {
            if (first) {
                first = false;

            } else {
                String[] tuple = line.split(",");
                int date = Integer.parseInt(tuple[0]);
                double close = Double.parseDouble(tuple[1]);
                if (tuple.length > 2) {
                    double zscore = Double.parseDouble(tuple[2]);
                    zscoresFromFile.add(zscore);
                }
                database.insertData(symbol, date, close, 0, 0);
                cnt ++;
            }
        }
        database.commitDataTransaction();
        logger.info(String.format("Inserted %d rows into data table", cnt));

    }

    @After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void testZscore() throws Exception {
        MockScenarioFactory scenarioFactory = new MockScenarioFactory();
        ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, scenarioFactory);
        algo.zscore();
        Map<Integer, ZScoreEntry> data = database.loadZScores(symbol);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(scenarioFactory.startDate));
        ZScoreEntry entry = data.get(scenarioFactory.startDate);
        entry.sanity();
        //assertEquals(zscoresFromFile.size(), entry.zscore.size());
        for(int i = 0; i < entry.zscore.length; i++) {
            logger.info("Compare zscore "+i+": "+entry.date[i]+": "+zscoresFromFile.get(i)+"/"+entry.zscore[i]);

            BigDecimal f = new BigDecimal(zscoresFromFile.get(i));
            BigDecimal z = new BigDecimal(entry.zscore[i]);

            // have to round down to 3 places to get it working! :/
            f = f.setScale(3, BigDecimal.ROUND_DOWN);
            z = z.setScale(3, BigDecimal.ROUND_DOWN);

            assertEquals(f, z);
        }

        logger.info("*****************************************");
        // now change scenario to include more data...
        scenarioFactory.endDate = 20000417;
        algo.zscore();
        data = database.loadZScores(symbol);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(scenarioFactory.startDate));
        entry = data.get(scenarioFactory.startDate);
        entry.sanity();
        assertEquals(zscoresFromFile.size(), entry.zscore.length);
        for(int i = 0; i < entry.zscore.length; i++) {
            logger.info("Compare zscore: "+entry.date[i]+": "+zscoresFromFile.get(i)+"/"+entry.zscore[i]);

            BigDecimal f = new BigDecimal(zscoresFromFile.get(i));
            BigDecimal z = new BigDecimal(entry.zscore[i]);

            // have to round down to 3 places to get it working! :/
            f = f.setScale(3, BigDecimal.ROUND_DOWN);
            z = z.setScale(3, BigDecimal.ROUND_DOWN);

            assertEquals(f, z);
        }
    }

    @Test
    public void testInitialDuplicateCloseValuesAnomaly() throws Exception {
        double results[] = {1.5, 1.09545, 0.91287};
        String symbol = "DupCloseValues";
        database.dropZScore(symbol);
        database.dropData(symbol);
        MockScenarioFactory scenarioFactory = new MockScenarioFactory();
        

        database.insertData(symbol, scenarioFactory.startDate, 16.12, 0, 0);
        database.insertData(symbol, scenarioFactory.startDate + 1, 16.12, 0, 0);
        database.insertData(symbol, scenarioFactory.startDate + 2, 16.12, 0, 0);
        database.insertData(symbol, scenarioFactory.startDate + 3, 16.37, 0, 0);
        database.insertData(symbol, scenarioFactory.startDate + 4, 16.37, 0, 0);
        database.insertData(symbol, scenarioFactory.startDate + 5, 16.37, 0, 0);
        database.commitDataTransaction();


        ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, scenarioFactory);
        algo.zscore();
        Map<Integer, ZScoreEntry> data = database.loadZScores(symbol);
        assertEquals(1, data.size());
        assertTrue(data.containsKey(scenarioFactory.startDate));
        ZScoreEntry entry = data.get(scenarioFactory.startDate);
        assertEquals(3, entry.zscore.length);
        for(int i = 0; i < entry.zscore.length; i++) {
            logger.info("z="+entry.zscore[i]);
            assertNotEquals(0.0, entry.zscore[i]);

            BigDecimal f = new BigDecimal(results[i]);
            BigDecimal z = new BigDecimal(entry.zscore[i]);

            // have to round down to 3 places to get it working! :/
            f = f.setScale(3, BigDecimal.ROUND_DOWN);
            z = z.setScale(3, BigDecimal.ROUND_DOWN);

            assertEquals(f, z);
        }
    }

    @Test
    public void testZScoreFind() throws Exception {

        /**
         * see doc/testZScoreFind.xlsx
         */

        final String symbol = "TestZScoreFind";
        database.dropZScore(symbol);
        database.dropData(symbol);

        // how much "data" there is
        final int dataRange = 20;

        final int entryLimit = -1;
        final int exitLimit = 1;

        /*
        // the following scenario date ranges are within available data
        s1.1: start=0 size=10 zscores direct increase 0-10 (NE)
        s1.2: start=1 size=10 zscores in pattern #1: -1 0 1 0 -1 0 1 0 ... (EE, end with ENE)
        s1.3: start=2 size=10 zscores in pattern #2: 1 0 -1 0 1 0 -1 0 ... (EE, end with NE)
        s1.4: start=3 size=10 zscores in pattern #3: 0 1 0 -1 0 1 0 -1 ... (EE, end on EE)
        s1.5: start=4 size=10 zscores in pattern #4: 0 -1 0 1 0 -1 0 1 ... (EE, end on ENE)
        s1.6: start=5 size=10 zscores decrease 5-(-5) (ENE)

        // the following scenario date ranges will exceed the available data
        s1.7: start=6 size=15 zscores direct increase 0-15 (NE) (actually 0 - 13)
        s1.8: start=7 size=15 zscores in pattern #1: -1 0 1 0 -1 0 1 0 ...
        s1.9: start=8 size=15 zscores in pattern #2: 1 0 -1 0 1 0 -1 0 ...
        s1.10: start=9 size=15 zscores in pattern #3: 0 1 0 -1 0 1 0 -1 ...
        s1.11: start=10 size=15 zscores in pattern #4: 0 -1 0 1 0 -1 0 1 ...
        s1.12: start=11 size=15 zscores increase (-9)-5 (ENE)

        pattern reference:

idx		p1		p2		p3		p4      sample start
0		-1		1		0		0       s1.1
1		0		0		1		-1      s1.2
2		1		-1		0		0       s1.3
3		0		0		-1		1       s1.4
4		-1		1		0		0       s1.5
5		0		0		1		-1      s1.6
6		1		-1		0		0       s1.7
7		0		0		-1		1       s1.8
8		-1		1		0		0       s1.9
9		0		0		1		-1      s1.10
10		1		-1		0		0       s1.11
11		0		0		-1		1       s1.12
12		-1		1		0		0
13		0		0		1		-1
14		1		-1		0		0
15		0		0		-1		1
16		-1		1		0		0
17		0		0		1		-1
18		1		-1		0		0
19		0		0		-1		1

expected output:

S1  1   NE
S1  2

         */

        int [] pattern1 = {-1, 0, 1, 0};
        int [] pattern2 = {1, 0, -1, 0};
        int [] pattern3 = {0, 1, 0, -1};
        int [] pattern4 = {0, -1, 0, 1};

        Scenario scenario1 = new Scenario("S1", 1, 1, 1, 10); // data[0:9]
        Scenario scenario2 = new Scenario("S1", 2, 2, 2, 11); // p1[0:9], data[1:10]
        Scenario scenario3 = new Scenario("S1", 3, 3, 3, 12); // p2[0:9], data[2:11]
        Scenario scenario4 = new Scenario("S1", 4, 4, 4, 13); // p3[0:9], data[3:12]
        Scenario scenario5 = new Scenario("S1", 5, 5, 5, 14); // p4[0:9], data[4:13]
        Scenario scenario6 = new Scenario("S1", 6, 6, 6, 15); // data[5:14]

        // these unrequited scenarios are 15 days long each, starting from t-4.
        Scenario scenario7 = new Scenario("S1", 7, 7, 7, 21);    // data[4:19]
        Scenario scenario8 = new Scenario("S1", 8, 8, 8, 22);    // p1[0:19], data[4:19]
        Scenario scenario9 = new Scenario("S1", 9, 9, 9, 23);    // p2[0:19], data[5:19]
        Scenario scenario10 = new Scenario("S1", 10, 10, 10, 24);  // p3[0:19], data[6:19]
        Scenario scenario11 = new Scenario("S1", 11, 11, 11, 25); // p4[0:19], data[7:19]
        Scenario scenario12 = new Scenario("S1", 12, 12, 12, 26); // data[4:19]

        // sanity
        assertEquals(scenario1.sampleStart, scenario1.trackingStart);
        assertEquals(scenario2.sampleStart, scenario2.trackingStart);
        assertEquals(scenario3.sampleStart, scenario3.trackingStart);
        assertEquals(scenario4.sampleStart, scenario4.trackingStart);
        assertEquals(scenario5.sampleStart, scenario5.trackingStart);
        assertEquals(scenario6.sampleStart, scenario6.trackingStart);
        assertEquals(scenario7.sampleStart, scenario7.trackingStart);
        assertEquals(scenario8.sampleStart, scenario8.trackingStart);
        assertEquals(scenario9.sampleStart, scenario9.trackingStart);
        assertEquals(scenario10.sampleStart, scenario10.trackingStart);
        assertEquals(scenario11.sampleStart, scenario11.trackingStart);
        assertEquals(scenario12.sampleStart, scenario12.trackingStart);

        // ensure scenarios ranges are as described in the doco
        assertEquals(9, scenario1.trackingEnd - scenario1.sampleStart);
        assertEquals(9, scenario2.trackingEnd - scenario2.sampleStart);
        assertEquals(9, scenario3.trackingEnd - scenario3.sampleStart);
        assertEquals(9, scenario4.trackingEnd - scenario4.sampleStart);
        assertEquals(9, scenario5.trackingEnd - scenario5.sampleStart);
        assertEquals(9, scenario6.trackingEnd - scenario6.sampleStart);

        assertEquals(14, scenario7.trackingEnd - scenario7.sampleStart);
        assertEquals(14, scenario8.trackingEnd - scenario8.sampleStart);
        assertEquals(14, scenario9.trackingEnd - scenario9.sampleStart);
        assertEquals(14, scenario10.trackingEnd - scenario10.sampleStart);
        assertEquals(14, scenario11.trackingEnd - scenario11.sampleStart);
        assertEquals(14, scenario12.trackingEnd - scenario12.sampleStart);



        final Set<Scenario> scenarioSet = new HashSet<Scenario>();
        scenarioSet.add(scenario1);
        scenarioSet.add(scenario2);
        scenarioSet.add(scenario3);
        scenarioSet.add(scenario4);
        scenarioSet.add(scenario5);
        scenarioSet.add(scenario6);
        scenarioSet.add(scenario7);
        scenarioSet.add(scenario8);
        scenarioSet.add(scenario9);
        scenarioSet.add(scenario10);
        scenarioSet.add(scenario11);
        scenarioSet.add(scenario12);

        // k=startDate
        Map<Integer, ZScoreEntry> zscores = new HashMap<Integer, ZScoreEntry>();
        ZScoreEntry list1 = new ZScoreEntry(10);
        ZScoreEntry list2 = new ZScoreEntry(10);
        ZScoreEntry list3 = new ZScoreEntry(10);
        ZScoreEntry list4 = new ZScoreEntry(10);
        ZScoreEntry list5 = new ZScoreEntry(10);
        ZScoreEntry list6 = new ZScoreEntry(10);
        ZScoreEntry list7 = new ZScoreEntry(15);
        ZScoreEntry list8 = new ZScoreEntry(15);
        ZScoreEntry list9 = new ZScoreEntry(15);
        ZScoreEntry list10 = new ZScoreEntry(15);
        ZScoreEntry list11 = new ZScoreEntry(15);
        ZScoreEntry list12 = new ZScoreEntry(15);

        zscores.put(1, list1);
        zscores.put(2, list2);
        zscores.put(3, list3);
        zscores.put(4, list4);
        zscores.put(5, list5);
        zscores.put(6, list6);
        zscores.put(7, list7);
        zscores.put(8, list8);
        zscores.put(9, list9);
        zscores.put(10, list10);
        zscores.put(11, list11);
        zscores.put(12, list12);


        // add a bunch of phony data, find needs to cross reference this to produce results
        // data: close == date
        for(int i = 1; i <= dataRange; i++) {
            database.insertData(symbol, i, i, 0, 0);
        }
        database.commitDataTransaction();

        // s1.1
        for(int i = 1; i <= 10; i++) {
            list1.addZScore(i, i);
        }

        // s1.2 .. s1.5
        for(int i = 0; i < 10; i++) {
            int z1 = pattern1[ i % pattern1.length];
            int z2 = pattern2[ i % pattern2.length];
            int z3 = pattern3[ i % pattern3.length];
            int z4 = pattern4[ i % pattern4.length];
            list2.addZScore(scenario2.sampleStart + i, z1);
            list3.addZScore(scenario3.sampleStart + i, z2);
            list4.addZScore(scenario4.sampleStart + i, z3);
            list5.addZScore(scenario5.sampleStart + i, z4);
        }

        // s1.6
        for(int i = 5, d = scenario6.sampleStart; i > -5; i--, d++) {
            list6.addZScore(d, i);
        }

        logger.info((dataRange - (scenario7.trackingEnd - dataRange)));
        // s1.7
        for(int i = 0, d = scenario7.sampleStart; d <= dataRange; i++, d++) {
            list7.addZScore(d, i);
        }

        // s1.8
        for(int i = 0, d = scenario8.sampleStart; d <= dataRange; i++, d++) {
            list8.addZScore(d, pattern1[ i % pattern1.length ]);
        }

        // s1.9
        for(int i = 0, d = scenario9.sampleStart; d <= dataRange; i++, d++) {
            list9.addZScore(d, pattern2[ i % pattern2.length ]);
        }

        // s1.10
        for(int i = 0, d = scenario10.sampleStart; d <= dataRange; i++, d++) {
            list10.addZScore(d, pattern3[ i % pattern3.length ]);
        }

        // s1.11
        for(int i = 0, d = scenario11.sampleStart; d <= dataRange; i++, d++) {
            list11.addZScore(d, pattern4[ i % pattern4.length ]);
        }

        // s1.12
        for(int i = 0, z = -9, d = scenario12.sampleStart; d <= dataRange; i++, d++, z++) {
            list12.addZScore(d, z);
        }

        // ensure data lengths are as described in the doco
        assertEquals(10, list1.getSize());
        assertEquals(10, list2.getSize());
        assertEquals(10, list3.getSize());
        assertEquals(10, list4.getSize());
        assertEquals(10, list5.getSize());
        assertEquals(10, list6.getSize());

        assertEquals(14, list7.getSize());
        assertEquals(13, list8.getSize());
        assertEquals(12, list9.getSize());
        assertEquals(11, list10.getSize());
        assertEquals(10, list11.getSize());
        assertEquals(9, list12.getSize());



        // insert zscores...
        database.insertZScores(symbol, zscores);

        // do the thing!
        ZScoreAlgorithm algo = new ZScoreAlgorithm(symbol, database, new AbstractScenarioFactory() {
            @Override
            public Set<Scenario> getScenarios(String symbol) throws Exception {
                return scenarioSet;
            }
        });
        algo.inMemSearch(entryLimit, exitLimit);


        // OK now everything's done, we need to read back the generated CSV
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        SearchResults.writeResults(bufferedWriter);
        bufferedWriter.close();

        logger.debug("\n"+stringWriter.toString());
        
        BufferedReader bufferedReader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line;
        
        // k=subscenario, v=list of pairs
        Map<Integer, List<EntryExitPair>> results = new HashMap<Integer, List<EntryExitPair>>();
        
        // line by line
        while((line = bufferedReader.readLine()) != null) {
            logger.debug("line="+line);
            String[] columns = line.split(",");
            
            // TestZScoreFind,S1,2,1,1,11,EE,4,4.0,-1.0,6,6.0,1.0
            //   0,       1,          2,          3,            4,          5,          6,        7,          8,         9,       10,        11,       12
            // sym,scenario,subscenario,sampleStart,trackingStart,trackingEnd, resultCode,entryDate,entryZScore,entryPrice, exitDate,exitZScore,exitPrice

            int subScenario = Integer.parseInt(columns[2]);
            
            List<EntryExitPair> pairs;
            if(results.containsKey(subScenario)) {
                logger.debug("seen sub-scenario "+subScenario);
                pairs = results.get(subScenario);
            } else {
                pairs = new ArrayList<EntryExitPair>();
                logger.debug("adding sub-scenario "+subScenario);
                results.put(subScenario, pairs);
            }
            
            EntryExitPair newPair = new EntryExitPair();
            
            if(columns[6].compareTo("EE") == 0) {
                newPair.resultCode = ENTRY_EXIT;
            } else if(columns[6].compareTo("ENE") == 0) {
                newPair.resultCode = ResultCode.ENTRY_NO_EXIT;
            } else if(columns[6].compareTo("NE") == 0) {
                newPair.resultCode = ResultCode.NO_ENTRY;
            } else {
                throw new Exception("Column 6 fubar: "+columns[6]);
            }
            
            newPair.entryDate = Integer.parseInt(columns[7]);
            newPair.entryZScore = Double.parseDouble(columns[8]);
            newPair.entryClosePrice = Double.parseDouble(columns[9]);
            
            newPair.exitDate = Integer.parseInt(columns[10]);
            newPair.exitZScore = Double.parseDouble(columns[11]);
            newPair.exitPrice = Double.parseDouble(columns[12]);

            pairs.add(newPair);
            
        }
        
        // now THAT's done, we can start checking teh results

        // S1.1 NE
        assertFalse(results.containsKey(1));

        // s1.2 EE, EE, ENE
        assertTrue(results.containsKey(2));
        List<EntryExitPair> pairs = results.get(2);

        assertPair(pairs.get(0), ENTRY_EXIT, 2, 4);
        assertPair(pairs.get(1), ENTRY_EXIT, 6, 8);
        assertPair(pairs.get(2), ENTRY_NO_EXIT, 10, 11);

        // s1.3 EE, EE
        assertTrue(results.containsKey(3));
        pairs = results.get(3);

        assertPair(pairs.get(0), ENTRY_EXIT, 5, 7);
        assertPair(pairs.get(1), ENTRY_EXIT, 9, 11);

        // s1.4 EE, EE
        assertTrue(results.containsKey(4));
        pairs = results.get(4);

        assertPair(pairs.get(0), ENTRY_EXIT, 7, 9);
        assertPair(pairs.get(1), ENTRY_EXIT, 11, 13);

        // s1.5 EE, EE, ENE
        assertTrue(results.containsKey(5));
        pairs = results.get(5);

        assertPair(pairs.get(0), ENTRY_EXIT, 6, 8);
        assertPair(pairs.get(1), ENTRY_EXIT, 10, 12);
        assertPair(pairs.get(2), ENTRY_NO_EXIT, 14, 14);

        // s1.6 ENE
        assertTrue(results.containsKey(6));
        pairs = results.get(6);

        assertPair(pairs.get(0), ENTRY_NO_EXIT, 12, 15);

        // s1.7 NE
        assertFalse(results.containsKey(7));

        // s1.8 EE, EE, EE, ENE
        assertTrue(results.containsKey(8));
        pairs = results.get(8);

        assertPair(pairs.get(0), ENTRY_EXIT, 8, 10);
        assertPair(pairs.get(1), ENTRY_EXIT, 12, 14);
        assertPair(pairs.get(2), ENTRY_EXIT, 16, 18);
        assertPair(pairs.get(3), ENTRY_NO_EXIT, 20, 20);

        // s1.9 EE, EE, ENE
        assertTrue(results.containsKey(9));
        pairs = results.get(9);

        assertPair(pairs.get(0), ENTRY_EXIT, 11, 13);
        assertPair(pairs.get(1), ENTRY_EXIT, 15, 17);
        assertPair(pairs.get(2), ENTRY_NO_EXIT, 19, 20);

        // s1.10 EE, EE
        assertTrue(results.containsKey(10));
        pairs = results.get(10);

        assertPair(pairs.get(0), ENTRY_EXIT, 13, 15);
        assertPair(pairs.get(1), ENTRY_EXIT, 17, 19);

        // s1.11 EE, EE, ENE
        assertTrue(results.containsKey(11));
        pairs = results.get(11);

        assertPair(pairs.get(0), ENTRY_EXIT, 12, 14);
        assertPair(pairs.get(1), ENTRY_EXIT, 16, 18);
        assertPair(pairs.get(2), ENTRY_NO_EXIT, 20, 20);

        // s1.12 ENE
        assertTrue(results.containsKey(12));
        pairs = results.get(12);

        assertPair(pairs.get(0), ENTRY_NO_EXIT, 12, 20);


    }

    private static void assertPair(EntryExitPair pair, ResultCode code, int entryDate, int exitDate) {

        // since price == date
        double entryPrice = entryDate;
        double exitPrice = exitDate;

        logger.debug(String.format("assert: %s: %s/%d/%d", pair.toString(), code, entryDate, exitDate));

        assertEquals(code, pair.resultCode);
        assertEquals(entryDate, pair.entryDate);
        assertEquals(entryPrice, pair.entryClosePrice, 0.3);

        assertEquals(exitDate, pair.exitDate);
        assertEquals(exitPrice, pair.exitPrice, 0.3);
    }
}