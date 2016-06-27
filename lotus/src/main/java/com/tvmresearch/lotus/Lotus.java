package com.tvmresearch.lotus;

import com.ib.client.CommissionReport;
import com.ib.client.Execution;
import com.ib.controller.*;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.db.model.*;
import com.tvmresearch.lotus.message.IBMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.tvmresearch.lotus.db.model.Investment.State.*;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Program entry point
 * <p>
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);
    private static final int sellCheckHour = 13; // 2pm local == 12pm EST
    private static final int sellCheckMinute = 10; // 12:10AM EST
    private static final int historyUpdateHour = 11; // 12pm local == 10pm EST
    private static final int cashUpdateHours = 1;
    public static volatile boolean intentionalShutdown = false;
    public volatile boolean running = true;
    private Broker broker;
    private TriggerDao triggerDao;
    private InvestmentDao investmentDao;
    private Compounder compounder;
    private ArrayBlockingQueue<IBMessage> eventQueue = new ArrayBlockingQueue<>(1024);
    private ImportTriggers importTriggers = new ImportTriggers();
    private LocalDateTime cashUpdateTS = null;
    private LocalDateTime nextHistoryUpdateTS = null;
    private LocalDateTime nextSellCheckTS = null;

    public Lotus() {
        triggerDao = new TriggerDaoImpl();
        investmentDao = new InvestmentDaoImpl();
    }

    public static void main(String[] args) throws InterruptedException {

        try {
            while (!intentionalShutdown) {
                logger.info("--- LOTUS STARTUP ---");
                Lotus lotus = new Lotus();
                lotus.mainLoop();
                logger.info("--- LOTUS RECONNECT ---");
                Thread.sleep(10000);
            }
            logger.info("--- LOTUS SHUTDOWN ---");

        } catch(LotusException e) {
            logger.error("Fatal Lotus exception", e);

        } catch(Exception e) {
            logger.error("Fatal uncaught exception", e);
        }
    }

    private void mainLoop() {

        try {
            broker = new InteractiveBroker(eventQueue);
            running = true;
            compounder = new Compounder(broker.getAvailableFundsUSD() - investmentDao.sumOfOutstandingBuyOrders());
            cashUpdateTS = LocalDateTime.now().plusHours(cashUpdateHours); // update timestamp since we just fetched it

            while (running) {
                updateCash();
                processTriggers();
                updateHistory();
                processEvents();
                doSellCheck();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.info("Sleep interrupted", e);

        } catch (ConnectException e) {
            logger.info("Not connected to IB");

        } finally {
            logger.info("The Final final block");
            if (broker != null)
                broker.disconnect();
        }
    }

    private void doSellCheck() {
        boolean run = false;

        if (nextSellCheckTS == null) {
            // first update
            // if before the true update hour, schedule for true update hour later today
            if (LocalDateTime.now().getHour() < sellCheckHour) {
                nextSellCheckTS = LocalDateTime.now().withHour(sellCheckHour).withMinute(sellCheckMinute).withSecond(0);
            } else {
                // after the true update hour, schedule for true update hour tomorrow
                nextSellCheckTS = LocalDateTime.now().withHour(sellCheckHour).withMinute(sellCheckMinute).withSecond(0).plusDays(1);
            }

            // either way, update now
            run = true;

        } else if (LocalDateTime.now().isAfter(nextSellCheckTS)) {
            // business as usual
            nextSellCheckTS = LocalDateTime.now().withHour(sellCheckHour).withMinute(sellCheckMinute).withSecond(0).plusDays(1);
            run = true;
        }

        if (run) {
            for (Investment investment : investmentDao.getPositions()) {
                double close = investmentDao.getLastHistoricalClose(investment);
                boolean sellLimitExceed = false;
                if (close > 0) {
                    sellLimitExceed = close >= investment.sellLimit;
                    double remaining = investment.sellLimit - close;
                    logger.info(String.format("doSellCheck: %s/%s: lastClose[%.2f] >= sellLimit[%.2f]? %s",
                            investment.trigger.exchange, investment.trigger.symbol, close, investment.sellLimit,
                            sellLimitExceed ? "Yes" : String.format("No (%.2f remaining)", remaining)));
                }
                boolean dtLimitExceeded = LocalDate.now().isAfter(investment.sellDateLimit)
                        || LocalDate.now().isEqual(investment.sellDateLimit);

                long remaining = DAYS.between(LocalDate.now(), investment.sellDateLimit);

                logger.info(String.format("doSellCheck: %s/%s: now >= sellDateLimit[%s]? %s",
                        investment.trigger.exchange, investment.trigger.symbol,
                        investment.sellDateLimit.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        dtLimitExceeded ? "Yes" : "No (" + remaining + " days remaining)"));

                if (dtLimitExceeded || sellLimitExceed) {
                    broker.sell(investment);
                    // messages from ibkr are queued through a pipe, so no possiblity of race here
                    investment.state = SELLUNCONFIRMED;
                    investmentDao.serialise(investment);
                }
            }
        }
    }

    private void updateHistory() {

        boolean run = false;

        if (nextHistoryUpdateTS == null) {
            // first update
            // if before the true update hour, schedule for true update hour later today
            if (LocalDateTime.now().getHour() < historyUpdateHour) {
                nextHistoryUpdateTS = LocalDateTime.now().withHour(historyUpdateHour).withMinute(0).withSecond(0);
            } else {
                // after the true update hour, schedule for true update hour tomorrow
                nextHistoryUpdateTS = LocalDateTime.now().withHour(historyUpdateHour).withMinute(0).withSecond(0).plusDays(1);
            }

            // either way, update now
            run = true;

        } else if (LocalDateTime.now().isAfter(nextHistoryUpdateTS)) {
            // business as usual
            nextHistoryUpdateTS = LocalDateTime.now().withHour(historyUpdateHour).withMinute(0).withSecond(0).plusDays(1);
            run = true;
        }

        if (run) {
            for (Investment investment : investmentDao.getPositions()) {
                int missingDays = investmentDao.getHistoricalMissingDays(investment);
                if (missingDays > 0)
                    broker.updateHistory(investmentDao, investment, missingDays);
            }
        }
    }

    private void updateCash() {

        int outstandingBuyOrders = investmentDao.outstandingBuyOrders();
        if ((cashUpdateTS == null || LocalDateTime.now().isAfter(cashUpdateTS)) && outstandingBuyOrders == 0) {
            cashUpdateTS = LocalDateTime.now().plusHours(cashUpdateHours);
            double brokerCash = broker.getAvailableFundsUSD();
            double compoundCash = compounder.getCash();
            double diff = compoundCash - brokerCash;
            if (diff < 0)
                diff = -diff;
            logger.info(String.format("updateCash: %.2f difference (ib=%.2f compounder=%.2f) buyOrders=%d",
                    diff, brokerCash, compoundCash, outstandingBuyOrders));

            compounder.setCash(brokerCash);
        }
    }

    private void processTriggers() {
        importTriggers.importAll();
        List<Trigger> triggerList = triggerDao.getTodaysTriggers();
        for (Trigger trigger : triggerList) {
            if (!validateTrigger(trigger)) {
                triggerDao.serialise(trigger);
                continue;
            }
            logger.info("processTriggers: " + trigger);
            Investment investment = new Investment(trigger);

            if (!compounder.apply(investment)) {
                logger.error("processTriggers: failed to apply compounding: " + investment);
            } else {

                investment.state = BUYUNCONFIRMED;

                investment.buyLimit = round(investment.trigger.price * Configuration.BUY_LIMIT_FACTOR);
                investment.qty = (int) Math.floor(investment.cmpTotal / investment.buyLimit);
                investment.qtyValue = investment.qty * investment.buyLimit;

                investment.sellLimit = round(investment.trigger.price * Configuration.SELL_LIMIT_FACTOR);
                investment.sellDateLimit = investment.buyDate.plusDays(Configuration.SELL_LIMIT_DAYS);

                broker.buy(investment);
            }
            investmentDao.serialise(investment);
            triggerDao.serialise(trigger);
        }
    }

    public boolean validateTrigger(Trigger trigger) {

        if (!trigger.event) {
            trigger.rejectReason = Trigger.RejectReason.NOTEVENT;
            return false;
        }

        if (trigger.zscore > Configuration.MIN_ZSCORE) {
            trigger.rejectReason = Trigger.RejectReason.ZSCORE;
            trigger.rejectData = Configuration.MIN_ZSCORE;
            return false;
        }

        if (!isActiveSymbol(trigger)) {
            trigger.rejectReason = Trigger.RejectReason.CATEGORY;
            return false;
        }

        if (trigger.avgVolume > Configuration.MIN_VOLUME) {
            trigger.rejectReason = Trigger.RejectReason.VOLUME;
            trigger.rejectData = Configuration.MIN_VOLUME;
            return false;
        }

        double nextInvest = compounder.nextInvestmentAmount();
        double pc = nextInvest / trigger.avgVolume;
        if (pc >= 1.0) {
            trigger.rejectReason = Trigger.RejectReason.INVESTAMT;
            trigger.rejectData = nextInvest;
            return false;
        }

        if (!compounder.fundsAvailable()) {
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
            if (rs.next()) {
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


    private double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    private void processEvents() throws InterruptedException {
        IBMessage msg;
        while ((msg = eventQueue.poll(1, TimeUnit.SECONDS)) != null) {
            msg.process(this);
        }
    }

    public void processPosition(Position position) {
        Investment investment = investmentDao.findConId(position.conid());
        if (investment == null) {
            logger.info("Position not found: " + position.contract().symbol() + "/" + position.contract().exchange());
            return;
        }

        switch (investment.state) {
            case BUYUNCONFIRMED:
            case BUYPRESUBMITTED:
            case BUYOPEN:
                logger.warn("Missed BUY order confirmation: " + investment.trigger.symbol);
                investment.state = BUYFILLED;
                investment.qtyFilled = position.position();
                investment.buyDate = LocalDate.now();
                investment.buyFillValue = position.marketValue();
                investmentDao.serialise(investment);
                break;
            case BUYFILLED:
                investment.marketPrice = position.marketPrice();
                investment.marketValue = position.marketValue();
                investment.avgCost = position.averageCost();
                investment.realPnL = position.realPnl();
                investmentDao.serialise(investment);
                break;
            case SELLUNCONFIRMED:
            case SELLPRESUBMITTED:
            case SELLOPEN:
                if (position.position() == 0) {
                    logger.warn("Missed SELL order confirmation: " + investment.trigger.symbol);
                    investment.state = SELLFILLED;
                    investment.errorCode = 69;
                    investment.errorMsg = "Missed SELL order confirmation";
                    investment.sellDateEnd = LocalDate.now();
                    investmentDao.serialise(investment);
                }
                break;
            case SELLFILLED:
                if (position.position() == 0) {
                    investment.state = CLOSED;
                    compounder.processWithdrawal(investment);
                    investmentDao.serialise(investment);
                }
                break;
            case CLOSED:
            case ORDERFAILED:
            case ERROR:
                break;
        }

        /**
         * IB went down before midnight and we lost the result of the order
         *
         * The question is can we process it here? Does the Position come in before the orderStatus normally???
         *
         * Position comes after the Filled status, so if a position appears for an invstment status < BUYFILLED we can change to BUYFILLED.
         *
         */


    }

    public void processOpenOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
        Investment investment = investmentDao.findOrder(order.orderId());

        if (investment == null || order.orderId() == 0) {
            logger.warn("processOpenOrder: unknown orderId");
            return;
        }

        /*
        @contract the same during repeated calls
        @order the same during repeated calls
        @orderState:
            status=PreSubmitted
            status=Submitted
            status=Filled
                commission changes

          variation:
           order.action
           order.commission
         */

        switch (orderState.status()) {
            case PreSubmitted:
                if (order.action() == Types.Action.BUY) {
                    investment.buyPermId = order.permId();
                    investment.state = BUYPRESUBMITTED;
                    investment.conId = contract.conid();
                } else {
                    investment.state = SELLPRESUBMITTED;
                    investment.sellPermId = order.permId();
                }
                investmentDao.serialise(investment);
                break;
            case Submitted:
                if (order.action() == Types.Action.BUY) {
                    investment.state = BUYOPEN;
                } else {
                    investment.state = SELLOPEN;
                    investment.sellDateStart = LocalDate.now();
                }
                investmentDao.serialise(investment);
                break;
            case Filled:
                if (order.action() == Types.Action.BUY) {
                    investment.state = BUYFILLED;
                    investment.buyCommission = truncDouble(orderState.commission());
                } else {
                    investment.state = SELLFILLED;
                    investment.sellCommission = truncDouble(orderState.commission());
                }
                investmentDao.serialise(investment);
                break;
            case Cancelled:
            case ApiCancelled:
                break;

            default:
                logger.error("processOpenOrder: undefined order status: " + orderState.status());
                break;
        }


    }

    public void processTradeReport(String tradeKey, NewContract contract, Execution execution) {

    }

    public void processTradeCommissionReport(String tradeKey, CommissionReport commissionReport) {

    }

    public void processOrderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        // This method is called whenever the status of an order changes.
        // It is also fired after reconnecting to TWS if the client has any open orders.
        // https://www.interactivebrokers.com/en/software/api/apiguide/java/orderstatus.htm

        Investment investment = investmentDao.findOrder(orderId);
        if (investment == null || orderId == 0) {
            logger.warn("processOrderStatus: unknown orderId");
            return;
        }
        /*

        status=Submitted
            filled will increase until remaining = 0

        status=Filled
            same number of events as there were for status=Submitted
            avgFillPrice gives it, I think

         */

        switch (status) {
            case Filled:
                switch (investment.state) {
                    case BUYUNCONFIRMED:
                    case BUYPRESUBMITTED:
                    case BUYOPEN:
                    case BUYFILLED:
                        investment.state = BUYFILLED;
                        investment.qtyFilled = filled;
                        investment.buyDate = LocalDate.now();
                        investment.buyFillValue = filled * avgFillPrice;
                        investmentDao.serialise(investment);
                        break;
                    case SELLUNCONFIRMED:
                    case SELLPRESUBMITTED:
                    case SELLOPEN:
                        if (remaining == 0) {
                            investment.state = SELLFILLED;
                            investment.sellDateEnd = LocalDate.now();
                        }
                        investment.sellFillVal = filled * avgFillPrice;
                        investment.avgSellPrice = avgFillPrice;
                        investmentDao.serialise(investment);
                        break;
                    case SELLFILLED:
                    default:
                        logger.error("Weird state: " + investment);
                        investment.state = ERROR;
                        investmentDao.serialise(investment);
                        break;
                }
                break;
            case Cancelled:
            case ApiCancelled:
            case PreSubmitted:
            case Submitted:
                break;

            default:
                logger.error("processOrderStatus: undefined order status: " + status);
                break;
        }
    }

    public void processOrderError(int orderId, int errorCode, String errorMsg) {
        /*
        399: Warning: your order will not be placed at the exchange until 2016-05-31 09:30:00 US/Eastern

         */
        logger.info(String.format("processOrderError: id=%d code=%d msg=%s", orderId, errorCode, errorMsg));
        switch (errorCode) {
            case 1100: // Connectivity between IB and Trader Workstation has been lost.
            case 399: // Warning: your order will not be placed at the exchange until 2016-05-31 09:30:00 US/Eastern
                return;

            default:
                if (orderId > 0) {
                    Investment investment = investmentDao.findOrder(orderId);
                    if (investment != null) {
                        compounder.cancel(investment);
                        investment.state = Investment.State.ORDERFAILED;
                        investment.errorMsg = errorMsg;
                        investment.errorCode = errorCode;
                        investmentDao.serialise(investment);
                    }
                }

        }
    }

    public void processDisconnect() {
        running = false;
    }

    private double truncDouble(double d) {
        if (d > 999999999)
            return 0;
        return Math.floor(d * 1000) / 1000;
    }
}
