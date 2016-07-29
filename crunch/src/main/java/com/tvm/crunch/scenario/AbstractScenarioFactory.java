package com.tvm.crunch.scenario;

import java.util.*;

/**
 * Created by matt on 18/11/14.
 */
public abstract class AbstractScenarioFactory {

    private Map<Integer, ScenarioSet> reducedScenarios;

    abstract Set<Scenario> getScenarios();

    public Map<Integer, ScenarioSet> getReducedScenarios() {
        return reducedScenarios;
    }

    /**
     * Returns a map k=start_date v=end_date representing all the ranges present in our bundle of scenarios.
     *
     * Bonus: Reduces scenarios with the same starting date.
     */
    protected void extractScenarioRanges() {
        Set<Scenario> scenarios = getScenarios();
        reducedScenarios = new HashMap<Integer, ScenarioSet>();

        for(Scenario s : scenarios) {
            if(reducedScenarios.containsKey(s.sampleStart)) {
                ScenarioSet scenarioSet = reducedScenarios.get(s.sampleStart);
                if(s.trackingEnd > scenarioSet.maxDate)
                    scenarioSet.maxDate = s.trackingEnd;
                scenarioSet.scenarios.add(s);
            } else {
                reducedScenarios.put(s.sampleStart, new ScenarioSet(s));
            }
        }
    }

}
