package com.tvmresearch.lotus.broker;

import com.ib.client.*;
import com.ib.client.Execution;
import com.ib.controller.NewContract;
import com.tvmresearch.lotus.Compounder;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.TriggerDao;

/**
 * Created by horse on 29/05/2016.
 */
public class TradeReport  extends IBMessage {
    public final Execution execution;
    public final String tradeKey;
    public final NewContract contract;

    public TradeReport(String tradeKey, NewContract contract, Execution execution) {
        this.tradeKey = tradeKey;
        this.contract = contract;
        this.execution = execution;
    }

    @Override
    public void process(Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao) {

    }
}
