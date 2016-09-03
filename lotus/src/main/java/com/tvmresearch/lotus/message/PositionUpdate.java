package com.tvmresearch.lotus.message;

import com.ib.controller.Position;
import com.tvmresearch.lotus.Lotus;

/**
 * Created by horse on 3/06/2016.
 */
public class PositionUpdate extends IBMessage {

    Position position;

    public PositionUpdate(Position position) {
        this.position = position;
    }

    @Override
    public void process(Lotus lotus) {
        lotus.processPosition(position);
    }
}
