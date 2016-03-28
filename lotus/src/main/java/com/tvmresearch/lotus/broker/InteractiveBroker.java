package com.tvmresearch.lotus.broker;

import com.ib.controller.*;
import com.tvmresearch.lotus.db.model.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IB API
 *
 * Created by horse on 27/03/2016.
 */
public class InteractiveBroker implements Broker {

    private static final Logger logger = LogManager.getLogger(InteractiveBroker.class);

    private Integer msgId;
    private Integer orderId;

    private double availableFunds;

    private final ConnectionHandler connectionHandler;
    private final ApiController controller;
    private final AccountHandler accountHandler;

    public class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public InteractiveBroker() {
        connectionHandler = new ConnectionHandler();
        accountHandler = new AccountHandler();
        controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

        controller.connect("localhost", 7497, 1);
        connectionHandler.waitForConnection();

        controller.reqAccountUpdates(true, connectionHandler.getAccount(), accountHandler);
        accountHandler.waitForEvent();
        availableFunds = accountHandler.availableFunds;


        /*
        StockHandler stockHandler = new StockHandler();
        controller.reqContractDetails(new NewContract(new StkContract("GIFI")), new StockHandler());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/




    }

    @Override
    public void disconnect() {
        controller.disconnect();
    }

    @Override
    public double getAvailableFunds() {
        return availableFunds;
    }

    @Override
    public void buy(Position position) {
        logger.info(String.format("BUY: %s/%s lim=%.2f qty=%d", position.trigger.exchange, position.trigger.symbol,
                position.buyLimit, position.qty));
        NewContract contract = position.createNewContract();
        NewOrder order = position.createNewOrder(connectionHandler.getAccount());

        BuyOrderHandler handler = new BuyOrderHandler(position);
        controller.placeOrModifyOrder(contract, order, handler);
        position.orderId = order.orderId();
        handler.waitForEvent();
        controller.removeOrderHandler(handler);
        availableFunds -= position.qtyValue;
    }

    @Override
    public void sell(Position position) {

    }

    @Override
    public boolean checkSellLimit(Position position) {

        double closePrice = getLastClose(position.createNewContract());
        logger.info(String.format("%s close: %.2f, limit: %.2f", position.trigger.symbol,
                closePrice, position.buyLimit));

        return false;
    }

    @Override
    public List<Position> getUnfilledPositions() {
        return new ArrayList<>();
    }

    @Override
    public List<Position> getOpenPositions() {
        return new ArrayList<>();
    }

    @Override
    public void updateUnfilledPosition(Position position) {

    }

    private double getLastClose(NewContract contract) {

        logger.info(String.format("getLastClose: %s/%s", contract.primaryExch(), contract.symbol()));
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Instant instant = Instant.now();
        String timeLimit = formatter.format(instant);
        HistoricalDataHandler historicalDataHandler = new HistoricalDataHandler();
        controller.reqHistoricalData(contract, timeLimit, 1, Types.DurationUnit.DAY,
                Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);
        historicalDataHandler.waitForEvent();
        //controller.cancelHistoricalData(historicalDataHandler);
        return historicalDataHandler.closePrice;
    }
}
