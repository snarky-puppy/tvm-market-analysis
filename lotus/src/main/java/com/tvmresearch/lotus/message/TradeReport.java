package com.tvmresearch.lotus.message;

import com.ib.client.Execution;
import com.ib.controller.NewContract;
import com.tvmresearch.lotus.Lotus;

/**
 * Created by horse on 29/05/2016.
 */
public class TradeReport extends IBMessage {
    public final Execution execution;
    public final String tradeKey;
    public final NewContract contract;

    public TradeReport(String tradeKey, NewContract contract, Execution execution) {
        this.tradeKey = tradeKey;
        this.contract = contract;
        this.execution = execution;
    }

    @Override
    public void process(Lotus lotus) {

        lotus.processTradeReport(tradeKey, contract, execution);
    }
}
