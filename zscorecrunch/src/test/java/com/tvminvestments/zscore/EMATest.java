package com.tvminvestments.zscore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by horse on 25/07/15.
 */
public class EMATest {

    private static final Logger logger = LogManager.getLogger(EMATest.class);

    @Test
    public void testEMA() {
        double[] testData = {0.25, 0.24, 0.27, 0.26, 0.29, 0.34, 0.32, 0.36, 0.32, 0.28, 0.25, 0.24, 0.25};

        EMA ema = new EMA(testData);

        logger.info("test data len="+testData.length);

        try {
            assertEquals(0.36, ema.calculate(13, 12), 3);
        } catch (EMAException e) {
            e.printStackTrace();
            assertFalse(true);
        }

    }


}