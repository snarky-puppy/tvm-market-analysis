package com.tvmresearch.lotus.db.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

/**
 * Triggers bean
 * <p>
 * Created by horse on 19/03/2016.
 */

public class Trigger {

    private static final Logger logger = LogManager.getLogger(Trigger.class);
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

    public Trigger() {
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trigger{");
        sb.append("id=").append(id);
        sb.append(", exchange='").append(exchange).append('\'');
        sb.append(", symbol='").append(symbol).append('\'');
        sb.append(", date=").append(date);
        sb.append(", price=").append(price);
        sb.append(", zscore=").append(zscore);
        sb.append(", avgVolume=").append(avgVolume);
        sb.append(", avgCost=").append(avgPrice);
        sb.append(", event=").append(event);
        sb.append(", rejectReason=").append(rejectReason);
        sb.append(", rejectData=").append(rejectData);
        sb.append('}');
        return sb.toString();
    }

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

}
