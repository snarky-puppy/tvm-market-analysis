package com.tvmresearch.lotus.broker;

/**
 * Created by horse on 27/03/2016.
 */
public class TWSException extends Exception {

    public final int id;
    public final int errorCode;
    public final String errorMsg;

    public TWSException(int id, int errorCode, String errorMsg) {
        this.id = id;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
