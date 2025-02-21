package com.tvmresearch.lotus;

/**
 * All general exceptions are unchecked
 * <p>
 * Created by horse on 19/03/2016.
 */
public class LotusException extends RuntimeException {
    public LotusException(Throwable cause) {
        super(cause);
    }

    public LotusException(String s) {
        super(s);
    }
}
