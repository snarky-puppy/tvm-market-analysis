package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implement event processing logic
 *
 * Created by matt on 23/03/16.
 */
public class EventProcessor {

    private static final Logger logger = LogManager.getLogger(EventProcessor.class);

    private final Broker broker;
    private final Compounder compounder;

    public EventProcessor(Broker broker, Compounder compounder) {
        this.broker = broker;
        this.compounder = compounder;
    }

    public void processTriggers(List<Trigger> triggerList) {
        triggerList.stream()
                .filter(this::validateTrigger)
                .forEach(this::createBuyOrder);
    }

    private void createBuyOrder(Trigger trigger) {

    }

    public boolean validateTrigger(Trigger trigger) {

        boolean rv = true;

        if(!trigger.event) {
            trigger.rejectReason = Trigger.RejectReason.NOTEVENT;
            rv = false;
        }

        if(trigger.zscore <= Configuration.MIN_ZSCORE) {
            trigger.rejectReason = Trigger.RejectReason.ZSCORE;
            trigger.rejectData = Configuration.MIN_ZSCORE;
            rv = false;
        }

        if(isActiveSymbol(trigger)) {
            trigger.rejectReason = Trigger.RejectReason.CATEGORY;
            rv = false;
        }

        if(trigger.avgVolume > Configuration.MIN_VOLUME) {
            trigger.rejectReason = Trigger.RejectReason.VOLUME;
            trigger.rejectData = Configuration.MIN_VOLUME;
            rv = false;
        }

        double nextInvest = compounder.nextInvestmentAmount();
        double pc = nextInvest / trigger.avgVolume;
        if(pc >= 1.0) {
            trigger.rejectReason = Trigger.RejectReason.INVESTAMT;
            trigger.rejectData = nextInvest;
            rv = false;
        }

        if(!rv) {
            trigger.serialise();
        }
        return rv;
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
}
