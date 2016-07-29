package com.tvm.crunch;

import com.tvm.crunch.scenario.Scenario;

/**
 * Created by horse on 28/07/2016.
 */
public interface TriggerProcessor {
    void processTrigger(Data data, int idx, double zscore, Scenario scenario);
    int getEntryZScore();
    int getExitZScore();
}
