package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;

/**
 * Created by matt on 24/03/16.
 */
public class Position {
    public Trigger trigger;

    public double limit;
    public int qty;
    public double minInvest;
    public double compoundAmount;
    public double totalInvest;
    public int qtyFilled = 0;
    public boolean submitted = false;
    public boolean eod = false;
    public boolean closed = false;

    public Position(Trigger trigger) {
        this.trigger = trigger;
    }

}
