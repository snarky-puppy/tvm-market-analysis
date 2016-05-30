package com.tvmresearch.lotus.db.model;

import java.time.LocalDate;
import java.util.Map;

/**
 * Created by matt on 30/05/16.
 */
public interface InvestmentHistoryDao {

    void populateHistory(Investment investment);

    void serialise(Investment investment);
}
