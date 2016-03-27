package com.tvmresearch.lotus.broker;

import com.ib.controller.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.TypeConverters;

/**
 * Created by horse on 27/03/2016.
 */
public class AccountHandler extends ASyncReceiver implements com.ib.controller.ApiController.IAccountHandler {

    private static final Logger logger = LogManager.getLogger(AccountHandler.class);

    public double availableFunds;

    @Override
    public void accountValue(String account, String key, String value, String currency) {
        logger.info(String.format("account=%s key=%s value=%s currency=%s",
                account, key, value, currency));

        if(key.compareTo("TotalCashBalance") == 0 && currency.compareTo("USD") == 0) {
            availableFunds = Double.valueOf(value);
        }
    }

    @Override
    public void accountTime(String timeStamp) {

    }

    @Override
    public void accountDownloadEnd(String account) {
        eventOccured();
    }

    @Override
    public void updatePortfolio(Position position) {
        logger.info("updatePortfolio: "+position);
    }
}
