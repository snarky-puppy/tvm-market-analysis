package com.tvmresearch.lotus;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by horse on 23/03/2016.
 */
public class Test {

    private static void test(int i) {
        System.out.println(i);
    }


    private static double round(double num) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static void main(String[] args) {


        String msg = "reason:\nsomething something\nsomething";

        System.out.println(msg.replaceAll("\n", ":::"));

    }
}
