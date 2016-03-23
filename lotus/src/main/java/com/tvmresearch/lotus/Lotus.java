package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;

/**
 * Program entry point
 *
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);

    public static void main(String[] args) {

        Connection connection = Database.connection();

        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();

        Broker broker = new InteractiveBrokerAPI();
        Compounder compounder = new Compounder(broker);
        EventProcessor eventProcessor = new EventProcessor(broker, compounder);
        eventProcessor.processTriggers(connection, Trigger.getTodaysTriggers(connection));

        Database.close(connection);

    }

}
