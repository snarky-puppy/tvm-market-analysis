package com.tvm.crunch;

/**
 * Created by horse on 21/07/2016.
 */
public class Point {
    public Integer date;
    public Double open;
    public Double close;
    public int index = -1;

    public Point(Data data, int idx) {
        index = idx;
        date = data.date[idx];
        open = data.open[idx];
        close = data.close[idx];
    }
}
