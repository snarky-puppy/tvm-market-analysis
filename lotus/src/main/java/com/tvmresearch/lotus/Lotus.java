package com.tvmresearch.lotus;

import com.ib.controller.Position;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import com.tvmresearch.lotus.db.model.TriggerDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Program entry point
 *
 * Created by matt on 23/03/16.
 */
public class Lotus {

    private static final Logger logger = LogManager.getLogger(Lotus.class);

    public static void main(String[] args) {
        Lotus lotus = new Lotus();
        lotus.main();
    }

    private void main() {

        logger.info("Starting trigger import");
        ImportTriggers importTriggers = new ImportTriggers();
        importTriggers.importAll();
        logger.info("Finished trigger import");

        Broker broker = null;

        try {
            broker = new InteractiveBroker();

            // Update our DB with any positions that were filled since the last run
            updatePositions(broker, new InvestmentDaoImpl());

            // TODO: after processing all completed sells, we should be able to estimate current cash balance.
            // startBank from 1st of the month + sum of all completed trades
            // TODO: Compare our calculations with what IBKR give us.


            Compounder compounder = new Compounder(broker.getAvailableFunds());
            EventProcessor eventProcessor = new EventProcessor(broker, compounder, new TriggerDaoImpl(), new InvestmentDaoImpl());
            eventProcessor.processTriggers();
            eventProcessor.processInvestments();

        } finally {
            logger.info("The Final final block");
            if(broker != null)
                broker.disconnect();
        }
    }

    public void updatePositions(Broker broker, InvestmentDao dao) {

        List<Position> positions = broker.getOpenPositions();
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



/*
            int brokerQty = position.position();

            List<Investment> buyOrders = investments.stream()
                    .filter(i -> i.state == Investment.State.BUY)
                    .collect(Collectors.toList());
            List<Investment> sellOrders = investments.stream()
                    .filter(i -> i.state == Investment.State.SELL)
                    .collect(Collectors.toList());

            // Process BUY orders first.
            // These since these are always DAY trades, we can be more sure of the correct qty since we'll run
            // after the markets close.
            // myTotalQty will still be higher since we haven't processed any SELL trades yet.
            if(buyOrders != null && buyOrders.size() > 0) {
                int myTotalQty = dao.getQtyFilledSum(position.conid());
                int qtyFilled = brokerQty - myTotalQty;

                // If we have two DAY trades on the same stock, and only the first one gets filled,
                // mark the second as COMPLETE with zero filled.

                for(Investment buyOrder : buyOrders) {
                    if(qtyFilled > 0) {
                        if(qtyFilled > buyOrder.qty) {
                            buyOrder.qtyFilled = buyOrder.qty;
                            qtyFilled -= buyOrder.qty;
                        } else {
                            buyOrder.qtyFilled = qtyFilled;
                            qtyFilled = 0;
                        }

                        buyOrder.qtyFilledValue = position.marketPrice() * buyOrder.qtyFilled;
                        buyOrder.state = Investment.State.FILLED;

                    } else {
                        buyOrder.qtyFilled = 0;
                        buyOrder.qtyFilledValue = 0.0;
                        buyOrder.errorMsg = "Not Filled";
                        buyOrder.state = Investment.State.COMPLETE;
                    }
                }
                dao.serialise(buyOrders);
            }

            // process SELL orders
            if(sellOrders != null && sellOrders.size() > 0) {
                int myTotalQty = dao.getQtyFilledSum(position.conid());
                int sellQty = sellOrders.stream().mapToInt(i -> i.qtyFilled).sum();
                int backLog = myTotalQty - brokerQty;


                for(Investment investment : sellOrders) {

                }
            }
*/
            /*
            1 position, many possible investments

            2 interesting investment states:
                - BUY
                - SELL

            filled sell order:
                single investment:
                    - broker positions == 0
                    - investments in the db but no positions from broker
                many investments:
                    -

            */





        }

    }

}
