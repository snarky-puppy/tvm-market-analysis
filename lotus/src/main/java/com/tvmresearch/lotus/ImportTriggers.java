package com.tvmresearch.lotus;


import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

/**
 * Import trigger files - "DailyTriggerReport"
 *
 * Created by horse on 21/03/2016.
 */
public class ImportTriggers {

    private static final Logger logger = LogManager.getLogger(ImportTriggers.class);

    private Connection connection = null;

    public ImportTriggers() {

    }

    public void importAll() {
        try {
            connection = Database.connection();
            connection.setAutoCommit(false);
            Files.walk(Configuration.INPUT_DIR)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(this::importFile);
            connection.commit();
        } catch (IOException|SQLException e) {
            throw new LotusException(e);
        } finally {
            if(connection != null)
                Database.close(connection);
        }
    }

    private void importFile(Path file) {

        BufferedReader bufferedReader = null;
        boolean first = true;

        try {
             bufferedReader = new BufferedReader(new FileReader(file.toFile()));
            for (;;) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                if(first)
                    first = false;
                else
                    parseLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LotusException(e);
        } finally {
            try {
                if(bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    private void parseLine(String line) {
        logger.debug(line);
        String fields[] = line.split(",");

        Trigger trigger = new Trigger();
        trigger.exchange = fields[0];
        trigger.symbol = fields[1];
        // fields[2] - scenario id
        // fields[3] - subscenario
        trigger.date = parseDate(fields[4]);
        trigger.price = Double.parseDouble(fields[5]);
        trigger.zscore = Double.parseDouble(fields[6]);
        trigger.avgVolume = Double.parseDouble(fields[7]);
        trigger.avgPrice = Double.parseDouble(fields[8]);

        saveTrigger(trigger);
    }

    private void saveTrigger(Trigger trigger) {
        try {
            trigger.serialise(connection);
        } catch(MySQLIntegrityConstraintViolationException e) {
            // ignore duplicates
        } catch(SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        }
    }

    private Date parseDate(String date) {
        int dt = Integer.parseInt(date);
        int y = dt / 10000;
        int m = (dt - (y * 10000)) / 100;
        int d = (dt - (y * 10000)) - (m * 100);
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, d);
        return c.getTime();
    }


    public static void main(String[] args) {
        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();
    }
}
