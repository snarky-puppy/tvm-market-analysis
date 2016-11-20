package com.tvm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Miscellaneous date functions.
 *
 * Created by matt on 29/10/14.
 */
public class DateUtil {
    private static final Logger logger = LogManager.getLogger(DateUtil.class);

    public static void assertValidDate(int date) throws IndexOutOfBoundsException {
        // If this code is still kicking around 6 years from now I will be impressed^H^H^H^H^H^H^Hdissapointed
        if(date < 19500101 || date > 20200101) {
            logger.error("Invalid date: "+date);
            System.out.println("Invalid date: "+date);
            throw new IndexOutOfBoundsException();
        }
    }

    public static int getYear(int date) {
        return date / 10000;
    }

    public static boolean isFirstOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1;
    }

    public static long timestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        return Long.parseLong(sdf.format(new Date()));
    }

    public static int toInteger(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return Integer.parseInt(sdf.format(d));
    }


    private static class DateComponents {
        public int y, m, d;

        @Override
        public String toString() {
            return "DateComponents{" +
                    "y=" + y +
                    ", m=" + m +
                    ", d=" + d +
                    '}';
        }

        DateComponents(int date) {
            assertValidDate(date);
            y = getYear(date);
            m = (date - (y * 10000)) / 100;
            d = (date - (y * 10000)) - (m * 100);
        }
    }

    public static int dateTimeToInteger(DateTime dateTime) {
        return (dateTime.getYear() * 10000) +
                (dateTime.getMonthOfYear() * 100) +
                (dateTime.getDayOfMonth());
    }

    public interface DateOperation {
        int op(int date, int n);
    }

    public static int addYears(int date, int numYears) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.plusYears(numYears));
    }

    public static int minusYears(int date, int numYears) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.minusYears(numYears));
    }

    public static int addDays(int date, int numDays) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.plusDays(numDays));
    }

    public static int minusDays(int date, int numDays) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.minusDays(numDays));
    }

    public static int addWeeks(int date, int numWeeks) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.plusWeeks(numWeeks));
    }

    public static int minusWeeks(int date, int numWeeks) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.minusWeeks(numWeeks));
    }

    public static int addMonths(int date, int numMonths) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.plusMonths(numMonths));
    }

    public static int minusMonths(int date, int numMonths) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dateTimeToInteger(dateTime.minusMonths(numMonths));
    }

    public static int today() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return Integer.parseInt(sdf.format(date));
    }

    public static int firstOfThisMonth() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM01");
        return Integer.parseInt(sdf.format(date));
    }

    public static int firstOfTheMonth(int date) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, dc.m, 1, 0, 0);
        return dateTimeToInteger(dateTime);
    }

    public static int findEndOfYearWeekDate(int date) {
        DateComponents dc = new DateComponents(date);
        DateTime dateTime = new DateTime(dc.y, 12, 31, 0, 0);
        int offset = 0;
        if(dateTime.dayOfWeek().get() > DateTimeConstants.FRIDAY)
            offset = dateTime.dayOfWeek().get() - DateTimeConstants.FRIDAY;
        return dateTimeToInteger(new DateTime(dc.y, 12, 31-offset, 0, 0));
    }

    public static int distance(int start, int end) {
        DateComponents startDateComponents = new DateComponents(start);
        DateComponents endDateComponents = new DateComponents(end);
        DateTime startDateTime = new DateTime(startDateComponents.y, startDateComponents.m, startDateComponents.d, 0, 0);
        DateTime endDateTime = new DateTime(endDateComponents.y, endDateComponents.m, endDateComponents.d, 0, 0);
        Days rv = Days.daysBetween(startDateTime.toLocalDate(), endDateTime.toLocalDate());
        return rv.getDays();
    }

}
