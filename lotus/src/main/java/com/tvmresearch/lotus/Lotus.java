package com.tvmresearch.lotus;

import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.broker.InteractiveBrokerAPI;
import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        Broker broker = null;

        try {
            broker = new InteractiveBroker();
            Compounder compounder = new Compounder(broker);
            EventProcessor eventProcessor = new EventProcessor(broker, compounder);
            eventProcessor.processTriggers(Trigger.getTodaysTriggers());
            eventProcessor.processUnfilledPositions(broker.getUnfilledPositions());
            eventProcessor.processFilledPositions(broker.getOpenPositions());
        } finally {
            logger.info("The Final final block");
            if(broker != null)
                broker.disconnect();
        }
    }

}
