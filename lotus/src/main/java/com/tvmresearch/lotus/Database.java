    package com.tvmresearch.lotus;

import com.tvmresearch.lotus.db.HibernateUtil;
import com.tvmresearch.lotus.db.model.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Calendar;
import java.util.List;

    /**
 * Database interface
 *
 * Created by horse on 19/03/2016.
 */
public class Database {

        private static final Logger logger = LogManager.getLogger(Database.class);

    public Database() {
        ensureSchema();
    }



    private void ensureSchema() {

    }

    public static void main(String[] args) {
        try {
            Trigger data = new Trigger();
            data.date = Calendar.getInstance().getTime();
            data.closePrice = 123.123;
            data.exchange = "ASX";
            data.symbol = "TVM";
            data.avgPrice = 1.0;
            data.avgVolume = 2.0;

            try {
                Session session = HibernateUtil.getSessionFactory().openSession();
                session.beginTransaction();
                session.save(data);
                session.getTransaction().commit();
                session.close();
            } catch(ConstraintViolationException e) {
                logger.error("*********************** SAVE EXCEPTION", e);
            }

            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            List result = session.createQuery("from Trigger").list();
            for (Trigger t : (List<Trigger>) result) {
                System.out.println("RESULT: "+t);
            }
            session.getTransaction().commit();
            session.close();


        } finally {
            HibernateUtil.getSessionFactory().close();
        }

    }
}
