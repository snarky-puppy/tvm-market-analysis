package com.tvm.crunch;

import java.io.IOException;

/**
 * Created by horse on 28/07/2016.
 */
public class Util {
    public static void waitForKeypress(boolean debug) {

        if(!debug)
            return;

        System.out.println("Waiting...");
        try {
            int n = System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Starting");
    }
}
