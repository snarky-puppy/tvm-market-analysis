package com.tvm.crunch.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by horse on 28/07/2016.
 */
public class ScenarioSet {
    public int startDate;
    public int maxDate;
    public List<Scenario> scenarios;


    ScenarioSet(Scenario s) {
        startDate = s.sampleStart;
        maxDate = s.trackingEnd;
        scenarios = new ArrayList<>();
        scenarios.add(s);
    }

}
