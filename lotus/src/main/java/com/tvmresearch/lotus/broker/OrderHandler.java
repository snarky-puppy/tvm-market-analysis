package com.tvmresearch.lotus.broker;

import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.tvmresearch.lotus.db.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 27/03/2016.
 */
public class OrderHandler extends ASyncReceiver implements com.ib.controller.ApiController.IOrderHandler {

    private List<Position> positionList;

    public OrderHandler() {
        positionList = new ArrayList<>();
    }

    @Override
    public void orderState(NewOrderState orderState) {

    }

    @Override
    public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {

    }

    @Override
    public void handle(int errorCode, String errorMsg) {

    }

    public void addPosition(Position position) {
        synchronized (positionList) {
            positionList.add(position);
        }
    }
}
