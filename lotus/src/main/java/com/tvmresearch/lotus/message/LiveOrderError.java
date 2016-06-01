package com.tvmresearch.lotus.message;

import com.tvmresearch.lotus.Lotus;

/**
 * Created by horse on 29/05/2016.
 */
public class LiveOrderError extends IBMessage {
    public int orderId;
    public int errorCode;
    public String errorMsg;

    public LiveOrderError(int orderId, int errorCode, String errorMsg) {
        this.orderId = orderId;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public void process(Lotus lotus) {
        lotus.processOrderError(this);
    }
}
