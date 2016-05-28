package com.tvmresearch.lotus.broker;

import com.ib.controller.ApiController;
import com.tvmresearch.lotus.LotusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * IB Connection Handler
 *
 * Created by horse on 27/03/2016.
 */
public class ConnectionHandler implements ApiController.IConnectionHandler {

    private static final Logger logger = LogManager.getLogger(ConnectionHandler.class);

    Semaphore semaphore = new Semaphore(0);
    private ArrayList<String> accountList;

    public boolean isConnected(){
        return this.semaphore.availablePermits() != 0;
    }
    public void waitForConnection(){
        try {
            this.semaphore.acquire();
        } catch (InterruptedException ex) {
        }
    }
    @Override
    public void connected() {
        semaphore.release(Integer.MAX_VALUE);//note that if you call
        //waitForConnection a lot this system WILL FAIL
    }

    @Override
    public void disconnected() {
        logger.info("Connection disconnected");
        semaphore.drainPermits();
    }

    @Override
    public void accountList(ArrayList<String> list) {
        accountList = list;
    }

    @Override
    public void error(Exception e) {
        logger.error(e.getMessage(), e);
        System.exit(1);
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        // 399: Warning: your order will not be placed at the exchange until 2016-03-28 09:30:00 US/Eastern
        if(errorCode != 399)
            logger.error(String.format("id=%d, errorCode=%d, msg=%s", id, errorCode, errorMsg));
        if(errorCode < 1100 && errorCode != 399 && errorCode != 202) {
            System.exit(1);
            //throw new LotusException(new TWSException(id, errorCode, errorMsg));
        }
    }

    @Override
    public void show(String string) {
        logger.info("show: "+string);

    }

    public String getAccount() {
        return accountList.get(0);
    }
}
