package com.tvmresearch.lotus;

import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.db.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Implement event processing logic
 *
 * Created by matt on 23/03/16.
 */
public class EventProcessor {

    private static final Logger logger = LogManager.getLogger(EventProcessor.class);

    private final Broker broker;
    private final Compounder compounder;
    private final InvestmentDao investmentDao;
    private final TriggerDao triggerDao;

    public EventProcessor(Broker broker, Compounder compounder, TriggerDao triggerDao, InvestmentDao investmentDao) {
        this.broker = broker;
        this.compounder = compounder;
        this.triggerDao = triggerDao;
        this.investmentDao = investmentDao;
    }

    public void processTriggers() {
        List<Trigger> triggerList = triggerDao.getTodaysTriggers();
        triggerList.stream()
                .filter(this::validateTrigger)
                .map(this::triggerInvestment)
                .filter(i -> i != null)
                .forEach(investmentDao::serialise);

        triggerDao.serialise(triggerList);
    }

    private Investment triggerInvestment(Trigger trigger) {
        Investment investment = compounder.createInvestment(trigger);

        if(investment == null)
            return null;

        if(!broker.buy(investment))
            compounder.releaseInvestmentFunds(investment);

        return investment;

    }

    public boolean validateTrigger(Trigger trigger) {



        if(!trigger.event) {
            trigger.rejectReason = Trigger.RejectReason.NOTEVENT;
            return false;
        }

        if(trigger.zscore > Configuration.MIN_ZSCORE) {
            trigger.rejectReason = Trigger.RejectReason.ZSCORE;
            trigger.rejectData = Configuration.MIN_ZSCORE;
            return false;
        }

        if(!isActiveSymbol(trigger)) {
            trigger.rejectReason = Trigger.RejectReason.CATEGORY;
            return false;
        }

        if(trigger.avgVolume > Configuration.MIN_VOLUME) {
            trigger.rejectReason = Trigger.RejectReason.VOLUME;
            trigger.rejectData = Configuration.MIN_VOLUME;
            return false;
        }

        double nextInvest = compounder.nextInvestmentAmount();
        double pc = nextInvest / trigger.avgVolume;
        if(pc >= 1.0) {
            trigger.rejectReason = Trigger.RejectReason.INVESTAMT;
            trigger.rejectData = nextInvest;
            return false;
        }

        if(!compounder.fundsAvailable()) {
            trigger.rejectReason = Trigger.RejectReason.NOFUNDS;
            trigger.rejectData = nextInvest;
            return false;
        }

        // The price does not conform to the minimum price variation for this contract.
       // if(!broker.verifyTickSize(trigger, Configuration.BUY_LIMIT_FACTOR - 1)) {
       //     trigger.rejectReason = Trigger.RejectReason.TICKSIZE;
       // }

        trigger.rejectReason = Trigger.RejectReason.OK;
        return true;
    }

    private boolean isActiveSymbol(Trigger trigger) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement("SELECT COUNT(*) FROM active_symbols WHERE exchange = ? AND symbol = ?");
            stmt.setString(1, trigger.exchange);
            stmt.setString(2, trigger.symbol);
            rs = stmt.executeQuery();
            if(rs.next()) {
                int i = rs.getInt(1);
                return i > 0;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    public void processInvestments() {
        // check open positions for Sell events
        List<Investment> investmentList = investmentDao.getFilledInvestments();
        for(Investment investment : investmentList) {
            if(isSellEvent(investment)) {
                broker.sell(investment);
                investmentDao.serialise(investment);
            }
        }
    }

    private boolean isSellEvent(Investment investment) {

        // check price limit
        double todayPrice = broker.getLastClose(investment);
        logger.info(String.format("[%s/%s] sell limit? %.2f >= %.2f",
                investment.trigger.exchange, investment.trigger.symbol,
                todayPrice, investment.sellLimit));
        if(todayPrice >= investment.sellLimit)
            return true;

        // check date limit
        LocalDate now = LocalDate.now();
        if(investment.sellDateLimit.isEqual(now) || now.isAfter(investment.sellDateLimit))
            return true;

        return false;
    }
}
