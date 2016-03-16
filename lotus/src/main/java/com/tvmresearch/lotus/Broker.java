package com.tvmresearch.lotus;

import com.ib.client.*;
import com.ib.controller.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import samples.apidemo.ApiDemo;

import javax.swing.*;
import java.util.ArrayList;


/**
 * Created by matt on 9/03/16.
 */
public class Broker implements ApiConnection.ILogger, ApiController.IConnectionHandler {

    private static final Logger logger = LogManager.getLogger(Broker.class);

    private ApiController controller = new ApiController(this, this, this);


    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.test();
    }

    private void test() {

        controller.connect("localhost", 7497, 0);

        NewContract contract = new NewContract();
        NewOrder order = new NewOrder();

        order.totalQuantity(100);
        order.lmtPrice(1);






        // place order
        controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
            @Override public void orderState(NewOrderState orderState) {
            }
            @Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            }
            @Override public void handle(int errorCode, final String errorMsg) {
                logger.error(String.format("place_order: code=%d, msg=%s",errorCode, errorMsg));
            }
        });


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
        logger.info("Accounts:");
        for(String s : list) {
            logger.info("\t"+s);
        }
    }

    @Override
    public void error(Exception e) {
        logger.error(e);
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        logger.error(String.format("message: id=%d, code=%d, msg=%s", id, errorCode, errorMsg));
    }

    @Override
    public void show(String string) {
        logger.info("show: "+string);
    }
}
