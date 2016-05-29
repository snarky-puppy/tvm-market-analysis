package com.tvmresearch.lotus.broker;

import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;

/**
 * Tuple for passing around order data
 *
 * Created by horse on 3/04/2016.
 */
public class LiveOpenOrder {
    public NewContract contract;
    public NewOrder order;
    public NewOrderState orderState;

    public LiveOpenOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
        this.contract = contract;
        this.order = order;
        this.orderState = orderState;
    }
}
