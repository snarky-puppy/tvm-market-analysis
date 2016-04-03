package com.tvmresearch.lotus.broker;

import com.ib.controller.ApiController;
import com.ib.controller.NewContract;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by horse on 1/04/2016.
 */
public class PositionHandler extends ASyncReceiver implements ApiController.IPositionHandler {
    private static final Logger logger = LogManager.getLogger(PositionHandler.class);

    @Override
    public void position(String account, NewContract contract, int position, double avgCost) {
        logger.info(String.format("acc=%s, con=%s, pos=%d, avgCost=%.2f", account, contract, position, avgCost));
    }

    @Override
    public void positionEnd() {
        eventOccured();
    }
}
