package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.db.model.Investment;

import java.util.List;

/**
 * Created by horse on 7/04/2016.
 */
public interface InvestmentDao {

    //int getQtyFilledSum(int conid);

    List<Investment> getTradesInProgress(int conid);

    List<Investment> getFilledInvestments();

    void serialise(List<Investment> investments);

    void serialise(Investment investment);
}
