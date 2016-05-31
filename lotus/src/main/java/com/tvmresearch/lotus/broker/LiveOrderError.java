package com.tvmresearch.lotus.broker;

import com.tvmresearch.lotus.Compounder;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.TriggerDao;

/**
 * Created by horse on 29/05/2016.
 */
public class LiveOrderError extends IBMessage  {
    public int orderId;
    public int errorCode;
    public String errorMsg;

    public LiveOrderError(int orderId, int errorCode, String errorMsg) {
        this.orderId = orderId;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public void process(Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao) {

    }
}
