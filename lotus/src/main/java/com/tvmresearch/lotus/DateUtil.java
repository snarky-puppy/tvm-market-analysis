package com.tvmresearch.lotus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;


/**
 * Miscellaneous date functions.
 *
 * Almost obselete, now that Java 8 has all the Joda time functions. java.time !!
 *
 * Created by matt on 29/10/14.
 */
public class DateUtil {
    private static final Logger logger = LogManager.getLogger(DateUtil.class);

    public static boolean isFirstOfMonth() {
        return LocalDate.now().getDayOfMonth() == 1;
    }

    public static LocalDate addDays(LocalDate date, int numDays) {
        return date.plusDays(numDays);
    }

    public static LocalDate firstOfThisMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    public static LocalDate firstOfLastMonth() {
        return LocalDate.now().withDayOfMonth(1).minusMonths(1);
    }
}
