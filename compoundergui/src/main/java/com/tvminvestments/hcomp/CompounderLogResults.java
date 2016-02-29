package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 28/02/2016.
 */
public class CompounderLogResults {
    private static final Logger logger = LogManager.getLogger(CompounderLogResults.class);

    public static List<CompounderLogRow> results = new ArrayList<>();

    public static void reset() {
        results = new ArrayList<>();
    }

    public static synchronized void add(CompounderLogRow r) {
        results.add(r);
    }


    public static void openResults() {

        File f = null;
        try {
            f = File.createTempFile("CompounderGUI", ".csv");

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(CompounderLogRow.header());
            for(CompounderLogRow r : results) {
                bw.write(r.toString());
            }
            bw.flush();
            bw.close();

            f.deleteOnExit();

            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            logger.error("Could not write file", e);
            e.printStackTrace();
        }


    }


}
