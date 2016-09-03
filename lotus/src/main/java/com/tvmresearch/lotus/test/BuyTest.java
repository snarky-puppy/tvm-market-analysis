package com.tvmresearch.lotus.test;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.controller.*;
import com.tvmresearch.lotus.broker.IBLogger;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import com.tvmresearch.lotus.db.model.Trigger;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static com.tvmresearch.lotus.db.model.Trigger.RejectReason.OK;

/**
 * Buy a single stock
 * <p>
 * Created by horse on 27/05/2016.
 */
public class BuyTest {

    private static String account = null;

    private static void log(String s) {
        System.out.println(s);
    }

    private static void log(String fmt, Object... o) {
        System.out.println(String.format(fmt, o));
    }

    public static void main(String[] args) throws InterruptedException {
        ApiController controller = null;
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); // format for display

        try {
            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();
            controller = new ApiController(new ApiController.IConnectionHandler() {

                @Override
                public void connected() {
                    //semaphore.release();
                    log("Connected");
                }

                @Override
                public void disconnected() {

                }

                @Override
                public void accountList(ArrayList<String> list) {
                    if (account == null) {
                        account = list.get(0);
                        log("Got account: " + account);
                        semaphore.release();
                    }
                }

                @Override
                public void error(Exception e) {
                    log(e.getMessage(), e);
                    System.exit(1);
                }

                @Override
                public void message(int id, int errorCode, String errorMsg) {
                    // 399: Warning: your order will not be placed at the exchange until 2016-03-28 09:30:00 US/Eastern
                    if (errorCode != 399)
                        log(String.format("message: id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
                    if (errorCode < 1100 && errorCode != 399 && errorCode != 202) {
                        System.exit(1);
                        //throw new LotusException(new TWSException(id, errorCode, errorMsg));
                    }
                }

                @Override
                public void show(String string) {
                    log("show: " + string);
                }
            }, new IBLogger(), new IBLogger());

            controller.connect("localhost", 4002, 1);

            semaphore.acquire();

            System.out.println("Account=" + account);


            InvestmentDao dao = new InvestmentDaoImpl();

            Trigger trigger = new Trigger();
            trigger.symbol = "NTM";
            trigger.exchange = "ASX";
            trigger.date = LocalDate.now();
            trigger.rejectReason = OK;
            Investment investment = new Investment(trigger);
            investment.qty = 100;
            investment.qtyFilled = 1;

            NewContract contract = investment.createNewContract();
            NewOrder order = investment.createBuyOrder(account);

            order.orderType(OrderType.MKT);

            controller.reqAccountSummary("All", new AccountSummaryTag[]{AccountSummaryTag.TotalCashValue}, new ApiController.IAccountSummaryHandler() {
                @Override
                public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
                    log("ACCOUNT SUMMARY: account=%s tag=%s value=%s currency=%s", account, tag, value, currency);
                }

                @Override
                public void accountSummaryEnd() {
                    log("ACCOUNT SUMMARY: END");
                }
            });


            controller.reqAccountUpdates(true, account, new ApiController.IAccountHandler() {
                @Override
                public void accountValue(String account, String key, String value, String currency) {
                    log("ACCOUNT: account=%s key=%s value=%s currency=%s", account, key, value, currency);
                }

                @Override
                public void accountTime(String timeStamp) {
                    //log("ACCOUNT: accountTime=%s", timeStamp);
                }

                @Override
                public void accountDownloadEnd(String account) {
                    log("ACCOUNT: accountDownloadEnd");
                }

                @Override
                public void updatePortfolio(Position position) {
                    log("ACCOUNT: portfolio: %s", position);
                }
            });


            controller.reqLiveOrders(new ApiController.ILiveOrderHandler() {
                @Override
                public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
                    log("LIVE ORDERS: openOrder: contract=%s\norder=%s\norderState=%s", contract, order, orderState);
                }

                @Override
                public void openOrderEnd() {
                    System.out.println("LIVE ORDERS - END");
                }

                @Override
                public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                    // order status change
                    // This method is called whenever the status of an order changes.
                    // It is also fired after reconnecting to TWS if the client has any open orders.

                    log("LIVE ORDERS: orderStatus: orderId=%d orderStatus=%s filled=%d remaining=%d " +
                                    "avgFillPrice=%f permId=%d parentId=%d lastFillPrice=%f clientId=%d whyHeld=%s",
                            orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId,
                            whyHeld);
                }

                @Override
                public void handle(int orderId, int errorCode, String errorMsg) {
                    System.out.println(String.format("LIVE ORDERS: handle: orderId=%d, code=%d, msg=%s", orderId, errorCode, errorMsg));
                }
            });

/*
            System.out.println("-----------------------------------------------");
            Thread.currentThread().sleep(5000);
            System.out.println("-----------------------------------------------");
            System.out.println("finished sleeping");


*/

            controller.placeOrModifyOrder(contract, order, null); /*new ApiController.IOrderHandler() {
                @Override
                public void orderState(NewOrderState orderState) {
                    // this one gets called for a BUY
                    System.out.println(String.format("PLACE OR MODIFY ORDER: orderState=%s", orderState));
                }

                @Override
                public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                    System.out.println(String.format("PLACE OR MODIFY ORDER: orderStatus: status=%s, filled=%d, remain=%d avgFillPrice=%f permId=%d parentId=%d lastFillPrice=%f clientId=%d whyHeld=%s"
                                                        ,status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld));
                    semaphore.release();
                }

                @Override
                public void handle(int errorCode, String errorMsg) {
                    System.out.println(String.format("PLACE OR MODIFY ORDER: handle: code=%d msg=%s", errorCode, errorMsg));
                    if(errorCode != 399) {
                        semaphore.release();
                    }
                }
            });*/

            //log("WAIT for place order");

            //semaphore.acquire();

            System.out.println("REQ EXECUTIONS Start");
            controller.reqExecutions(new ExecutionFilter(0, account, null, null, null, null, null), new ApiController.ITradeReportHandler() {

                @Override
                public void tradeReport(String tradeKey, NewContract contract, Execution execution) {
                    System.out.println(String.format("REQ EXECUTIONS: trade_key=%s contract=%s execution=%s", tradeKey, contract, execution));
                }

                @Override
                public void tradeReportEnd() {
                    System.out.println("REQ EXECUTIONS End");
                    //semaphore.release();
                }

                @Override
                public void commissionReport(String tradeKey, CommissionReport commissionReport) {
                    System.out.println(String.format("REQ EXECUTIONS trade_key=%s report=%s", tradeKey, commissionReport));
                }
            });

            //semaphore.acquire();

            System.out.println("Order Id=" + order.orderId());

            log("-- little sleep --");
            Thread.sleep(30000);
            log("-- /little sleep --");

            //controller.cancelOrder(order.orderId());

            System.out.println("--------------- SLEEPING (forever) --------------------------------");
            while (true) {
                Thread.sleep(60000);
            }
            //System.out.println("------------FINISHED SLEEPING----------------------------");


        } finally {
            if (controller != null)
                controller.disconnect();
        }


    }
}
