package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;

/**
 * Investment aka BUY order
 *
 * Created by horse on 24/03/2016.
 */
public class Investment {

    private final Trigger trigger;

    public Investment(Trigger trigger) {
        this.trigger = trigger;
    }
}
