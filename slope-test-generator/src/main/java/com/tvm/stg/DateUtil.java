package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

    public static Date fromInteger(int d) {
        DateComponents dc = new DateComponents(d);
        DateTime dt = new DateTime(dc.y, dc.m, dc.d, 0, 0);
        return dt.toDate();
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


}
