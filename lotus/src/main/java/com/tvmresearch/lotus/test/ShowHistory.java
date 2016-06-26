package com.tvmresearch.lotus.test;

import com.ib.controller.*;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.IBLogger;
import com.tvmresearch.lotus.broker.InteractiveBroker;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by horse on 21/05/2016.
 */
public class ShowHistory {

    public static class IBLogger implements ApiConnection.ILogger {
        @Override
        public void log(String string) {
            //logger.info("IB: "+string);
        }
    }
    private static void log(String s) {
        System.out.println(s);
    }

    private static void log(String fmt, Object ...o) {
        System.out.println(String.format(fmt, o));
    }

    static String account = null;


    public static void main(String[] args) throws InterruptedException {
        ApiController controller = null;
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd"); // format for display


        try {
            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();

            controller = new ApiController(new ApiController.IConnectionHandler() {

                @Override
                public void connected() {
                    //semaphore.release();
                    log("Connected");
                }

                @Override
                public void disconnected() {

                }

                @Override
                public void accountList(ArrayList<String> list) {
                    if(account == null) {
                        account = list.get(0);
                        log("Got account: "+account);
                        semaphore.release();
                    }
                }

                @Override
                public void error(Exception e) {
                    log("error: "+e.getMessage(), e);
                    System.exit(1);
                }

                @Override
                public void message(int id, int errorCode, String errorMsg) {
                    // 399: Warning: your order will not be placed at the exchange until 2016-03-28 09:30:00 US/Eastern
                    if(errorCode != 399)
                        log(String.format("message: id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
                    if(errorCode < 1100 && errorCode != 162 && errorCode != 399 && errorCode != 202) {
                        System.exit(1);
                        //throw new LotusException(new TWSException(id, errorCode, errorMsg));
                    }
                    if(errorCode == 162)
                        semaphore.release();
                }

                @Override
                public void show(String string) {
                    log("show: "+string);
                }
            }, new com.tvmresearch.lotus.broker.IBLogger(), new com.tvmresearch.lotus.broker.IBLogger());


            controller.connect("localhost", 4002, 1);

            semaphore.acquire();


            ApiController.IHistoricalDataHandler historicalDataHandler = new ApiController.IHistoricalDataHandler() {

                int cnt = 0;

                @Override
                public void historicalData(Bar bar, boolean hasGaps) {


                    Date dt = new Date(bar.time()*1000);
                    LocalDate date = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                    String s = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));

                    System.out.println(String.format("%d: %s[%d](%s): %f", ++cnt, bar.formattedTime(), bar.time(), s, bar.close()));
                }

                @Override
                public void historicalDataEnd() {
                    semaphore.release();
                }
            };



            NewContract contract = new NewContract();
            contract.symbol("PRGO");
            contract.exchange("SMART");
            contract.primaryExch("NYSE");
            contract.secType(com.ib.controller.Types.SecType.STK);

            int missingDays = 70;

            final int maxDays = 30;
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss zzz")
                            .withZone(ZoneId.of("GMT"));



            do {
                int fetchedDays = missingDays;
                LocalDate end = LocalDate.now();

                if(missingDays > maxDays) {
                    end = end.minusDays(missingDays - maxDays);
                    fetchedDays = maxDays;
                }

                String timeLimit = formatter.format(end.atStartOfDay());
                System.out.println("Timelimit="+timeLimit);

                controller.reqHistoricalData(contract, timeLimit, fetchedDays, Types.DurationUnit.DAY,
                        Types.BarSize._1_day, Types.WhatToShow.TRADES, true, historicalDataHandler);
                semaphore.acquire();

                missingDays -= fetchedDays;

            } while(missingDays > 0);



            System.out.println("Finished historical gather");


        } finally {
            controller.disconnect();
        }
    }

}
