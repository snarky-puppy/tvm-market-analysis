package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;

import java.sql.Connection;

/**
 * Implement event processing logic
 *
 * Created by matt on 23/03/16.
 */
public class EventProcessor {

    public void processTriggers() {

    }

    public boolean validateTrigger(Connection connection, Trigger trigger) {
        return  trigger.event
                && trigger.zscore <= Configuration.MIN_ZSCORE
                && isHealthCare(trigger)
                && trigger.avgVolume > Configuration.MIN_VOLUME;


    }

    private boolean isHealthCare(Trigger trigger) {
        return false;
    }

    private void processEvents() {

    }


}
