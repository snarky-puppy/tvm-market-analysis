package com.tvmresearch.lotus.broker;

import com.tvmresearch.lotus.Lotus;
import com.tvmresearch.lotus.LotusException;

import java.util.concurrent.Semaphore;

/**
 * Created by horse on 27/03/2016.
 */
public class ASyncReceiver {
    static Semaphore semaphore = new Semaphore(0);
    static LotusException exception = null;

    public void waitForEvent(){
        try {
            System.out.println("waiting...");
            semaphore.acquire();
            System.out.println("finished waiting");
            if(exception != null) {
                System.out.printf("exxeption");
                LotusException e = exception;
                exception = null;
                throw e;
            }
        } catch (InterruptedException ex) {
        }
    }

    public void eventOccured() {
        System.out.println("Event occured");
        semaphore.release(Integer.MAX_VALUE);

    }

    public static void errorOccured(TWSException e) {
        exception = new LotusException(e);
        semaphore.drainPermits();
    }
}
