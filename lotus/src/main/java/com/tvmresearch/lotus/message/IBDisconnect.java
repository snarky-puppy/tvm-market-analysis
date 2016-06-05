package com.tvmresearch.lotus.message;

import com.tvmresearch.lotus.Lotus;

/**
 * Created by horse on 6/06/2016.
 */
public class IBDisconnect extends IBMessage {
    @Override
    public void process(Lotus lotus) {
        lotus.processDisconnect();
    }
}
