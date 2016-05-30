package com.tvmresearch.lotus.db.model;

import com.tvmresearch.lotus.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Map;

/**
 * Created by matt on 30/05/16.
 */
public class InvestmentHistoryDaoImpl implements InvestmentHistoryDao {
    @Override
    public void populateHistory(Investment investment) {
        /*
        Connection connection = Database.connection();

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM ")




        } finally {
            Database.close(stmt, connection);
        }
        */
    }

    @Override
    public void serialise(Investment investment) {

    }

}
