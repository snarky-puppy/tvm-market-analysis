package com.tvmresearch.lotus;

import java.time.LocalDate;

/**
 * Created by matt on 27/06/16.
 */
public class HistoricalDataPoint {
    public final double close;
    public final LocalDate date;

    public HistoricalDataPoint(LocalDate date, double close) {
        this.date = date;
        this.close = close;
    }
}
