package com.tvmresearch.lotus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by matt on 27/06/16.
 */
public class DateUtil {

    // http://stackoverflow.com/questions/25798876/count-days-between-two-dates-with-java-8-while-ignoring-certain-days-of-week
    static long daysBetween(LocalDate start, LocalDate end, List<DayOfWeek> ignore) {
        return Stream.iterate(start, d -> d.plusDays(1))
                .limit(start.until(end, ChronoUnit.DAYS))
                .filter(d->!ignore.contains(d.getDayOfWeek()))
                .count();
    }

    public static long businessDaysBetween(LocalDate start, LocalDate end) {
        return daysBetween(start, end, Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    public static LocalDate minusBusinessDays(LocalDate end, int numDays) {
        while(numDays > 0) {
            end = end.minusDays(1);
            if(end.getDayOfWeek() != DayOfWeek.SATURDAY && end.getDayOfWeek() != DayOfWeek.SUNDAY)
                numDays --;
        }
        return end;
    }
}
