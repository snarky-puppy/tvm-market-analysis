package com.tvmresearch.lotus;

import com.ib.controller.Position;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.broker.OpenOrder;
import com.tvmresearch.lotus.db.model.Investment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Program entry point
 *
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);

    public static void main(String[] args) {

        Lotus lotus = new Lotus();
        lotus.main();
    }

    private void main() {

        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();

        Broker broker = null;

        try {
            broker = new InteractiveBroker();

            // Update our DB with any positions that were filled since the last run
            updatePositions(broker);

            // Match the list of orders from IBKR against what we have in our db
            processOrders(broker);

            Compounder compounder = new Compounder(broker.getAvailableFunds());
            EventProcessor eventProcessor = new EventProcessor(broker, compounder);

            //eventProcessor.processTriggers(Trigger.getTodaysTriggers());

            //eventProcessor.processInvestments(investments);

        } finally {
            logger.info("The Final final block");
            if(broker != null)
                broker.disconnect();
        }
    }

    private void processOrders(Broker broker) {
        List<OpenOrder> openOrders = broker.getOpenOrders();
        List<Investment> investments;
        Map<Long, List<Investment>> contractMap = Investment.loadAll().stream().collect(Collectors.groupingBy(Investment::getConId));

        // Open SELL orders

        // Closed SELL orders

        // Open BUY orders

        // Closed BUY orders


    }

    private void updatePositions(Broker broker) {
        List<Position> positions = broker.getOpenPositions();
        for (Position p : positions) {
            Investment i = Investment.loadAndFill(p);
            if (i != null) {
                i.serialise();
            }
        }
    }

}
