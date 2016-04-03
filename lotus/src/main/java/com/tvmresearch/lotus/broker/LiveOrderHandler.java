package com.tvmresearch.lotus.broker;

import com.ib.controller.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 1/04/2016.
 */
public class LiveOrderHandler extends ASyncReceiver implements ApiController.ILiveOrderHandler {

    private static final Logger logger = LogManager.getLogger(LiveOrderHandler.class);

    List<OpenOrder> openOrders = new ArrayList<>();

    @Override
    public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
        logger.info(String.format("openOrder: contract=%s, order=%s, orderState=%s", contract, order, orderState));
        openOrders.add(new OpenOrder(contract, order, orderState));
    }

    @Override
    public void openOrderEnd() {
        eventOccured();
    }

    @Override
    public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        logger.info(String.format("orderStatus: permid=%d, status=%s", permId, status));
    }

    @Override
    public void handle(int orderId, int errorCode, String errorMsg) {
        logger.error(String.format("code=%d, msg=%s", errorCode, errorMsg));
    }
}
