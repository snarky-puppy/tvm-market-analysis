package com.tvminvestments.zscore;

/**
 * Created by horse on 16/07/15.
 */
public class RangeBounds {
    private final int min;
    private final int max;
    private final long count;

    public RangeBounds(int min, int max) {
        this.min = min;
        this.max = max;
        count = 0;
    }

    public RangeBounds(int min, int max, long count) {
        this.min = min;
        this.max = max;
        this.count = count;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public long getCount() { return count; }

    @Override
    public String toString() {
        return "RangeBounds{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
