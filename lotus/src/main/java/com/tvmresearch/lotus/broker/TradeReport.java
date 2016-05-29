package com.tvmresearch.lotus.broker;

import com.ib.client.*;
import com.ib.client.Execution;
import com.ib.controller.NewContract;

/**
 * Created by horse on 29/05/2016.
 */
public class TradeReport {
    public final Execution execution;
    public final String tradeKey;
    public final NewContract contract;

    public TradeReport(String tradeKey, NewContract contract, Execution execution) {
        this.tradeKey = tradeKey;
        this.contract = contract;
        this.execution = execution;
    }
}
