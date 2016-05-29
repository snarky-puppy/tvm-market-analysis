package com.tvmresearch.lotus.broker;

import com.ib.client.CommissionReport;

/**
 * Created by horse on 29/05/2016.
 */
public class TradeCommissionReport {
    private final String tradeKey;
    private final CommissionReport commissionReport;

    public TradeCommissionReport(String tradeKey, CommissionReport commissionReport) {
        this.tradeKey = tradeKey;
        this.commissionReport = commissionReport;
    }
}
