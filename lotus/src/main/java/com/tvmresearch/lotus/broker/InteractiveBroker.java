package com.tvmresearch.lotus.broker;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.controller.*;
import com.tvmresearch.lotus.LotusException;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.event.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

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

    private final List<Execution> executions = new ArrayList<>();

    public class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public InteractiveBroker(ArrayBlockingQueue<Event> inputQueue, ArrayBlockingQueue<Event> outputQueue) {
        connectionHandler = new ConnectionHandler();
        accountHandler = new AccountHandler();
        liveOrderHandler = new LiveOrderHandler();


        controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

        controller.connect("localhost", 4002, 1);
        connectionHandler.waitForConnection();

        logger.info("Requesting account updates");
        controller.reqAccountUpdates(true, connectionHandler.getAccount(), accountHandler);
        accountHandler.waitForEvent();

        if(accountHandler.exchangeRate == 0.0 || accountHandler.availableFunds == 0.0) {
            throw new LotusException("Account details were not provided");
        }

        logger.info(String.format("Account: AUD=%.2f rate=%.2f USD=%.2f",
                accountHandler.availableFunds,
                accountHandler.exchangeRate,
                accountHandler.availableFunds / accountHandler.exchangeRate

            ));


        controller.reqLiveOrders(liveOrderHandler);
        liveOrderHandler.waitForEvent();
        //controller.removeLiveOrderHandler(liveOrderHandler);

        //controller.orderStatus();

        //volatile boolean tradeLogComplete = false;

        class Temp {
            public int qty;
            public double avgPrice;
        }


        //Map<String, Temp> execMap = new HashMap<>();

        controller.reqExecutions(new ExecutionFilter(0, connectionHandler.getAccount(), null, null, null, null, null), new ApiController.ITradeReportHandler() {
            private final Logger logger = LogManager.getLogger(InteractiveBroker.class);

            @Override
            public void tradeReport(String tradeKey, NewContract contract, Execution execution) {
                logger.info(String.format("trade_key=%s contract=%s execution=%s", tradeKey, contract, execution));
               /* if(!execMap.containsKey(contract.symbol())) {
                    Temp t = new Temp();
                    //t.avgPrice =

                }
                */
            }

            @Override
            public void tradeReportEnd() {
                logger.info("Trade Report End");
                //tradeLogComplete = true;
            }

            @Override
            public void commissionReport(String tradeKey, CommissionReport commissionReport) {
                logger.info(String.format("trade_key=%s report=%s", tradeKey, commissionReport));
            }
        });



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

        System.exit(1);
*/
    }

    @Override
    public void disconnect() {
        controller.disconnect();
    }


    @Override
    public double getAvailableFunds() {
        return accountHandler.availableFunds / accountHandler.exchangeRate;
    }

    @Override
    public boolean buy(Investment investment) {
        logger.info(String.format("BUY: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.buyLimit, investment.qty));

        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createBuyOrder(connectionHandler.getAccount());

        BuyOrderHandler handler = new BuyOrderHandler(investment);
        controller.placeOrModifyOrder(contract, order, handler);

        handler.waitForEvent();
        controller.removeOrderHandler(handler);

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
                investment.sellLimit, investment.qtyFilled));
        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createSellOrder(connectionHandler.getAccount());

        logger.info(String.format("contract=%s, order=%s", contract, order));

        logger.info("1");
        SellOrderHandler handler = new SellOrderHandler(investment);
        logger.info("2");
        controller.placeOrModifyOrder(contract, order, handler);
        logger.info("3");
        handler.waitForEvent();
        logger.info("4");
        controller.removeOrderHandler(handler);
        logger.info("5");
    }


    @Override
    public List<Position> getOpenPositions() {
        return accountHandler.positions;
    }


    public double getLastClose(Investment investment) {
        NewContract contract = investment.createNewContract();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Instant instant = Instant.now();
        String timeLimit = formatter.format(instant);

        logger.info(String.format("getLastClose: %s/%s TimeLimit=%s", contract.primaryExch(), contract.symbol(), timeLimit));

        HistoricalDataHandler historicalDataHandler = new HistoricalDataHandler();
        controller.reqHistoricalData(contract, timeLimit, 1, Types.DurationUnit.DAY,
                Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);
        historicalDataHandler.waitForEvent();
        //controller.cancelHistoricalData(historicalDataHandler); // request is removed once completed
        return historicalDataHandler.closePrice;
    }


    @Override
    public void updateHistory(InvestmentDao dao, Investment investment) {
        NewContract contract = investment.createNewContract();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Instant instant = Instant.now();
        String timeLimit = formatter.format(instant);

        logger.info(String.format("updateHistory: %s/%s TimeLimit=%s", contract.primaryExch(), contract.symbol(), timeLimit));
        Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();

            class Pair {
                public LocalDate date;
                public double close;
                Pair(LocalDate d, double c) { date = d; close = c; }
            }
            List<Pair> history = new ArrayList<>();

            ApiController.IHistoricalDataHandler historicalDataHandler = new ApiController.IHistoricalDataHandler() {
                @Override
                public void historicalData(Bar bar, boolean hasGaps) {
                    Date dt = new Date(bar.time()*1000);
                    LocalDate date = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    logger.info(String.format("history: %s: %s: %f", investment.trigger.symbol, bar.formattedTime(), bar.close()));
                    if(date.isAfter(investment.trigger.date) || date.isEqual(investment.trigger.date))
                        history.add(new Pair(date, bar.close()));
                }

                @Override
                public void historicalDataEnd() {
                    semaphore.release();
                }
            };

            final long days = ChronoUnit.DAYS.between(investment.trigger.date, LocalDate.now());

            System.out.println(String.format("%s to %s - %d days",
                    investment.buyDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                    days));


            controller.reqHistoricalData(contract, timeLimit, (int) days, Types.DurationUnit.DAY,
                    Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);

            semaphore.acquire();

            for(Pair p : history) {
                dao.addHistory(investment, p.date, p.close);
            }

        } catch (InterruptedException e) {
            throw new LotusException(e);
        }
    }
}
