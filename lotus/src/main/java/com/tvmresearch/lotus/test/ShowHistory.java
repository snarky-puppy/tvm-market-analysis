package com.tvmresearch.lotus.test;

import com.ib.controller.ApiConnection;
import com.ib.controller.ApiController;
import com.ib.controller.Bar;
import com.ib.controller.Types;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by horse on 21/05/2016.
 */
public class ShowHistory {
/*

    public static class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ApiController controller = null;
        ConnectionHandler connectionHandler = new ConnectionHandler();
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); // format for display

        try {
            controller = new ApiController(connectionHandler, new IBLogger(), new IBLogger());

            controller.connect("localhost", 4002, 1);
            connectionHandler.waitForConnection();

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                            .withZone(ZoneId.of("GMT"));

            Instant instant = Instant.now();
            String timeLimit = formatter.format(instant);

            System.out.println("Timelimit="+timeLimit);

            InvestmentDao dao = new InvestmentDaoImpl();
            List<Investment> investments = dao.getPositions();
            for(Investment investment : investments) {

                final String symbol = investment.trigger.symbol;
                final Map<LocalDate, Double> history = new HashMap<>();

                Semaphore semaphore = new Semaphore(1);
                semaphore.acquire();

                ApiController.IHistoricalDataHandler historicalDataHandler = new ApiController.IHistoricalDataHandler() {

                    @Override
                    public void historicalData(Bar bar, boolean hasGaps) {


                        Date dt = new Date(bar.time()*1000);
                        LocalDate date = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                        String s = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));


                        System.out.println(String.format("%s[%d](%s): %f", bar.formattedTime(), bar.time(), s, bar.close()));
                        //LocalDate ld = new LocalDate(bar.time());

                        //history.put(ld, bar.close());

                        if(date.isAfter(investment.trigger.date) || date.isEqual(investment.trigger.date))
                            dao.addHistory(investment, date, bar.close());
                    }

                    @Override
                    public void historicalDataEnd() {
                        semaphore.release();
                    }
                };


                final long days = ChronoUnit.DAYS.between(investment.buyDate, LocalDate.now());

                System.out.println(String.format("%s to %s - %d days",
                        investment.buyDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                        days));

                controller.reqHistoricalData(investment.createNewContract(), timeLimit, (int) days, Types.DurationUnit.DAY,
                        Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);

                semaphore.acquire();
                System.out.println("Finished historical gather");

            }

        } finally {
            controller.disconnect();
        }
    }
    */
}
