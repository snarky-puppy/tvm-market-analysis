package com.tvmresearch.lotus.broker;

/**
 * Created by horse on 29/05/2016.
 */
public class LiveOrderError {
    public int orderId;
    public int errorCode;
    public String errorMsg;

    public LiveOrderError(int orderId, int errorCode, String errorMsg) {
        this.orderId = orderId;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
