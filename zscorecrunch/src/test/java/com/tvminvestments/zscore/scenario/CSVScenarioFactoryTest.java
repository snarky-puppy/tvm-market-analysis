package com.tvminvestments.zscore.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class CSVScenarioFactoryTest {
    private static final Logger logger = LogManager.getLogger(CSVScenarioFactoryTest.class);

    @Test
    public void testGetScenarios() throws Exception {
        String filePath = CSVScenarioFactoryTest.class.getResource("/scenarios.csv").getPath();
        CSVScenarioFactory factory = new CSVScenarioFactory(filePath);
        Set<Scenario> set = factory.getScenarios("A");

        assertEquals(30, set.size());

        for(Scenario s : set) {
            assertEquals('S', s.name.charAt(0));
            logger.info(s);
        }

    }
}