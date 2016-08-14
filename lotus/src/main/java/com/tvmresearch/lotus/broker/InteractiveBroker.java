package com.tvmresearch.lotus.broker;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.controller.*;
import com.tvmresearch.lotus.DateUtil;
import com.tvmresearch.lotus.HistoricalDataPoint;
import com.tvmresearch.lotus.LotusException;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ConnectException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * IB API
 * <p>
 * Created by horse on 27/03/2016.
 */
public class InteractiveBroker implements Broker {

    private static final Logger logger = LogManager.getLogger(InteractiveBroker.class);

    private static final int clientId = 2;

    private final ApiController controller;
    private final BlockingQueue<IBMessage> outputQueue;
    private InvestmentDao investmentDao;

    // k=symbol
    private Map<String, Position> positions = new HashMap<>();

    private String account;
    private double availableFunds = 0.0;
    private double exchangeRate = 0.0;


    public InteractiveBroker(BlockingQueue<IBMessage> _outputQueue, InvestmentDao investmentDao) throws InterruptedException, ConnectException {
        this.outputQueue = _outputQueue;
        this.investmentDao = investmentDao;

        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        final Boolean[] connectFailed = {false};

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
                if (account == null) {
                    account = list.get(0);
                    logger.info("Got account: " + account);
                    semaphore.release();
                }
            }

            @Override
            public void error(Exception e) {
                logger.error("error: " + e.getMessage(), e);
                queueDisconnect();

            }

            @Override
            public void message(int id, int errorCode, String errorMsg) {

                switch(errorCode) {
                    case 399:
                        // 399: Warning: your order will not be placed at the exchange until 2016-03-28 09:30:00 US/Eastern
                        // ignore it
                        break;
                    case 502:
                        // 502: Couldn't connect to TWS.  Confirm that "Enable ActiveX and Socket Clients" is enabled on the TWS "Configure->API" menu.
                        logger.error(String.format("message: id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
                        connectFailed[0] = true;
                        semaphore.release();
                        break;
                    default:
                        logger.error(String.format("message: id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
                        break;
                }
            }

            @Override
            public void show(String string) {
                logger.info("show: " + string);
            }
        }, new IBLogger(), new IBLogger());


        controller.connect("localhost", 4002, clientId);
        semaphore.acquire();

        if (connectFailed[0]) {
            throw new ConnectException();
        }

        controller.reqAccountUpdates(true, account, new ApiController.IAccountHandler() {
            @Override
            public void accountValue(String account, String key, String value, String currency) {
                //System.out.println(String.format("%s=%s [%s]", key, value, currency));
                if (key.compareTo("TotalCashValue") == 0 && currency.compareTo("AUD") == 0) {
                    logger.info("accountValue: TotalCashValue=" + value);
                    availableFunds = Double.valueOf(value);
                }
                if (key.compareTo("ExchangeRate") == 0 && currency.compareTo("USD") == 0) {
                    logger.info("accountValue: ExchangeRate=" + value);
                    exchangeRate = Double.valueOf(value);
                }
            }

            @Override
            public void accountTime(String timeStamp) {

            }

            @Override
            public void accountDownloadEnd(String account) {
                semaphore.release();
            }

            @Override
            public void updatePortfolio(Position position) {
                String symbol = position.contract().symbol();
                positions.put(symbol, position);
                logger.info("updatePortfolio: " + position);
                try {
                    outputQueue.put(new PositionUpdate(position));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        semaphore.acquire();

        if (exchangeRate == 0.0 || availableFunds == 0.0) {
            logger.error(String.format("Account details were not provided: %f/%f", exchangeRate, availableFunds));
            disconnect();
            throw new ConnectException();
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
                logger.info(String.format("orderStatus: orderId=%d orderStatus=%s filled=%d remaining=%d " +
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
                    outputQueue.put(new LiveOrderError(orderId, errorCode, errorMsg.replaceAll("\n", ":::")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        controller.reqExecutions(new ExecutionFilter(clientId, account, null, null, null, null, null), new ApiController.ITradeReportHandler() {
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

    private void queueDisconnect() {
        try {
            outputQueue.put(new IBDisconnect());
        } catch (InterruptedException e2) {
            e2.printStackTrace();
            System.exit(1);
        }
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
        order.orderId(investmentDao.getNextOrderId());

        controller.placeOrModifyOrder(contract, order, null);
        investment.buyOrderId = order.orderId();
    }

    @Override
    public void sell(Investment investment) {
        logger.info(String.format("SELL: %s/%s lim=%.2f qty=%d", investment.trigger.exchange, investment.trigger.symbol,
                investment.sellLimit, investment.qtyFilled));

        NewContract contract = investment.createNewContract();
        NewOrder order = investment.createSellOrder(account);
        order.orderId(investmentDao.getNextOrderId());

        controller.placeOrModifyOrder(contract, order, null);
        investment.sellOrderId = order.orderId();
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
    public void updateHistory(InvestmentDao dao, Investment investment, int missingDays) {
        NewContract contract = investment.createNewContract();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                        .withZone(ZoneId.of("GMT"));

        Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();

            List<HistoricalDataPoint> history = new ArrayList<>();

            ApiController.IHistoricalDataHandler historicalDataHandler = new ApiController.IHistoricalDataHandler() {
                @Override
                public void historicalData(Bar bar, boolean hasGaps) {
                    Date dt = new Date(bar.time() * 1000);
                    LocalDate date = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    logger.info(String.format("history: %s: %s: %f", investment.trigger.symbol, bar.formattedTime(), bar.close()));
                    history.add(new HistoricalDataPoint(date, bar.close()));
                }

                @Override
                public void historicalDataEnd() {
                    semaphore.release();
                }
            };

            ////////////////////////////////

            final int maxDays = 30;

            logger.info(String.format("updateHistory: %s/%s missingDays=%d", contract.primaryExch(), contract.symbol(), missingDays));

            do {
                int fetchedDays = missingDays;
                LocalDate end = LocalDate.now();

                if (missingDays > maxDays) {
                    end = DateUtil.minusBusinessDays(end, missingDays - maxDays);
                    fetchedDays = maxDays;
                }

                String timeLimit = formatter.format(end.atStartOfDay());

                controller.reqHistoricalData(contract, timeLimit, fetchedDays, Types.DurationUnit.DAY,
                        Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);
                semaphore.acquire();

                missingDays -= fetchedDays;

            } while (missingDays > 0);

            dao.addHistory(investment, history);

        } catch (InterruptedException e) {
            throw new LotusException(e);
        }

    }

    @Override
    public double getExchangeRate() {
        return exchangeRate;
    }

    @Override
    public double getAvailableFundsUSD() {
        return availableFunds / exchangeRate;
    }

    @Override
    public double getAvailableFundsAUD() {
        return availableFunds;
    }

    private class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }
}
