package com.tvmresearch.lotus.broker;

/**
 * An executed order
 *
 * Created by horse on 13/05/2016.
 */
public class Execution {

    enum Type {
        BUY,
        SELL
    }

    public Type type;
    public String symbol;
    public int shares;
    public double price;
    public double commission;

}
