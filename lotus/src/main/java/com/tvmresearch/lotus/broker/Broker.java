package com.tvmresearch.lotus.broker;

import com.ib.controller.Position;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;

import java.util.List;

/**
 * Broker interface
 *
 * Created by horse on 23/03/2016.
 */
public interface Broker {

    double getAvailableFunds();

    boolean buy(Investment investment);

    void sell(Investment investment);


    /**
     * Filled positions and now full fledged investments
     *
     * @return list of filled positions
     */
    List<Position> getOpenPositions();


    /**
     * Disconnect from API
     */
    void disconnect();

    double getLastClose(Investment investment);
}
