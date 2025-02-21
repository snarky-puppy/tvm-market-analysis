package com.tvm.stg;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

/**
 * Wrap market data & search functions
 *
 * Created by horse on 18/11/14.
 */
public class Data {
    private static final Logger logger = LogManager.getLogger(Data.class);

    public int[] date;
    public double[] open;
    //public double[] high;
    //public double[] low;
    public double[] close;
    public long[] volume;
    public String symbol;
    //public double[] openInterest;


    public Data(String symbol, int size) {
        this.symbol = symbol;
        date = new int[size];
        open = new double[size];
        //high = new double[size];
        //low = new double[size];
        close = new double[size];
        volume = new long[size];
        //openInterest = new double[size];
    }
}
