package com.tvmresearch.lotus.message;

import com.ib.client.CommissionReport;
import com.tvmresearch.lotus.Lotus;

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
    public void process(Lotus lotus) {
        lotus.processTradeCommissionReport(this);
    }
}
