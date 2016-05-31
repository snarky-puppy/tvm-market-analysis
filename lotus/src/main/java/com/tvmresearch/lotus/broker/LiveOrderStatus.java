package com.tvmresearch.lotus.broker;

import com.ib.controller.OrderStatus;
import com.tvmresearch.lotus.Compounder;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.TriggerDao;

/**
 * Created by horse on 29/05/2016.
 */
public class LiveOrderStatus extends IBMessage {

    public int orderId;
    public OrderStatus status;
    public int filled;
    public int remaining;
    public double avgFillPrice;
    public long permId;
    public int parentId;
    public double lastFillPrice;
    public int clientId;
    public String whyHeld;
    
    public LiveOrderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId,
                           int parentId, double lastFillPrice, int clientId, String whyHeld) {
        this.orderId = orderId;
        this.status = status;
        this.filled = filled;
        this.remaining = remaining;
        this.avgFillPrice = avgFillPrice;
        this.permId = permId;
        this.parentId = parentId;
        this.lastFillPrice = lastFillPrice;
        this.clientId = clientId;
        this.whyHeld = whyHeld;
    }

    @Override
    public void process(Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao) {
        // This method is called whenever the status of an order changes.
        // It is also fired after reconnecting to TWS if the client has any open orders.
        // https://www.interactivebrokers.com/en/software/api/apiguide/java/orderstatus.htm


    }
}
