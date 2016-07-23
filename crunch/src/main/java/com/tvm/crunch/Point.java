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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Point{");
        sb.append("date=").append(date);
        sb.append(", open=").append(open);
        sb.append(", close=").append(close);
        sb.append(", index=").append(index);
        sb.append('}');
        return sb.toString();
    }
}
