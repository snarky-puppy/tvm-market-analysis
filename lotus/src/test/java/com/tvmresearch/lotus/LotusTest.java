package com.tvmresearch.lotus;

import com.ib.client.Contract;
import com.ib.controller.NewContract;
import com.ib.controller.Position;
import com.tvmresearch.lotus.broker.Broker;
import com.tvmresearch.lotus.broker.OpenOrder;
import com.tvmresearch.lotus.db.model.Investment;
import com.tvmresearch.lotus.db.model.InvestmentDao;
import com.tvmresearch.lotus.db.model.InvestmentDaoImpl;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by horse on 7/04/2016.
 */
public class LotusTest {

    class TestUpdatePositionsBroker implements Broker {

        List<Position> openPositions = new ArrayList<>();

        @Override
        public double getAvailableFunds() {
            return 0;
        }

        @Override
        public boolean buy(Investment investment) {
            return false;
        }

        @Override
        public void sell(Investment investment) {

        }

        @Override
        public List<Position> getOpenPositions() {
            return openPositions;
        }

        @Override
        public void disconnect() {

        }

        @Override
        public double getLastClose(Investment investment) {
            return 0;
        }

        @Override
        public void updateHistory(InvestmentDao dao, Investment investment) {

        }

    }

    Position createPosition(int position, double marketPrice) {
        Contract con = new Contract();
        NewContract contract = new NewContract(con);
        Position p = new Position(contract, "ACC", position, marketPrice, 0, 0, 0, 0);
        return p;
    }


    @org.junit.Test
    public void testUpdatePositions() throws Exception {
        Lotus lotus = new Lotus();

        /*
        TestUpdatePositionsBroker broker = new TestUpdatePositionsBroker();

        InvestmentDao dao  = new InvestmentDao() {
            @Override
            public int getQtyFilledSum(int conid) {
                return 0;
            }

            @Override
            public List<Investment> getTradesInProgress(int conid) {
                return null;
            }

            @Override
            public void serialise(List<Investment> investments) {

            }

            @Override
            public void serialise(Investment investment) {

            }
        };


        lotus.updatePositions(broker, dao);
        */
    }

    /* tests:
    no positions and no investments
    positions but no investments
    single buy order
    single sell order
    multi buy order
    multi sell order
    multi buy single sell
    single buy multi sell
     */

}