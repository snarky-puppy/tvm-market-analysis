package com.tvminvestments.zscore.scenario;

import com.tvminvestments.zscore.DateUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by horse on 5/09/15.
 */
public class DailyScenarioFactory implements AbstractScenarioFactory {

    private final int trackingDays;

    public DailyScenarioFactory(int trackingDays) {
        this.trackingDays = trackingDays;
    }

    @Override
    public Set<Scenario> getScenarios(String symbol) throws Exception {
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
