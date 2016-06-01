package com.tvmresearch.lotus.test;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.controller.*;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;


/**
 * Created by horse on 21/05/2016.
 */
public class ShowExecutions {

/*
    public static class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ApiController controller = null;
        ConnectionHandler connectionHandler = new ConnectionHandler();
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); // format for display

        try {
            controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

            controller.connect("localhost", 4002, 1);
            connectionHandler.waitForConnection();

            System.out.println("Account="+connectionHandler.getAccount());


            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();


            controller.reqLiveOrders(new ApiController.ILiveOrderHandler() {
                @Override
                public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
                    System.out.println(String.format("openOrder: %s %s %s", contract, order, orderState));
                }

                @Override
                public void openOrderEnd() {
                    semaphore.release();

                }

                @Override
                public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                    System.out.println(String.format("orderId=%d, status=%s, filled=%d, remaining=%d, avgFillPrice=%f, permId=%d, parentId=%d, lastFillPrice=%f, clientId=%d, whyHeld=%s",
                    orderId,  status, filled,  remaining,  avgFillPrice,  permId,  parentId,  lastFillPrice,  clientId,  whyHeld));
                }

                @Override
                public void handle(int orderId, int errorCode, String errorMsg) {
                    System.out.println(String.format("orderId=%d code=%d msg=%s", orderId, errorCode, errorMsg));
                }
            });

            semaphore.acquire();


            controller.reqAccountUpdates(true, connectionHandler.getAccount(), new ApiController.IAccountHandler() {
                @Override
                public void accountValue(String account, String key, String value, String currency) {
                    if((key.compareTo("TotalCashValue") == 0 && currency.compareTo("AUD") == 0) ||
                       (key.compareTo("ExchangeRate") == 0 && currency.compareTo("USD") == 0)) {
                        System.out.println(String.format("account=%s key=%s value=%s currency=%s", account, key, value, currency));
                    }
                }

                @Override
                public void accountTime(String timeStamp) {
                    System.out.println("account: time="+timeStamp);
                }

                @Override
                public void accountDownloadEnd(String account) {
                    semaphore.release();
                }

                @Override
                public void updatePortfolio(Position position) {
                    System.out.println(position);
                }
            });

            semaphore.acquire();
            System.out.println("Trade Report Start");

            controller.reqExecutions(new ExecutionFilter(0, connectionHandler.getAccount(), null, null, null, null, null), new ApiController.ITradeReportHandler() {

                @Override
                public void tradeReport(String tradeKey, NewContract contract, Execution execution) {
                    System.out.println(String.format("trade_key=%s contract=%s execution=%s", tradeKey, contract, execution));
                }

                @Override
                public void tradeReportEnd() {
                    System.out.println("Trade Report End");
                    semaphore.release();
                }

                @Override
                public void commissionReport(String tradeKey, CommissionReport commissionReport) {
                    System.out.println(String.format("trade_key=%s report=%s", tradeKey, commissionReport));
                }
            });

            semaphore.acquire();

        } finally {
            controller.disconnect();
        }
    }
    */
}
