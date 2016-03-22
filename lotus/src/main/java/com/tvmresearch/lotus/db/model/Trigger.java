package com.tvmresearch.lotus.db.model;


import java.util.Date;

/**
 * Triggers bean
 *
 * Created by horse on 19/03/2016.
 */

public class Trigger {

    public Trigger() {}

    public String exchange;

    public String symbol;

    public Date date;

    public double price;

    public double zscore;

    public double avgVolume;

    public double avgPrice;

    public boolean seen = false;
    public boolean actioned = false;
    public boolean ignored = false;


}
