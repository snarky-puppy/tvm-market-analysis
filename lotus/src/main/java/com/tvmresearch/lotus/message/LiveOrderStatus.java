package com.tvmresearch.lotus.message;

import com.ib.controller.OrderStatus;
import com.tvmresearch.lotus.Lotus;

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
    public void process(Lotus lotus) {
        lotus.processOrderStatus(orderId, status, filled, remaining,
                avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
    }
}
