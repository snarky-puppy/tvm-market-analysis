package com.tvmresearch.lotus.broker;

import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.tvmresearch.lotus.db.model.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Created by horse on 27/03/2016.
 */
public class BuyOrderHandler extends ASyncReceiver implements com.ib.controller.ApiController.IOrderHandler {

    private static final Logger logger = LogManager.getLogger(BuyOrderHandler.class);

    private final Position position;

    public BuyOrderHandler(Position position) {
        this.position = position;
    }

    @Override
    public void orderState(NewOrderState orderState) {
        logger.info(orderState);
        eventOccured();
    }

    @Override
    public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        logger.info(status);
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
        if(errorCode != 399) {
            logger.error(String.format("code=%d, msg=%s [%s/%s]", errorCode, errorMsg, position.trigger.exchange, position.trigger.symbol));
            position.errorCode = errorCode;
            position.errorMsg = errorMsg;
            position.orderId = -1;
        }
        position.serialise();
        eventOccured();

    }
}
