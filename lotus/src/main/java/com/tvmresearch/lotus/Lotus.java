package com.tvmresearch.lotus;

import com.tvmresearch.lotus.broker.*;
import com.tvmresearch.lotus.db.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Program entry point
 *
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);

    private Broker broker;
    private TriggerDao triggerDao;
    private InvestmentDao investmentDao;
    private Compounder compounder;
    private ArrayBlockingQueue<IBMessage> eventQueue = new ArrayBlockingQueue<>(1024);
    private ImportTriggers importTriggers = new ImportTriggers();
    private LocalTime fxUpdateTS = null;

    private int buyOrderCount = 0;

    private static volatile boolean running = true;

    public static void main(String[] args) {
        Lotus lotus = new Lotus();
        lotus.mainLoop();
    }

    public Lotus() {
        triggerDao = new TriggerDaoImpl();
        investmentDao = new InvestmentDaoImpl();
        //compounder = new Compounder();
    }

    private void mainLoop() {

        LocalTime fxCheck = LocalTime.now();

        try {
            broker = new InteractiveBroker(eventQueue);
            compounder = new Compounder(broker.getAvailableFunds(), broker.getExchangeRate());
            fxUpdateTS = LocalTime.now(); // update FX check timestamp since we just fetched it

            // threads....
            //  1. ensure ibgateway is running
            //  2. periodically update AUD.USD
            //  3.

            while(running) {
                updateFX();
                processTriggers();
                processEvents();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {

        } finally {
            logger.info("The Final final block");
            if(broker != null)
                broker.disconnect();
        }
    }

    private void updateFX() {
        // update every hour should be enough
        if(fxUpdateTS == null || fxUpdateTS.plusHours(1).isAfter(LocalTime.now())) {
            fxUpdateTS = LocalTime.now();
            double fx = broker.getExchangeRate();
            double cash = broker.getAvailableFunds();
            compounder.updateCashAndRate(cash, fx);
        }
    }

    private void processTriggers() {
        importTriggers.importAll();
        List<Trigger> triggerList = triggerDao.getTodaysTriggers();
        for(Trigger trigger : triggerList) {
            if (!validateTrigger(trigger)) {
                triggerDao.serialise(trigger);
                continue;
            }
            Investment investment = new Investment(trigger);

            if (!compounder.apply(investment)) {
                logger.error("failed to apply compounding: " + investment);
            } else {

                investment.buyLimit = round(investment.trigger.price * Configuration.BUY_LIMIT_FACTOR);
                investment.buyDate = LocalDate.now();

                investment.qty = (int) Math.floor(investment.cmpTotal / investment.buyLimit);
                investment.qtyValue = investment.qty * investment.buyLimit;

                investment.sellLimit = round(investment.trigger.price * Configuration.SELL_LIMIT_FACTOR);

                investment.sellDateLimit = investment.buyDate.plusDays(Configuration.SELL_LIMIT_DAYS);

                broker.buy(investment);
                buyOrderCount ++;
            }
            investmentDao.serialise(investment);
            triggerDao.serialise(trigger);
        }
    }

    private void processEvents() throws InterruptedException {

        IBMessage msg = null;
        while((msg = eventQueue.poll(1, TimeUnit.SECONDS)) != null) {
            msg.process(compounder, triggerDao, investmentDao);
        }
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


/*
    public void updatePositions(Broker broker, InvestmentDao dao) {

        Collection<Position> positions = broker.getOpenPositions();
        for (Position position : positions) {


            List<Investment> investments = dao.getTradesInProgress(position.conid());
            if(investments.size() == 0) {
                //logger.error("No open trades for position: "+position);
                continue;
            }

            if(investments.size() > 1) {
                logger.error("Can't handle more than one trade in progress per instrument: "+investments.get(0).trigger.symbol);
                continue;
            }

            Investment investment = investments.get(0);

            // any left over BUYs are COMPLETE (completely unfilled)
            if(investment.state == Investment.State.BUY && investment.qtyFilled == null) {
                investment.state = Investment.State.COMPLETE;
                investment.errorMsg = "Unfilled";
                dao.serialise(investment);
            }

            // filled SELL order
            if(position.position() == 0 && (investment.sellDateEnd == null)) {
                // completed SELL order
                investment.sellDateEnd = LocalDate.now();
                investment.realPnL = position.realPnl();
                investment.sellPrice = position.marketPrice();
                investment.state = Investment.State.COMPLETE;
                dao.serialise(investment);

            }

            // filled BUY order: qty_filled & qty_filled_val
            if(position.position() > 0 && (investment.qtyFilled == null || investment.qtyFilledValue == null)) {
                investment.qtyFilled = position.position();
                investment.qtyFilledValue = position.marketValue();
                investment.state = Investment.State.FILLED;
                dao.serialise(investment);
            }
        }

    }
*/

    private double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
