package com.tvmresearch.lotus;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

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


        double inf = 1.7976931348623157E308;

        if(inf > 99999) {
            System.out.println("Infinite");
        } else
            System.out.println("not infinite");

        System.out.println(round(inf));

        double value = 1.7976931;

        System.out.println(Math.floor(value * 100) / 100);

    }
}
