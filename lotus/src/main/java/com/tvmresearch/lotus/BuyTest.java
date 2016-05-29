package com.tvmresearch.lotus;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.controller.*;
import com.tvmresearch.lotus.broker.ConnectionHandler;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import com.tvmresearch.lotus.db.model.Trigger;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.concurrent.Semaphore;

import static com.tvmresearch.lotus.db.model.Trigger.RejectReason.OK;

/**
 * Buy a single stock
 *
 * Created by horse on 27/05/2016.
 */
public class BuyTest {

    private static void log(String s) {
        System.out.println(s);
    }

    private static void log(String fmt, Object ...o) {
        System.out.println(String.format(fmt, o));
    }

    public static void main(String[] args) throws InterruptedException {
        ApiController controller = null;
        ConnectionHandler connectionHandler = new ConnectionHandler();
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); // format for display

        try {
            controller = new ApiController(connectionHandler, new ShowExecutions.IBLogger(), new ShowExecutions.IBLogger());

            controller.connect("localhost", 4002, 1);
            connectionHandler.waitForConnection();

            System.out.println("Account="+connectionHandler.getAccount());


            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();

            InvestmentDao dao = new InvestmentDaoImpl();

            Trigger trigger = new Trigger();
            trigger.symbol = "AAPL";
            trigger.exchange = "NASDAQ";
            trigger.date = LocalDate.now();
            trigger.rejectReason = OK;
            Investment investment = new Investment(trigger);
            investment.qty = 1;
            investment.qtyFilled = 1;

            NewContract contract = investment.createNewContract();
            NewOrder order = investment.createBuyOrder(connectionHandler.getAccount());

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

            /*
            controller.reqAccountUpdates(true, connectionHandler.getAccount(), new ApiController.IAccountHandler() {
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
                    log("ACCOUNT: portfolio: %s/%d", position.contract().symbol(), position.position());
                }
            });
            */

            controller.reqLiveOrders(new ApiController.ILiveOrderHandler() {
                @Override
                public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
                    //System.out.println((String.format("liveOrder: openOrder: contract=%s, order=%s, orderState=%s", contract, order, orderState)));
                    System.out.println(String.format("LIVE ORDERS Open Order: %s %s/%s [%s]", contract.symbol(), order.action(), order.tif(), orderState));
                }

                @Override
                public void openOrderEnd() {
                    System.out.println("LIVE ORDERS - END");
                }

                @Override
                public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                    // order status change

                    System.out.println(String.format("LIVE ORDERS: orderStatus: permid=%d, status=%s, filled=%d, remain=%d, why=%s", permId, status, filled, remaining, whyHeld));
                }

                @Override
                public void handle(int orderId, int errorCode, String errorMsg) {
                    System.out.println(String.format("LIVE ORDERS handle: code=%d, msg=%s", errorCode, errorMsg));
                }
            });

/*
            System.out.println("-----------------------------------------------");
            Thread.currentThread().sleep(5000);
            System.out.println("-----------------------------------------------");
            System.out.println("finished sleeping");


*/

            controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
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
            });


            semaphore.acquire();

            System.out.println("REQ EXECUTIONS Start");
            controller.reqExecutions(new ExecutionFilter(0, connectionHandler.getAccount(), null, null, null, null, null), new ApiController.ITradeReportHandler() {

                @Override
                public void tradeReport(String tradeKey, NewContract contract, Execution execution) {
                    System.out.println(String.format("REQ EXECUTIONS: trade_key=%s contract=%s execution=%s", tradeKey, contract, execution));
                }

                @Override
                public void tradeReportEnd() {
                    System.out.println("REQ EXECUTIONS End");
                    semaphore.release();
                }

                @Override
                public void commissionReport(String tradeKey, CommissionReport commissionReport) {
                    System.out.println(String.format("REQ EXECUTIONS trade_key=%s report=%s", tradeKey, commissionReport));
                }
            });

            semaphore.acquire();

            System.out.println("Order Id="+ order.orderId());

            log("-- little sleep --");
            Thread.sleep(30000);
            log("-- /little sleep --");

            controller.cancelOrder(order.orderId());

            System.out.println("--------------- SLEEPING (forever) --------------------------------");
            while(true) {
                Thread.currentThread().sleep(60000);
            }
            //System.out.println("------------FINISHED SLEEPING----------------------------");


        } finally {
            controller.disconnect();
        }


    }
}
