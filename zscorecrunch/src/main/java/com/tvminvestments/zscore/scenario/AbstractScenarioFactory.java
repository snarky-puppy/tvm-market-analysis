package com.tvminvestments.zscore.scenario;

import java.net.UnknownHostException;
import java.util.Set;

/**
 * Created by matt on 18/11/14.
 */
public interface AbstractScenarioFactory {

    Set<Scenario> getScenarios(String symbol) throws Exception;
}
