package com.tvminvestments.zscore.scenario;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by horse on 22/03/2016.
 */
public class DailyScenarioFactoryTest {

    private class DailyScenarioTestFactory extends DailyScenarioFactory {

        public int today = 0;

        public DailyScenarioTestFactory(int trackingDays) {
            super(trackingDays);
        }

        @Override
        protected int getTodaysDate() {
            return today;
        }
    }


    @Test
    public void testDailyScenarioFactory() {
        final int trackingDays = 2;
        DailyScenarioTestFactory factory = new DailyScenarioTestFactory(trackingDays);

        try {
            factory.today = 20160229;
            validateScenario(factory.getScenarios("TEST"), 20160101, 20160227, 20160229);

            factory.today = 20160301;
            validateScenario(factory.getScenarios("TEST"), 20160201, 20160228, 20160301);

        } catch(Exception e) {
            fail();
        }
    }

    private void validateScenario(Set<Scenario> scenarios, int sampleStart, int trackingStart, int trackingEnd) {
        assertEquals(1, scenarios.size());
        Scenario scenario = scenarios.iterator().next();
        assertEquals(sampleStart, scenario.sampleStart);
        assertEquals(trackingStart, scenario.trackingStart);
        assertEquals(trackingEnd, scenario.trackingEnd);
    }

}