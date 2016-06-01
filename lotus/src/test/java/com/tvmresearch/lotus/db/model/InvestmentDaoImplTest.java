package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.DBUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by horse on 1/06/2016.
 */
public class InvestmentDaoImplTest {

    @BeforeClass
    public static void initDB() {
        try {
            DBUtil.setupDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        TriggerDao triggerDao = new TriggerDaoImpl();

        Trigger t1 = DBUtil.createTrigger("TST1");
        Trigger t2 = DBUtil.createTrigger("TST2");
        Trigger t3 = DBUtil.createTrigger("TST3");

        triggerDao.serialise(Arrays.asList(t1, t2, t3));

        t1.rejectReason = Trigger.RejectReason.OK;
        triggerDao.serialise(t1);
    }

    @AfterClass
    public static void destroyDB() {
        try {
            DBUtil.dropDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void getTradesInProgress() throws Exception {

    }

    @Test
    public void getFilledInvestments() throws Exception {

    }

    @Test
    public void serialise() throws Exception {

        TriggerDao triggerDao = new TriggerDaoImpl();
        Trigger trigger = triggerDao.load(1);
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

        InvestmentDao dao = new InvestmentDaoImpl();

        dao.serialise(investment);

        Investment i2 = dao.findUnconfirmed("TST1");
        assertNotNull(i2);

        assertEquals("TST1", i2.trigger.symbol);
        assertEquals(100, i2.buyLimit, 0.2);
    }

    @Test
    public void addHistory() throws Exception {

    }

}