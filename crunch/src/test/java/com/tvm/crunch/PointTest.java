package com.tvm.crunch;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Point test
 *
 * Created by horse on 21/07/2016.
 */
public class PointTest {

    @Test
    public void testPoint() {
        Data data = new Data(null, null, 1);

        Point p = new Point(data, 0);
        assertTrue(p.date.equals(0));
        assertTrue(p.close.equals(0.0));

        p.date = 20160721;
        assertTrue(p.date.equals(20160721));
        if(p.date == 20160721) {
            assertTrue(true);
        }
    }
}