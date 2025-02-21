package com.tvminvestments.zscore;

import junit.framework.TestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class DateUtilTest extends TestCase {

    @Test
    public void testAssertValidDate() throws Exception {
        boolean exception = false;
        try {
            DateUtil.assertValidDate(20141010);
            exception = false;
        } catch(IndexOutOfBoundsException e) {
            exception = true;
        }
        assertFalse(exception);

        try {
            DateUtil.assertValidDate(19691231);
            exception = false;
        } catch(IndexOutOfBoundsException e) {
            exception = true;
        }
        assertTrue(exception);

        try {
            DateUtil.assertValidDate(20200102);
            exception = false;
        } catch(IndexOutOfBoundsException e) {
            exception = true;
        }
        assertTrue(exception);
    }

    @Test
    public void testAddYears() throws Exception {
        int date = 19990101;
        assertEquals(20000101, DateUtil.addYears(date, 1));
        assertEquals(20010101, DateUtil.addYears(date, 2));
        assertEquals(20020101, DateUtil.addYears(date, 3));
    }

    @Test
    public void testSubtractYears() throws Exception {
        int date = 20200101;
        assertEquals(20190101, DateUtil.minusYears(date, 1));
        assertEquals(20180101, DateUtil.minusYears(date, 2));
        assertEquals(20170101, DateUtil.minusYears(date, 3));
    }

    @Test
    public void testGetYear() {
        assertEquals(1999, DateUtil.getYear(19990101));
        assertEquals(1978, DateUtil.getYear(19780213));
        assertEquals(1984, DateUtil.getYear(19840112));
        assertEquals(2020, DateUtil.getYear(20201231));
    }
}