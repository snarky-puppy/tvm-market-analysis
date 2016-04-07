package com.tvmresearch.lotus.broker;

import com.ib.controller.Position;
import com.tvmresearch.lotus.db.model.Investment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.TypeConverters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Receive account info
 *
 * Created by horse on 27/03/2016.
 */
public class AccountHandler extends ASyncReceiver implements com.ib.controller.ApiController.IAccountHandler {

    private static final Logger logger = LogManager.getLogger(AccountHandler.class);

    // k=conId, v=Investment
    public List<Position> positions = new ArrayList<>();
    public double availableFunds;
    public double exchangeRate;

    @Override
    public void accountValue(String account, String key, String value, String currency) {
        //if(currency != null && currency.compareTo("USD") == 0)

        //logger.info(String.format("account=%s key=%s value=%s currency=%s",
        //            account, key, value, currency));
        if(key.compareTo("ExcessLiquidity") == 0 && currency.compareTo("AUD") == 0) {
            availableFunds = Double.valueOf(value);
        }
        if(key.compareTo("ExchangeRate") == 0 && currency.compareTo("USD") == 0) {
            exchangeRate = Double.valueOf(value);
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
        logger.info(position);
        // TODO: report.updatePortfolio(position)
        positions.add(position);
    }
}
