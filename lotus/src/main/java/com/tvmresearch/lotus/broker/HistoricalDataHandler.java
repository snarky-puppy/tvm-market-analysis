package com.tvmresearch.lotus.broker;

import com.ib.controller.ApiController;
import com.ib.controller.Bar;


/**
 * Created by horse on 27/03/2016.
 */
public class HistoricalDataHandler extends ASyncReceiver implements ApiController.IHistoricalDataHandler {

    public double closePrice;

    @Override
    public void historicalData(Bar bar, boolean hasGaps) {
        closePrice = bar.close();
    }

    @Override
    public void historicalDataEnd() {
        eventOccured();
    }
}
