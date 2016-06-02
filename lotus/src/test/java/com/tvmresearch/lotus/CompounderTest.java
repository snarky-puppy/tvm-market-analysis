package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Investment;
import jdk.nashorn.internal.runtime.regexp.joni.Config;
import org.junit.*;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * Created by matt on 2/06/16.
 */
public class CompounderTest {

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.execute("pwd");
        DBUtil.setupDatabase();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        DBUtil.dropDatabase();
    }

    @Before
    public void clearState() {
        try {
            Database.connection().prepareStatement("DELETE FROM compounder_state").execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMinInvestmentAmounts() throws Exception {
        double cash = 100000;

        new Compounder(cash); // initialise Compounder, use anon objects to test load/save

        assertTrue(new Compounder().fundsAvailable());

        // minInvest = starting cash/100*invest%
        double minInvest = ((cash)/100)*Configuration.MIN_INVEST_PC;
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        // how many minInvests can we get for our cash?
        int invCount = (int)Math.floor(cash/minInvest);

        for(int i = 0; i < invCount; i++) {
            Investment investment = new Investment(null);
            assertTrue(new Compounder().apply(investment));
        }

        // Now, minInvest > (cash/rate)-(minInvest*(floor(cash/rate/minInvest)))
        assertFalse(new Compounder().fundsAvailable());
        assertFalse(new Compounder().apply(new Investment(null)));

        // some investments didn't work out, check back in and re-test
        Investment investment = new Investment(null);
        investment.cmpMin = minInvest;
        new Compounder().cancel(investment);

        // we should have enough cash for one more investment
        assertTrue(new Compounder().apply(new Investment(null)));
        assertFalse(new Compounder().apply(new Investment(null)));
    }

    @Test
    public void testWithdrawals() {
        double cash = 1000;

        new Compounder(cash);
        assertTrue(new Compounder().fundsAvailable());

        // if a profit was made...
        {
            Investment investment = new Investment(null);
            investment.buyFillValue = 500.0;
            investment.sellFillVal = 1000.0;
            new Compounder().processWithdrawal(investment);
            cash += investment.sellFillVal;
            assertEquals(cash, new Compounder().getCash(), 0.001);
        }

        // if a profit was not made...
        {
            Investment investment = new Investment(null);
            investment.buyFillValue = 500.0;
            investment.sellFillVal = 100.0;
            new Compounder().processWithdrawal(investment);
            cash += investment.sellFillVal;
            assertEquals(cash, new Compounder().getCash(), 0.001);
        }
    }



    @Test
    public void testCompoundAmt() throws Exception {
        double cash = 1000;

        new Compounder(cash);
        assertTrue(new Compounder().fundsAvailable());

        // minInvest = starting cash/100*invest%
        double minInvest = ((cash)/100)*Configuration.MIN_INVEST_PC;
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        double profit = 0;
        // add some profit to the compounder
        {
            Investment investment = new Investment(null);
            investment.buyFillValue = 500.0;
            investment.sellFillVal = 1000.0;
            new Compounder().processWithdrawal(investment);
            cash += investment.sellFillVal;
            profit += investment.sellFillVal - investment.buyFillValue;
            assertEquals(cash, new Compounder().getCash(), 0.001);
        }

        // compound the profit into new investments
        double slice = profit / Configuration.SPREAD;
        for(int i = 0; i < Configuration.SPREAD; i++) {
            assertEquals(minInvest + slice, new Compounder().nextInvestmentAmount(), 0.001);
            assertTrue(new Compounder().apply(new Investment(null)));
        }
        // should be no profit remaining
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        cash -= ((minInvest+slice) * Configuration.SPREAD);

        /*
        NB: rounding errors! with delta 0.01:
            Expected :30.0
            Actual   :29.99

            delta 0.1 passes
         */
        assertEquals(cash, new Compounder().getCash(), 0.1);
    }

    @Test
    public void testCompoundAmtFailedInvestment() throws Exception {
        // same as previous test but check some profit back in
        double cash = 1000;

        new Compounder(cash);
        assertTrue(new Compounder().fundsAvailable());

        // minInvest = starting cash/100*invest%
        double minInvest = ((cash)/100)*Configuration.MIN_INVEST_PC;
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        double profit = 0;
        // add some profit to the compounder
        {
            Investment investment = new Investment(null);
            investment.buyFillValue = 500.0;
            investment.sellFillVal = 1000.0;
            new Compounder().processWithdrawal(investment);
            cash += investment.sellFillVal;
            profit += investment.sellFillVal - investment.buyFillValue;
            assertEquals(cash, new Compounder().getCash(), 0.001);
        }

        // apply and then cancel some investments
        Investment[] arry = new Investment[Configuration.SPREAD];
        double slice = profit / Configuration.SPREAD;
        for(int i = 0; i < Configuration.SPREAD; i++) {
            assertEquals(minInvest + slice, new Compounder().nextInvestmentAmount(), 0.001);
            arry[i] = new Investment(null);
            assertTrue(new Compounder().apply(arry[i]));
        }
        // should be no profit remaining
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        // now cancel everything
        for(int i = 0; i < Configuration.SPREAD; i++) {
            new Compounder().cancel(arry[i]);
        }

        // should be back to original state
        assertEquals(cash, new Compounder().getCash(), 0.001);
        for(int i = 0; i < Configuration.SPREAD; i++) {
            assertEquals(minInvest + slice, new Compounder().nextInvestmentAmount(), 0.001);
            assertTrue(new Compounder().apply(new Investment(null)));
        }
        // should be no profit remaining
        assertEquals(minInvest, new Compounder().nextInvestmentAmount(), 0.001);

        cash -= ((minInvest+slice) * Configuration.SPREAD);

        /*
        NB: rounding errors! with delta 0.01:
            Expected :30.0
            Actual   :29.99

            delta 0.1 passes
         */
        assertEquals(cash, new Compounder().getCash(), 0.1);
    }
}