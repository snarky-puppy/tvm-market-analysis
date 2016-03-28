package com.tvmresearch.lotus.broker;

import com.ib.controller.ApiController;
import com.ib.controller.NewContractDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Stock Data callback
 *
 * Created by horse on 27/03/2016.
 */
public class StockHandler implements ApiController.IContractDetailsHandler {
    private static final Logger logger = LogManager.getLogger(StockHandler.class);
    private double minTick;

    @Override
    public void contractDetails(ArrayList<NewContractDetails> list) {
        logger.info("CONTRACT DETAILS:");
        for(NewContractDetails details : list) {
            minTick = details.minTick();
            logger.info(details);
        }
    }
}
