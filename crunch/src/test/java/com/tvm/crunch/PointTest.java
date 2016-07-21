package com.tvm.crunch;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by horse on 21/07/2016.
 */
public class PointTest {

    @Test
    public void testPoint() {
        Point p = new Point();
        assertNull(p.date);
        assertNull(p.price);

        p.date = 20160721;
        assertTrue(p.date.equals(20160721));
        if(p.date == 20160721) {
            assertTrue(true);
        }
    }
}