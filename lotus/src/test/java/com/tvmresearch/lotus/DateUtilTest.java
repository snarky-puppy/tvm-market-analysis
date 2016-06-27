package com.tvmresearch.lotus;

import org.junit.Test;

import java.time.LocalDate;

import static com.tvmresearch.lotus.DateUtil.businessDaysBetween;
import static com.tvmresearch.lotus.DateUtil.daysBetween;
import static com.tvmresearch.lotus.DateUtil.minusBusinessDays;
import static org.junit.Assert.assertEquals;
import static java.time.temporal.ChronoUnit.DAYS;


/**
 * Created by matt on 27/06/16.
 */
public class DateUtilTest {

    @Test
    public void testDaysBetween() {
        LocalDate d1 = LocalDate.of(2016, 1, 1);
        LocalDate d2 = LocalDate.of(2016, 1, 2);
        LocalDate d3 = LocalDate.of(2016, 1, 3);
        LocalDate d4 = LocalDate.of(2016, 1, 4);
        LocalDate d8 = LocalDate.of(2016, 1, 8);
        LocalDate d11 = LocalDate.of(2016, 1, 11);
        LocalDate d12 = LocalDate.of(2016, 1, 12);
        LocalDate d18 = LocalDate.of(2016, 1, 18);

        // test standard functionality
        assertEquals(1, DAYS.between(d1, d2));
        assertEquals(1, DAYS.between(d2, d3));
        assertEquals(1, DAYS.between(d3, d4));

        assertEquals(2, DAYS.between(d1, d3));
        assertEquals(3, DAYS.between(d1, d4));
        assertEquals(4, DAYS.between(d4, d8));
        assertEquals(3, DAYS.between(d8, d11));
        assertEquals(7, DAYS.between(d11, d18));
        assertEquals(17, DAYS.between(d1, d18));

        System.out.println(d1.getDayOfWeek());
        System.out.println(d2.getDayOfWeek());

        // test weekday functionality
        assertEquals(1, businessDaysBetween(d1, d2));
        assertEquals(0, businessDaysBetween(d2, d3));
        assertEquals(0, businessDaysBetween(d3, d4));
        assertEquals(1, businessDaysBetween(d1, d4));
        assertEquals(4, businessDaysBetween(d4, d8));
        assertEquals(1, businessDaysBetween(d8, d11));
        assertEquals(1, businessDaysBetween(d11, d12));
        assertEquals(0, businessDaysBetween(d12, d12));
        assertEquals(5, businessDaysBetween(d11, d18));
        assertEquals(11, businessDaysBetween(d1, d18));

    }

    @Test
    public void testMinusBusinessDays() {
        assertEquals(LocalDate.of(2016, 1, 1), minusBusinessDays(LocalDate.of(2016, 1, 4), 1));
        assertEquals(LocalDate.of(2016, 1, 1), minusBusinessDays(LocalDate.of(2016, 1, 8), 5));
        assertEquals(LocalDate.of(2016, 1, 1), minusBusinessDays(LocalDate.of(2016, 1, 15), 10));
    }

}