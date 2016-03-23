package com.tvmresearch.lotus;

import com.ib.controller.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by matt on 9/03/16.
 */
public class InteractiveBrokerAPI implements ApiConnection.ILogger, ApiController.IConnectionHandler, Broker {

    private static final Logger logger = LogManager.getLogger(InteractiveBrokerAPI.class);

    private ApiController controller = new ApiController(this, this, this);
    private List<String> accountList = new ArrayList<String>();


    public static void main(String[] args) {
        InteractiveBrokerAPI interactiveBrokerAPI = new InteractiveBrokerAPI();
        interactiveBrokerAPI.test();
    }

    private void test() {

        controller.connect("localhost", 7497, 0);
        try {
            boolean accountListPopulated = false;
            while(accountListPopulated == false) {
                synchronized (accountList) {
                    accountListPopulated = accountList.size() > 0;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        controller.cancelAllOrders();

        logger.info("2");

        for(int i = 0; i < 5; i++) {
            NewContract contract = new NewContract();
            NewOrder order = new NewOrder();


            order.totalQuantity(100);
            order.lmtPrice(1);

            // contract
            contract.symbol("BHP");
            contract.secType(Types.SecType.STK);
            contract.exchange("SMART");
            contract.primaryExch("ASX");
            contract.currency("AUD");

            // order
            order.account(accountList.get(0));
            order.action(Types.Action.BUY);
            order.totalQuantity(100);
            order.orderType(OrderType.MKT);
            order.lmtPrice(1.0);
            order.tif(Types.TimeInForce.DAY);

            // place order
            controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
                @Override
                public void orderState(NewOrderState orderState) {
                }

                @Override
                public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                }

                @Override
                public void handle(int errorCode, final String errorMsg) {
                    logger.error(String.format("place_order: code=%d, msg=%s", errorCode, errorMsg));
                }
            });
        }


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        controller.disconnect();
    }



    @Override
    public void log(String valueOf) {

    }

    @Override
    public void connected() {
        logger.info("Connected");
    }

    @Override
    public void disconnected() {
        logger.info("Disconnected");
    }

    @Override
    public void accountList(ArrayList<String> list) {
        synchronized (accountList) {
            this.accountList.addAll(list);
            logger.info("Accounts:");
            for (String s : list) {
                logger.info("\t" + s);
            }
        }
    }

    @Override
    public void error(Exception e) {
        logger.error(e);
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        logger.info(String.format("message: id=%d, code=%d, msg=%s", id, errorCode, errorMsg));
    }

    @Override
    public void show(String string) {
        logger.info("show: "+string);
    }

    @Override
    public double getAvailableFunds() {
        return 200000;
    }
}
