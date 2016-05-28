package com.tvmresearch.lotus.broker;

import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.tvmresearch.lotus.db.model.Investment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

/**
 *
 * Created by horse on 27/03/2016.
 */
public class SellOrderHandler extends ASyncReceiver implements com.ib.controller.ApiController.IOrderHandler {

    private static final Logger logger = LogManager.getLogger(SellOrderHandler.class);

    private final Investment investment;

    public SellOrderHandler(Investment investment) {
        this.investment = investment;
    }

    @Override
    public void orderState(NewOrderState orderState) {
        logger.info(orderState);
        //eventOccured();
    }

    @Override
    public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        logger.info(status);
        investment.state = Investment.State.SELL;
        investment.sellDateStart = LocalDate.now();
        eventOccured();
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
        logger.info(String.format("%d: %s", errorCode, errorMsg));
        if(errorCode != 399) {
            logger.error(String.format("code=%d, msg=%s [%s/%s]", errorCode, errorMsg, investment.trigger.exchange, investment.trigger.symbol));
            investment.errorCode = errorCode;
            investment.errorMsg = errorMsg;
            //investment.conId = -1;
            //investment.state = Investment.State.ERROR;
            eventOccured();
        }
    }
}
