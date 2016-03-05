package com.tvminvestments.zscore;

import com.tvminvestments.zscore.scenario.AbstractScenarioFactory;
import com.tvminvestments.zscore.scenario.Scenario;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by matt on 18/11/14.
 */
public class MockScenarioFactory2 implements AbstractScenarioFactory {


    public int endDate = 20140623;

    @Override
    public Set<Scenario> getScenarios(String symbol) throws UnknownHostException {
        Set<Scenario> rv = new HashSet<Scenario>();
        rv.add(new Scenario("T1", 1, 19991118, 19991118, endDate));
        rv.add(new Scenario("T1", 2, 20001118, 20001118, endDate));
        rv.add(new Scenario("T1", 3, 20011118, 20011118, endDate));
        rv.add(new Scenario("T1", 4, 20021118, 20021118, endDate));
        rv.add(new Scenario("T1", 5, 20031118, 20031118, endDate));
        rv.add(new Scenario("T1", 6, 20041118, 20041118, endDate));
        return rv;
    }
}
