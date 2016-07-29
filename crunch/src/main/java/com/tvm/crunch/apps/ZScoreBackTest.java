package com.tvm.crunch.apps;

import com.sun.org.apache.xml.internal.resolver.readers.SAXCatalogParser;
import com.tvm.crunch.*;
import com.tvm.crunch.database.FileDatabaseFactory;
import com.tvm.crunch.scenario.AbstractScenarioFactory;
import com.tvm.crunch.scenario.CSVScenarioFactory;
import com.tvm.crunch.scenario.Scenario;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * ZScore strategy back test
 *
 * Created by horse on 28/07/2016.
 */
public class ZScoreBackTest extends MarketExecutor implements TriggerProcessor {

    static final AbstractScenarioFactory scenarioFactory = new CSVScenarioFactory();

    @Override
    public int getEntryZScore() {
        return -2;
    }

    @Override
    public int getExitZScore() {
        return 2;
    }

    public static void main(String[] args) {

        boolean visualvm = false;

        Util.waitForKeypress(visualvm);

        if(false) {
            ZScoreBackTest trendContBackTest = new ZScoreBackTest("test");
            trendContBackTest.execute();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), ZScoreBackTest::new);
        }

        Util.waitForKeypress(visualvm);
    }

    private ZScoreBackTest(String market) {
        super(market);
    }

    @Override
    protected ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue) {
        return new ResultWriter(queue) {
            @Override
            protected String getProjectName() {
                return "ZScore";
            }

            @Override
            protected String getMarket() {
                return market;
            }
        };
    }

    @Override
    protected void processSymbol(String symbol) {
        Data data = db().loadData(market, symbol);
        data.zscore(scenarioFactory, this);
    }

    @Override
    public void processTrigger(Data data, int idx, double zscore, Scenario scenario) {
        Design10Result r = Design10Result.create(data, idx, zscore, scenario);
        enqueueResult(r);
    }
}
