package com.tvmresearch.lotus.db.model;


import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.tvmresearch.lotus.Configuration;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Triggers bean
 *
 * Created by horse on 19/03/2016.
 */

public class Trigger {

    private static final Logger logger = LogManager.getLogger(Trigger.class);

    public Trigger() {}

    public Integer id = null;

    public String exchange;

    public String symbol;

    public LocalDate date;

    public double price;

    public double zscore;

    public double avgVolume;

    public double avgPrice;

    public boolean event = false;

    public RejectReason rejectReason = RejectReason.NOTPROCESSED;
    public Double rejectData = null;


    public enum RejectReason {
        NOTEVENT,
        NOTPROCESSED,
        ZSCORE,
        CATEGORY,
        VOLUME,
        INVESTAMT,
        NOFUNDS,
        OK
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "id=" + id +
                ", exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                ", date=" + date +
                ", price=" + price +
                ", zscore=" + zscore +
                ", avgVolume=" + avgVolume +
                ", avgPrice=" + avgPrice +
                ", event=" + event +
                ", rejectReason=" + rejectReason +
                ", rejectData=" + rejectData +
                '}';
    }
}
