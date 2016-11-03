package com.tvm.crunch.scenario;


import com.tvm.crunch.DateUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by horse on 5/09/15.
 */
public class DailyBackTestScenarioFactory extends AbstractScenarioFactory {

    private final int trackingDays;

    public DailyBackTestScenarioFactory(int trackingDays) {
        this.trackingDays = trackingDays;
    }

    @Override
    public Set<Scenario> getScenarios() {
        int sampleStart = DateUtil.firstOfTheMonth(getTodaysDate());
        sampleStart = DateUtil.minusMonths(sampleStart, 1);

        // Premium Data feed is delayed by a day or so, reflect this in the scenario
        int today = getTodaysDate();
        int trackingStart = DateUtil.minusDays(today, trackingDays);

        Scenario s = new Scenario("Daily", 0, sampleStart, trackingStart, today);
        Set<Scenario> rv = new HashSet<>();
        rv.add(s);
        return rv;
    }

    protected int getTodaysDate() {
        return DateUtil.today();
    }
}
