package com.tvm.crunch;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by horse on 21/07/2016.
 */
public class PointTest {

    @Test
    public void testPoint() {
        Data data = new Data(1);

        Point p = new Point(data, 0);
        assertNull(p.date);
        assertNull(p.close);

        p.date = 20160721;
        assertTrue(p.date.equals(20160721));
        if(p.date == 20160721) {
            assertTrue(true);
        }
    }
}