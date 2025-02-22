package com.tvm.crunch;

/**
 * Created by horse on 27/07/2016.
 */
public class DataException extends Throwable {

    public interface Ignoree {
        void run() throws DataException;
    }

    public static void ignore(Ignoree runnable) {
        try {
            runnable.run();
        } catch(DataException e) {
        }
    }

}
