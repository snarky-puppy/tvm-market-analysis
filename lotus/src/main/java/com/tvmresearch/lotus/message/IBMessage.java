package com.tvmresearch.lotus.message;

import com.tvmresearch.lotus.Lotus;

/**
 * Message base class
 * <p>
 * Created by matt on 31/05/16.
 */
public abstract class IBMessage {
    public abstract void process(Lotus lotus);
}
