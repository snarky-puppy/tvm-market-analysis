package com.tvmresearch.lotus.broker;

import com.ib.controller.*;
import com.tvmresearch.lotus.LotusException;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
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

    private final ConnectionHandler connectionHandler;
    private final ApiController controller;
    private final AccountHandler accountHandler;
    private final LiveOrderHandler liveOrderHandler;


    public class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public InteractiveBroker() {
        connectionHandler = new ConnectionHandler();
        accountHandler = new AccountHandler();
        liveOrderHandler = new LiveOrderHandler();

        controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

        controller.connect("localhost", 7497, 1);
        connectionHandler.waitForConnection();

        logger.info("Requesting account updates");
        controller.reqAccountUpdates(true, connectionHandler.getAccount(), accountHandler);
        accountHandler.waitForEvent();

        if(accountHandler.exchangeRate == 0.0 || accountHandler.availableFunds == 0.0) {
            throw new LotusException("Account details were not provided");
        }


        controller.reqLiveOrders(liveOrderHandler);
        liveOrderHandler.waitForEvent();
        //controller.removeLiveOrderHandler(liveOrderHandler);



        /*
        StockHandler stockHandler = new StockHandler();
        controller.reqContractDetails(new NewContract(new StkContract("GIFI")), new StockHandler());
        */

        /*
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

    }

    @Override
    public void disconnect() {
        controller.disconnect();
    }


    @Override
    public double getAvailableFunds() {
        return accountHandler.availableFunds / conversionRate();
    }

    @Override
    public boolean buy(Investment investment) {
        logger.info(String.format("BUY: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.buyLimit, investment.qty));

        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createNewOrder(connectionHandler.getAccount());

        BuyOrderHandler handler = new BuyOrderHandler(investment);
        controller.placeOrModifyOrder(contract, order, handler);

        handler.waitForEvent();
        controller.removeOrderHandler(handler);


        //investment.permId = n++;
        //investment.conId = m++;

        //logger.info(contract);

        boolean landed;
        do {
            synchronized (liveOrderHandler.orderIdToContractIdMap) {
                landed = liveOrderHandler.orderIdToContractIdMap.containsKey(order.orderId());
            }
            if(!landed) {
                try { Thread.sleep(100); } catch (InterruptedException e) {   }
            }
        } while(!landed);

        investment.conId = liveOrderHandler.orderIdToContractIdMap.get(order.orderId());

        if(investment.errorCode != null)
            return false;
        else
            return true;
    }

    @Override
    public void sell(Investment investment) {
        logger.info(String.format("SELL: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.buyLimit, investment.qty));
    }

    /*
    @Override
    public boolean checkSellLimit(Investment investment) {

        double closePrice = getLastClose(investment.createNewContract());
        logger.info(String.format("%s close: %.2f, limit: %.2f", investment.trigger.symbol,
                closePrice, investment.buyLimit));

        return false;
    }

    @Override
    public List<Investment> getUnfilledPositions() {
        return new ArrayList<>();
    }
    */

    @Override
    public List<Position> getOpenPositions() {
        return accountHandler.positions;
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

    private double conversionRate() {
        return accountHandler.exchangeRate;
    }
}
