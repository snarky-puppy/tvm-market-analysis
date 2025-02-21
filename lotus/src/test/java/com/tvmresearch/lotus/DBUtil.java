package com.tvmresearch.lotus;

import com.mysql.fabric.xmlrpc.base.Data;
import com.tvmresearch.lotus.db.model.*;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;

import static org.junit.Assert.assertNotNull;

/**
 * Created by horse on 1/06/2016.
 */
public class DBUtil {

    public static void execute(String ...cmd) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);

        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = processBuilder.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(cmd[0]+" returned "+process.exitValue());
    }

    public static void setupDatabase() throws IOException {
        execute("./testdb.sh", "-create");
        DataSource dataSource;
        dataSource = new DataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost/lotustest?useSSL=false");
        dataSource.setUsername("lotustest");
        dataSource.setPassword("lotustest");
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(1);
        dataSource.setMaxIdle(1);
        dataSource.setMinIdle(1);
        dataSource.setLogAbandoned(true);
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(10);
        Database.setDataSource(dataSource);
    }

    public static void dropDatabase() throws IOException {
        execute("./testdb.sh", "-destroy");
    }

    public static void main(String[] args) throws IOException {
        setupDatabase();
        dropDatabase();
    }

    public static Trigger createTrigger(String symbol) {
        Trigger trigger = new Trigger();
        trigger.exchange = "NYSE";
        trigger.symbol = symbol;
        trigger.date = LocalDate.now();
        trigger.price = 42.00;
        trigger.zscore = -2;
        trigger.avgPrice = 42.0;
        trigger.avgVolume = 9000;
        trigger.event = true;
        trigger.rejectReason = Trigger.RejectReason.NOTPROCESSED;

        return trigger;
    }

    public static Investment createInvestment(Trigger trigger) {
        assertNotNull(trigger);

        Investment investment = new Investment(trigger);
        investment.cmpMin = 10000;
        investment.cmpVal = 0;
        investment.cmpTotal = 10000;

        investment.buyLimit = 100;
        investment.qty = 10;
        investment.qtyValue = 10000;

        investment.sellLimit = 110;
        investment.sellDateLimit = LocalDate.now().plusWeeks(6);

        return investment;
    }

}
