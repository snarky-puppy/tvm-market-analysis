package com.tvminvestments.zscore;


import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.Scenario;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by matt on 18/11/14.
 */
public class MockScenarioFactory implements AbstractScenarioFactory {

    public int startDate = 19991118;
    public int endDate = 20000101;

    @Override
    public Set<Scenario> getScenarios(String symbol) throws UnknownHostException {
        Set<Scenario> rv = new HashSet<Scenario>();
        rv.add(new Scenario("T1", 0, startDate, startDate, endDate));
        return rv;
    }
}
