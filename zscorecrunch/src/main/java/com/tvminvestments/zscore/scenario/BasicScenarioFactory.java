package com.tvminvestments.zscore.scenario;

import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.RangeBounds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by matt on 20/11/14.
 */
public class BasicScenarioFactory implements AbstractScenarioFactory {

    private static final Logger logger = LogManager.getLogger(BasicScenarioFactory.class);
    private final Database database;

    public BasicScenarioFactory(Database database) {
        this.database = database;

    }

    @Override
    public Set<Scenario> getScenarios(String symbol) throws Exception {
        int maxDate;
        int minDate;
        Set<Scenario> rv = new HashSet<Scenario>();

        RangeBounds bounds = database.findDataBounds(symbol);
        if(bounds == null) {
            logger.error("Not found in database: "+symbol);
            return null;
        }

        minDate = bounds.getMin();
        maxDate = bounds.getMax();

        logger.info(symbol+": min/max: "+bounds.toString());

        rv.add(new Scenario("ALL", 0, minDate, minDate, maxDate));

        return rv;

    }
}
