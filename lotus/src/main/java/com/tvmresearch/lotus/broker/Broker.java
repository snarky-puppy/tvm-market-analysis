package com.tvmresearch.lotus.broker;

import com.tvmresearch.lotus.db.model.Position;

import java.util.List;

/**
 * Broker interface
 *
 * Created by horse on 23/03/2016.
 */
public interface Broker {

    double getAvailableFunds();

    void buy(Position position);

    void sell(Position position);



    // returns true if the close price of position breached the sell limit
    boolean checkSellLimit(Position position);

    /**
     * Unfilled positions according to the broker
     *
     * @return list of unfilled positions
     */
    List<Position> getUnfilledPositions();

    /**
     * Filled positions and now full fledged investments
     *
     * @return list of filled positions
     */
    List<Position> getOpenPositions();

    /**
     * Update Position.qtyFilled
     *
     * @param position
     */
    void updateUnfilledPosition(Position position);

    /**
     * Disconnect from API
     */
    void disconnect();
}
