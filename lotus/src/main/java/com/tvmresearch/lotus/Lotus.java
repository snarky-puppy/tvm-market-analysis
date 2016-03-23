package com.tvmresearch.lotus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;

/**
 * Program entry point
 *
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);

    public static void main(String[] args) {

        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();

        Broker broker = new InteractiveBrokerAPI();

        Compounder compounder = new Compounder(broker);


        EventProcessor eventProcessor = new EventProcessor();
        eventProcessor.processTriggers();

    }

    private boolean isFirstOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1;
    }
}
