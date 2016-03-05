package com.tvminvestments.zscore;

import org.junit.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class CloseDataTest {

    @org.junit.Test
    public void testFindDateIndex() throws Exception {

        CloseData data = new CloseData("test", 5);
        for (int i = 0; i < 5; i++) {
            data.close[i] = i;
            data.date[i] = i;
        }

        assertEquals(0, data.findDateIndex(0));
        assertEquals(1, data.findDateIndex(1));
        assertEquals(2, data.findDateIndex(2));
        assertEquals(3, data.findDateIndex(3));
        assertEquals(4, data.findDateIndex(4));
        assertEquals(4, data.findDateIndex(5));
        assertEquals(4, data.findDateIndex(6));

        assertEquals(-1, data.findDateIndex(6, false));

    }

    @Test
    public void testFindDateIndex1() throws Exception {

    }
}