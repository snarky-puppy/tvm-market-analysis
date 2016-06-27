package com.tvmresearch.lotus.db.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Created by horse on 7/04/2016.
 */
public interface InvestmentDao {


    List<Investment> getPositions();

    void serialise(List<Investment> investments);

    void serialise(Investment investment);

    void addHistory(Investment investment, LocalDate date, double close);

    Investment findUnconfirmed(String symbol);

    Investment findOrder(int orderId);

    Investment findConId(int conid);

    Map<LocalDate, Double> getHistory(Investment investment);

    int getHistoricalMissingDays(Investment investment);

    double getLastHistoricalClose(Investment investment);

    int outstandingBuyOrders();
}
