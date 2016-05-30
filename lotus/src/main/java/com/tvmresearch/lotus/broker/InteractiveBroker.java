package com.tvmresearch.lotus.broker;

import com.ib.client.*;
import com.ib.client.Execution;
import com.ib.controller.*;
import com.tvmresearch.lotus.LotusException;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * IB API
 *
 * Created by horse on 27/03/2016.
 */
public class InteractiveBroker implements Broker {

    private static final Logger logger = LogManager.getLogger(InteractiveBroker.class);

    private final ApiController controller;
    private final BlockingQueue<Object> outputQueue;

    // k=symbol
    private Map<String, Position> positions = new HashMap<>();

    private String account;
    private double availableFunds = 0.0;
    private double exchangeRate = 0.0;


    private class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }


    public InteractiveBroker(BlockingQueue<Object> _outputQueue) throws InterruptedException {
        this.outputQueue = _outputQueue;

        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        controller = new ApiController(new ApiController.IConnectionHandler() {

            @Override
            public void connected() {
                //semaphore.release();
                logger.info("Connected");
            }

            @Override
            public void disconnected() {

            }

            @Override
            public void accountList(ArrayList<String> list) {
                if(account == null) {
                    account = list.get(0);
                    logger.info("Got account: "+account);
                    semaphore.release();
                }
            }

            @Override
            public void error(Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
            }

            @Override
            public void message(int id, int errorCode, String errorMsg) {
                // 399: Warning: your order will not be placed at the exchange until 2016-03-28 09:30:00 US/Eastern
                if(errorCode != 399)
                    logger.error(String.format("message: id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
                if(errorCode < 1100 && errorCode != 399 && errorCode != 202) {
                    System.exit(1);
                    //throw new LotusException(new TWSException(id, errorCode, errorMsg));
                }
            }

            @Override
            public void show(String string) {
                logger.info("show: "+string);
            }
        }, new IBLogger(), new IBLogger());


        controller.connect("localhost", 4002, 2);
        semaphore.acquire();


        controller.reqAccountUpdates(true, account, new ApiController.IAccountHandler() {
            @Override
            public void accountValue(String account, String key, String value, String currency) {
                if(key.compareTo("TotalCashValue") == 0 && currency.compareTo("AUD") == 0) {
                    logger.info("accountValue: TotalCashValue="+value);
                    availableFunds = Double.valueOf(value);
                }
                if(key.compareTo("ExchangeRate") == 0 && currency.compareTo("USD") == 0) {
                    logger.info("accountValue: ExchangeRate="+value);
                    exchangeRate = Double.valueOf(value);
                }
            }

            @Override
            public void accountTime(String timeStamp) {

            }

            @Override
            public void accountDownloadEnd(String account) {
                logger.info("accountDownloadEnd - pre release");
                semaphore.release();
                logger.info("accountDownloadEnd - post release");
            }

            @Override
            public void updatePortfolio(Position position) {
                String symbol = position.contract().symbol();
                positions.put(symbol, position);
                logger.info("updatePortfolio: "+position);
            }
        });

        semaphore.acquire();

        if(exchangeRate == 0.0 || availableFunds == 0.0) {
            throw new LotusException("Account details were not provided");
        }

        logger.info(String.format("Account: AUD=%.2f rate=%.2f USD=%.2f",
                availableFunds,
                exchangeRate,
                availableFunds / exchangeRate

            ));

        controller.reqLiveOrders(new ApiController.ILiveOrderHandler() {
            @Override
            public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
                logger.info(String.format("openOrder: contract=%s, order=%s, orderState=%s", contract, order, orderState));
                try {
                    outputQueue.put(new LiveOpenOrder(contract, order, orderState));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void openOrderEnd() {

            }

            @Override
            public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice,
                                    long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                logger.info(String.format("orderStatus: orderId=%d orderStatus=%s filled=%d remaining=%d "+
                        "avgFillPrice=%f permId=%d parentId=%d lastFillPrice=%f clientId=%d whyHeld=%s",
                        orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId,
                        whyHeld));

                try {
                    outputQueue.put(new LiveOrderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId,
                                                        lastFillPrice, clientId, whyHeld));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void handle(int orderId, int errorCode, String errorMsg) {
                logger.info(String.format("handle: orderId=%d code=%d msg=%s", orderId, errorCode, errorMsg.replaceAll("\n", ":::")));
                try {
                    outputQueue.put(new LiveOrderError(orderId, errorCode, errorMsg));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        controller.reqExecutions(null, new ApiController.ITradeReportHandler() {
            @Override
            public void tradeReport(String tradeKey, NewContract contract, Execution execution) {
                logger.info(String.format("tradeReport: tradeKey=%s contract=%s execution=%s", tradeKey, contract, execution));
                try {
                    outputQueue.put(new TradeReport(tradeKey, contract, execution));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void tradeReportEnd() {

            }

            @Override
            public void commissionReport(String tradeKey, CommissionReport commissionReport) {
                logger.info(String.format("commissionReport: tradeKey=%s commissionReport=%s", tradeKey, commissionReport));
                try {
                    outputQueue.put(new TradeCommissionReport(tradeKey, commissionReport));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void disconnect() {
        controller.disconnect();
    }

    @Override
    public void buy(Investment investment) {
        logger.info(String.format("BUY: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.buyLimit, investment.qty));

        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createBuyOrder(account);

        controller.placeOrModifyOrder(contract, order, null);
    }

    @Override
    public void sell(Investment investment) {
        logger.info(String.format("SELL: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.sellLimit, investment.qtyFilled));

        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createSellOrder(account);

        controller.placeOrModifyOrder(contract, order, null);
    }

    @Override
    public Collection<Position> getOpenPositions() {
        return positions.values();
    }


    public double getLastClose(Investment investment) {
        NewContract contract = investment.createNewContract();
        //logger.info(String.format("getLastClose: %s/%s", contract.primaryExch(), contract.symbol()));
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Instant instant = Instant.now();
        String timeLimit = formatter.format(instant);

        final double[] closePrice = new double[1];
        final LocalDate[] date = new LocalDate[1];
        Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();

            controller.reqHistoricalData(contract, timeLimit, 1, Types.DurationUnit.DAY,
                    Types.BarSize._1_day, Types.WhatToShow.TRADES, true, new ApiController.IHistoricalDataHandler() {
                        @Override
                        public void historicalData(Bar bar, boolean hasGaps) {
                            logger.info(String.format("%s: %f", bar.formattedTime(), bar.close()));
                            Date dt = new Date(bar.time() * 1000);
                            date[0] = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            closePrice[0] = bar.close();

                        }

                        @Override
                        public void historicalDataEnd() {
                            semaphore.release();
                        }
                    });

            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return closePrice[0];
    }

    @Override
    public void updateHistory(InvestmentDao dao, Investment investment) {

    }

    @Override
    public double getExchangeRate() {
        return exchangeRate;
    }

    @Override
    public double getAvailableFunds() {
        return availableFunds;
    }
}
