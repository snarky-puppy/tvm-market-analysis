package com.tvmresearch.lotus;


import com.tvmresearch.lotus.db.model.Trigger;
import com.tvmresearch.lotus.db.model.TriggerDao;
import com.tvmresearch.lotus.db.model.TriggerDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Import trigger files - "DailyTriggerReport"
 *
 * Created by horse on 21/03/2016.
 */
public class ImportTriggers {

    private static final Logger logger = LogManager.getLogger(ImportTriggers.class);

    public ImportTriggers() {
    }

    public void importAll() {
        try {
            TriggerDao dao = new TriggerDaoImpl();
            Files.walk(Configuration.INPUT_DIR)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .map(this::importFile)
                    .forEach(dao::serialise);
        } catch (IOException e) {
            throw new LotusException(e);
        }
    }

    private List<Trigger> importFile(Path file) {

        List<Trigger> rv = new ArrayList<>();
        BufferedReader bufferedReader = null;
        boolean first = true;

        logger.info("importFile: "+file);

        try {
             bufferedReader = new BufferedReader(new FileReader(file.toFile()));
            for (;;) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                if(first)
                    first = false;
                else {
                    Trigger trigger = parseLine(line);
                    if(trigger != null)
                        rv.add(trigger);
                }
            }
            backupFile(file);
            return rv;

        } catch (IOException e) {
            throw new LotusException(e);
        } finally {
            try {
                if(bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    private void backupFile(Path file) throws IOException {
        Path newFile = Configuration.INPUT_DIR_ARCHIVE.resolve(file.getFileName());
        Files.move(file, newFile);
    }

    private Trigger parseLine(String line) {
        //logger.debug(line);
        String fields[] = line.split(",");

        Trigger trigger = new Trigger();
        trigger.exchange = fields[0];

        // XXX:
        if(trigger.exchange.compareTo("ASX") == 0)
            return null;

        trigger.symbol = fields[1];
        // fields[2] - scenario id
        // fields[3] - subscenario
        trigger.date = parseDate(fields[4]);
        trigger.price = Double.parseDouble(fields[5]);
        trigger.zscore = Double.parseDouble(fields[6]);
        trigger.avgVolume = Double.parseDouble(fields[7]);
        trigger.avgPrice = Double.parseDouble(fields[8]);

        return trigger;

    }

    private LocalDate parseDate(String date) {
        int dt = Integer.parseInt(date);
        int y = dt / 10000;
        int m = (dt - (y * 10000)) / 100;
        int d = (dt - (y * 10000)) - (m * 100);
        return LocalDate.of(y, m, d);
    }


    public static void main(String[] args) {
        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();
    }
}
