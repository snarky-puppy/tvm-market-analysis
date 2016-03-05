package com.tvminvestments.zscore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;

import java.io.File;

import static org.junit.Assert.*;

public class AppTest {
    private static final Logger logger = LogManager.getLogger(AppTest.class);

    /*
    @org.junit.Test
    public void testGetOutFile() throws Exception {

        App app = new App("test");
        String one = app.getOutFile();

        logger.info("one="+one);

        File oneFile = new File(one);

        assertTrue(oneFile.createNewFile());
        String two = app.getOutFile();
        logger.info("two="+two);
        File twoFile = new File(two);
        assertTrue(twoFile.createNewFile());

        assertTrue(two.compareTo(one) > 0);

        assertTrue(twoFile.delete());
        assertTrue(oneFile.delete());


    }
    */
}