package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.DBUtil;
import com.tvmresearch.lotus.Database;
import com.tvmresearch.lotus.LotusException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Investment db tests
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
    public void serialise() throws Exception {

        TriggerDao triggerDao = new TriggerDaoImpl();
        Trigger trigger = triggerDao.load(1);
        assertNotNull(trigger);

        Investment investment = DBUtil.createInvestment(trigger);
        investment.buyCommission = 0.2;

        InvestmentDao dao = new InvestmentDaoImpl();

        dao.serialise(investment);

        Investment i2 = dao.findUnconfirmed("TST1");
        assertNotNull(i2);

        assertEquals("TST1", i2.trigger.symbol);
        assertEquals(100, i2.buyLimit, 0.2);
        assertEquals(0.2, i2.buyCommission, 0.01);
        assertNull(i2.sellCommission);
    }

    @Test
    public void historicallyCorrect() throws Exception {
        InvestmentDao dao = new InvestmentDaoImpl();
        TriggerDao triggerDao = new TriggerDaoImpl();

        Trigger trigger0 = DBUtil.createTrigger("HIST0");
        triggerDao.serialise(trigger0);
        Investment investment0 = DBUtil.createInvestment(trigger0);
        dao.serialise(investment0);

        Trigger trigger1 = DBUtil.createTrigger("HIST1");
        triggerDao.serialise(trigger1);
        Investment investment1 = DBUtil.createInvestment(trigger1);
        dao.serialise(investment1);

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = Database.connection();
            stmt = connection.prepareStatement(
                    "INSERT INTO investment_history " +
                    "VALUES(NULL, ?, ?, ?);");

            // with some data a while ago
            stmt.setLong(1, investment0.id);
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now().minusDays(42)));
            stmt.setDouble(3, 69.42);
            stmt.executeUpdate();

            Map<LocalDate, Double> history0 = dao.getHistory(investment0);
            assertEquals(1, history0.size());
            assertTrue(history0.containsKey(LocalDate.now().minusDays(42)));
            assertEquals(42, dao.getHistoricalMissingDays(investment0));

            // with some data today
            stmt.setLong(1, investment1.id);
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            stmt.setDouble(3, 69.42);
            stmt.executeUpdate();

            Map<LocalDate, Double> history1 = dao.getHistory(investment1);
            assertEquals(1, history1.size());
            assertTrue(history1.containsKey(LocalDate.now()));
            assertEquals(0, dao.getHistoricalMissingDays(investment1));

        } catch (SQLException e) {
            throw new LotusException(e);
        } finally {
            Database.close(rs, stmt, connection);
        }
    }

    @Test
    public void historicallyCorrect2() throws Exception {
        InvestmentDao dao = new InvestmentDaoImpl();
        TriggerDao triggerDao = new TriggerDaoImpl();

        Trigger trigger = DBUtil.createTrigger("HIST2");
        trigger.date = LocalDate.now();
        triggerDao.serialise(trigger);
        Investment investment = DBUtil.createInvestment(trigger);
        dao.serialise(investment);
        assertEquals(0, dao.getHistoricalMissingDays(investment));
    }

    @Test
    public void historicallyCorrect3() throws Exception {
        InvestmentDao dao = new InvestmentDaoImpl();
        TriggerDao triggerDao = new TriggerDaoImpl();

        Trigger trigger = DBUtil.createTrigger("HIST3");
        trigger.date = LocalDate.now().minusDays(1);
        triggerDao.serialise(trigger);
        Investment investment = DBUtil.createInvestment(trigger);
        dao.serialise(investment);
        assertEquals(1, dao.getHistoricalMissingDays(investment));
    }
}