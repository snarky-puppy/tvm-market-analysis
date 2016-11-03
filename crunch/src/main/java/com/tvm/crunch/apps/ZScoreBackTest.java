package com.tvm.crunch.apps;

import com.tvm.crunch.*;
import com.tvm.crunch.database.DatabaseFactory;
import com.tvm.crunch.database.FileDatabaseFactory;
import com.tvm.crunch.scenario.AbstractScenarioFactory;
import com.tvm.crunch.scenario.DailyBackTestScenarioFactory;
import com.tvm.crunch.scenario.Scenario;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * ZScore strategy back test
 *
 * Created by horse on 28/07/2016.
 */
public class ZScoreBackTest extends MarketExecutor implements TriggerProcessor {

    static final AbstractScenarioFactory scenarioFactory = new DailyBackTestScenarioFactory(2);

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
            ZScoreBackTest trendContBackTest = new ZScoreBackTest("test", new FileDatabaseFactory());
            trendContBackTest.executeAllSymbols();
        } else {
            executeAllMarkets(new FileDatabaseFactory(), ZScoreBackTest::new);
        }

        Util.waitForKeypress(visualvm);
    }

    private ZScoreBackTest(String market, DatabaseFactory databaseFactory) {
        super(market, databaseFactory);
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
