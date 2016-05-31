package com.tvmresearch.lotus.broker;

import com.ib.client.CommissionReport;
import com.tvmresearch.lotus.Compounder;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.TriggerDao;

/**
 * Created by horse on 29/05/2016.
 */
public class TradeCommissionReport extends IBMessage {
    private final String tradeKey;
    private final CommissionReport commissionReport;

    public TradeCommissionReport(String tradeKey, CommissionReport commissionReport) {
        this.tradeKey = tradeKey;
        this.commissionReport = commissionReport;
    }

    @Override
    public void process(Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao) {

    }
}
