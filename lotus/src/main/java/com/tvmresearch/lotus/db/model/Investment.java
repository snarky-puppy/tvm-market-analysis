package com.tvmresearch.lotus.db.model;

import com.ib.controller.*;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;

import java.sql.*;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Position record
 *
 * Created by matt on 24/03/16.
 */
public class Investment {
    public Trigger trigger;

    public Map<LocalDate, Double> history;

    public Integer id = null;

    // compounder accounting
    public double cmpMin;
    public double cmpVal;
    public double cmpTotal;

    /* ib */
    public long buyOrderId;
    public long sellOrderId;
    public long buyPermId;
    public long sellPermId;

    public long conId;

    public State state;

    /* buying */

    // 0.1% higher than trigger close price
    public double buyLimit;
    public LocalDate buyDate;

    // quantity stocks needed to fill cmpTotal
    public int qty;
    // price of qty stocks
    public double qtyValue = 0.0;
    // qty stocks actually filled
    public Integer qtyFilled;
    // value of stocsk actually filled
    public Double buyFillValue;

    /* selling */

    // sell when value reaches this limit (>10%)
    public double sellLimit;
    // sell when date reaches this limit (84 days)
    public LocalDate sellDateLimit;

    // average sell price
    public Double avgSellPrice;

    // total value of withdrawal
    public Double sellFillVal;

    // date of selling
    public LocalDate sellDateStart;
    public LocalDate sellDateEnd;



    public Double realPnL;

    public Integer errorCode;
    public String errorMsg;

    public enum State {
        UNCONFIRMED,
        BUYPRESUBMITTED,
        BUY,
        BUYFILLED,
        SELLPRESUBMITTED,
        SELL,
        SELLFILLED,
        ORDERFAILED, ERROR
    }

    public Investment(Trigger trigger) {
        this.trigger = trigger;
        this.buyDate = LocalDate.now();
        this.state = State.UNCONFIRMED;
        this.history = history;
    }


    public NewContract createNewContract() {
        NewContract rv = new NewContract();
        if(trigger.exchange.compareTo("ASX") == 0)
            rv.currency("AUD");
        else
            rv.currency("USD");

        if(trigger.exchange.compareTo("NYSE_Arca") == 0)
            trigger.exchange = "ARCA";

        //if(trigger.exchange.equals("NASDAQ"))
        //    trigger.exchange = "ISLAND";

        rv.exchange("SMART");
        rv.symbol(trigger.symbol);

        //rv.secId(trigger.symbol);
        rv.primaryExch(trigger.exchange);
        rv.secType(com.ib.controller.Types.SecType.STK);
        return rv;
    }

    public NewOrder createBuyOrder(String account) {
        NewOrder order = new NewOrder();
        order.account(account);
        order.action(com.ib.controller.Types.Action.BUY);
        order.totalQuantity(qty);
        order.orderType(OrderType.LMT);
        order.lmtPrice(buyLimit);
        order.tif(com.ib.controller.Types.TimeInForce.DAY);
        order.transmit(true);
        return order;
    }

    public NewOrder createSellOrder(String account) {
        NewOrder order = new NewOrder();
        order.account(account);
        order.action(com.ib.controller.Types.Action.SELL);
        order.totalQuantity(qtyFilled);
        order.orderType(OrderType.MKT);
        order.lmtPrice(sellLimit);
        order.tif(com.ib.controller.Types.TimeInForce.GTC);
        order.transmit(true);
        return order;
    }
}
