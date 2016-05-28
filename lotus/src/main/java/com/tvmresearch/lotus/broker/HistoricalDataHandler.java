package com.tvmresearch.lotus.broker;

import com.ib.controller.ApiController;
import com.ib.controller.Bar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;


/**
 * Historical data handler
 *
 * Created by horse on 27/03/2016.
 */
public class HistoricalDataHandler extends ASyncReceiver implements ApiController.IHistoricalDataHandler {

    private static final Logger logger = LogManager.getLogger(HistoricalDataHandler.class);

    public double closePrice;
    public LocalDate date;

    @Override
    public void historicalData(Bar bar, boolean hasGaps) {

        logger.info(String.format("%s: %f", bar.formattedTime(), bar.close()));
        Date dt = new Date(bar.time()*1000);
        date = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        closePrice = bar.close();
    }

    @Override
    public void historicalDataEnd() {
        eventOccured();
    }
}
