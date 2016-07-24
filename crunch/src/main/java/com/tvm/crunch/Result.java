package com.tvm.crunch;

/**
 * Created by horse on 21/07/2016.
 */
public abstract class Result {

    public abstract String toString();
    public abstract String getHeader();

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
