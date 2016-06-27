package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.HistoricalDataPoint;

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



    Investment findUnconfirmed(String symbol);

    Investment findOrder(int orderId);

    Investment findConId(int conid);

    Map<LocalDate, Double> getHistory(Investment investment);

    int getHistoricalMissingDays(Investment investment);

    double getLastHistoricalClose(Investment investment);

    int outstandingBuyOrders();

    void addHistory(Investment investment, List<HistoricalDataPoint> history);
}
