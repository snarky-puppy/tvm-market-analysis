package com.tvm.crunch;

/**
 * Created by horse on 21/07/2016.
 */
public abstract class Result {

    public abstract String toString();

    private void appendDatePricePair(StringBuffer sb, Integer[] date, Double[] price) {
        for(int i = 0; i < date.length; i++) {
            if(date[i] != null)
                sb.append(",").append(date[i]);
            else
                sb.append(",");
            if(price[i] != null)
                sb.append(",").append(price[i]);
            else
                sb.append(",");
        }
    }

    private void appendPoint(StringBuffer sb, Point point) {
            if(point.date != null)
                sb.append(",").append(point.date);
            else
                sb.append(",");
            if(point.price != null)
                sb.append(",").append(point.price);
            else
                sb.append(",");

    }
}
