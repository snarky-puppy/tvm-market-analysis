package com.tvm.crunch;

/**
 * Created by horse on 21/07/2016.
 */
public abstract class Result {

    public abstract String toString();

    protected void appendDatePricePair(StringBuffer sb, Integer[] date, Double[] price) {
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

    protected void append(StringBuffer sb, String value) {
        if(value == null)
            sb.append(",");
        else
            sb.append(",").append(value);
    }

    protected void append(StringBuffer sb, Double value) {
        if(value == null)
            sb.append(",");
        else
            sb.append(",").append(value);
    }

    protected void append(StringBuffer sb, Integer value) {
        if(value == null)
            sb.append(",");
        else
            sb.append(",").append(value);
    }


    protected void appendPointOpen(StringBuffer sb, Point point) {
        if(point == null) {
            sb.append(",,");
        } else {
            if (point.date != null)
                sb.append(",").append(point.date);
            else
                sb.append(",");
            if (point.open != null)
                sb.append(",").append(point.open);
            else
                sb.append(",");
        }
    }

    protected void appendPointClose(StringBuffer sb, Point point) {
        if(point == null) {
            sb.append(",,");
        } else {
            if (point.date != null)
                sb.append(",").append(point.date);
            else
                sb.append(",");
            if (point.close != null)
                sb.append(",").append(point.close);
            else
                sb.append(",");
        }
    }
}
