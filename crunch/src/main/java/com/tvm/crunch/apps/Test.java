package com.tvm.crunch.apps;

import com.tvm.crunch.FileDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by horse on 23/07/2016.
 */
public class Test {

    public static void main(String[] args) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        System.out.println(sdf.format(date));
    }
}
