package com.tvmresearch.lotus;

import jdk.nashorn.internal.runtime.regexp.joni.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Import trigger files - "DailyTriggerReport"
 *
 * Created by horse on 21/03/2016.
 */
public class ImportTriggers {

    public void importAll() {
        try {
            Files.walk(Configuration.INPUT_DIR)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(this::importFile);
        } catch (IOException e) {
            throw new LotusException(e);
        }
    }

    private void importFile(Path file) {

    }

    public static void main(String[] args) {
        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();
    }
}
