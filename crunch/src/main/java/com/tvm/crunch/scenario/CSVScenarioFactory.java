package com.tvm.crunch.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Create scenarios based on file input
 *
 * Created by matt on 24/11/14.
 */
public class CSVScenarioFactory extends AbstractScenarioFactory {

    private static final Logger logger = LogManager.getLogger(CSVScenarioFactory.class);
    private String inputFile = "scenarios.csv";

    private static Set<Scenario> scenarioSet = null;

    public CSVScenarioFactory() {
        ensureInputFileLoaded();
        extractScenarioRanges();
    }

    public CSVScenarioFactory(String inputFile) {
        this.inputFile = inputFile;
    }

    private synchronized void ensureInputFileLoaded() {
        if(scenarioSet != null)
            return;

        String scenarioName = null;
        Set<Scenario> tempSet = new HashSet<Scenario>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;

            while ((line = br.readLine()) != null) {
                String[] tuple = line.split(",");

                if (tuple[0].matches("^S[0-9]+")) {
                    scenarioName = tuple[0];
                } else {
                    if (scenarioName == null) {
                        System.out.println("Invalid scenario file format, scenario column must exist and start with 'S'");
                        System.exit(1);
                    }

                    int subScenario = Integer.parseInt(tuple[0]);
                    int sampleStart = Integer.parseInt(tuple[1]);
                    int trackingStart = Integer.parseInt(tuple[2]);
                    int trackingEnd = Integer.parseInt(tuple[3]);
                    Scenario scenario = new Scenario(scenarioName, subScenario, sampleStart, trackingStart, trackingEnd);
                    tempSet.add(scenario);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        scenarioSet = tempSet;
    }

    @Override
    public Set<Scenario> getScenarios() {
        return scenarioSet;
    }
}
