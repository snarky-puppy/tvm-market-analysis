package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement event processing logic
 *
 * Created by matt on 23/03/16.
 */
public class EventProcessor {

    private static final Logger logger = LogManager.getLogger(EventProcessor.class);

    private final Broker broker;
    private final Compounder compounder;

    public EventProcessor(Broker broker, Compounder compounder) {
        this.broker = broker;
        this.compounder = compounder;
    }

    public void processTriggers(Connection connection, List<Trigger> triggerList) {

        List<Trigger> okTriggerList = new ArrayList<>();
        for(Trigger trigger : triggerList) {
            if(validateTrigger(connection, trigger))
                okTriggerList.add(trigger);
        }

    }

    public boolean validateTrigger(Connection connection, Trigger trigger) {

        boolean rv = true;
        if(!trigger.event) {
            trigger.rejectReason = Trigger.RejectReason.NOTEVENT;
            rv = false;
        }

        if(trigger.zscore <= Configuration.MIN_ZSCORE) {
            trigger.rejectReason = Trigger.RejectReason.ZSCORE;
            trigger.rejectData = Configuration.MIN_ZSCORE;
            rv = false;
        }

        if(isHealthCare(trigger)) {
            trigger.rejectReason = Trigger.RejectReason.CATEGORY;
            rv = false;
        }

        if(trigger.avgVolume > Configuration.MIN_VOLUME) {
            trigger.rejectReason = Trigger.RejectReason.VOLUME;
            trigger.rejectData = Configuration.MIN_VOLUME;
            rv = false;
        }

        double nextInvest = compounder.nextInvestmentAmount();
        double pc = nextInvest / trigger.avgVolume;
        if(pc >= 1.0) {
            trigger.rejectReason = Trigger.RejectReason.INVESTAMT;
            trigger.rejectData = nextInvest;
            rv = false;
        }


        if(rv == false) {
            try {
                trigger.serialise(connection);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new LotusException(e);
            }
        }
        return rv;
    }

    private boolean isHealthCare(Trigger trigger) {
        return false;
    }

    private void processEvents() {

    }


}
