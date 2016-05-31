package com.tvmresearch.lotus.broker;

import com.tvmresearch.lotus.Compounder;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.TriggerDao;

/**
 * Message base class
 *
 * Created by matt on 31/05/16.
 */
public abstract class IBMessage {
    public abstract void process(Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao);
}
