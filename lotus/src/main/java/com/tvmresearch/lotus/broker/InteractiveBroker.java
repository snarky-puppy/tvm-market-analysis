package com.tvmresearch.lotus.broker;

import com.ib.client.Order;
import com.ib.contracts.StkContract;
import com.ib.controller.*;
import com.tvmresearch.lotus.db.model.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by horse on 27/03/2016.
 */
public class InteractiveBroker implements Broker {

    private static final Logger logger = LogManager.getLogger(InteractiveBroker.class);

    private Integer msgId;
    private Integer orderId;

    private final ConnectionHandler connectionHandler;
    private final ApiController controller;
    private final AccountHandler accountHandler;
    private final HistoricalDataHandler historicalDataHandler;
    private final OrderHandler orderHandler;

    public class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public InteractiveBroker() {
        connectionHandler = new ConnectionHandler();
        accountHandler = new AccountHandler();
        historicalDataHandler = new HistoricalDataHandler();
        orderHandler = new OrderHandler();
        controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

        controller.connect("localhost", 7497, 1);
        connectionHandler.waitForConnection();

        controller.reqAccountUpdates(true, connectionHandler.getAccount(), accountHandler);
        accountHandler.waitForEvent();
    }

    @Override
    public void disconnect() {
        controller.disconnect();
    }

    @Override
    public double getAvailableFunds() {
        return accountHandler.availableFunds;
    }

    @Override
    public void buy(Position position) {
        NewContract contract = position.createNewContract();
        NewOrder order = position.createNewOrder(connectionHandler.getAccount());
        orderHandler.addPosition(position);
        controller.placeOrModifyOrder(contract, order, orderHandler);
    }

    @Override
    public void sell(Position position) {

    }

    @Override
    public boolean checkSellLimit(Position position) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Instant instant = Instant.now();
        String timeLimit = formatter.format(instant);

        controller.reqHistoricalData(position.createNewContract(), timeLimit, 1, Types.DurationUnit.DAY,
                Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);
        historicalDataHandler.waitForEvent();
        logger.info(String.format("%s close: %.2f, limit: %.2f", position.trigger.symbol,
                historicalDataHandler.closePrice, position.buyLimit));

        return false;
    }

    @Override
    public List<Position> getUnfilledPositions() {
        return null;
    }

    @Override
    public List<Position> getOpenPositions() {
        return null;
    }

    @Override
    public void updateUnfilledPosition(Position position) {

    }
}
