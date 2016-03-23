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
        int elapsedDays = trigger.elapsedDays(connection);

        return  trigger.zscore <= Configuration.MIN_ZSCORE
                && isHealthCare(trigger)
                && trigger.avgVolume > Configuration.MIN_VOLUME
                &&
                && (elapsedDays == -1 || elapsedDays > Configuration.RETRIGGER_MIN_DAYS);
    }

    private boolean isHealthCare(Trigger trigger) {
        return false;
    }

    private void processEvents() {

    }


}
