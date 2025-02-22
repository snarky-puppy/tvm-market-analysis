package com.tvm.crunch.scenario;


import com.tvm.crunch.DateUtil;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Create a set of scenarios to back test daily triggers
 *
 * Created by horse on 5/09/15.
 */
public class DailyBackTestScenarioFactory extends AbstractScenarioFactory {

    private int yearsFromToday;
    Set<Scenario> scenarios;

    public DailyBackTestScenarioFactory(int yearsFromToday) {
        this.yearsFromToday = yearsFromToday;
        scenarios = generateScenarios();
        extractScenarioRanges();
    }

    @Override
    public Set<Scenario> getScenarios() {
        return scenarios;
    }

    private Set<Scenario> generateScenarios() {
        Set<Scenario> rv = new HashSet<>();

        final int today = DateUtil.today();
        int start = DateUtil.minusYears(today, yearsFromToday);

        while(start <= today) {

            int sampleStart = DateUtil.firstOfTheMonth(start);
            sampleStart = DateUtil.minusMonths(sampleStart, 1);

            // Premium Data feed is delayed by a day or so, reflect this in the scenario
            int trackingStart = DateUtil.minusDays(start, 2);

            Scenario s = new Scenario("Daily", 0, sampleStart, trackingStart, start);

            rv.add(s);

            start = DateUtil.addDays(start, 1);
        }
        return rv;
    }
}
