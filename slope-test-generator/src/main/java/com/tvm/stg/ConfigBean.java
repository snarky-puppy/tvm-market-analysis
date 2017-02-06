package com.tvm.stg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 21/1/17.
 */
public class ConfigBean {

    public Path dataDir;

    // slope items
    public List<IntRange> pointDistances = new ArrayList<>();
    public DoubleRange targetPc = new DoubleRange(10.0, 20.0, 1.0, false);
    public DoubleRange stopPc = new DoubleRange(5.0, 10.0, 1.0, false);
    public IntRange maxHoldDays = new IntRange(30, 50, 2, false);
    public DoubleRange slopeCutoff = new DoubleRange(-0.25, 0.0, 0.1, true);
    public IntRange daysDolVol = new IntRange(30, 60, 10, false);
    public DoubleRange minDolVol = new DoubleRange(10000000.0,100000000.0, 1000000.0, false);
    public IntRange tradeStartDays = new IntRange(10, 30, 5, false);
    public IntRange daysLiqVol = new IntRange(10, 30, 5, false);

    // symbols list
    public List<String> symbols;

    // Compounder
    public int startBank = 200000;
    public int profitRollover = 20000;

    public double minPercent = 0.0;
    public double maxPercent = 50.0;
    public double stepPercent = 10.0;

    public int minSpread = 0;
    public int maxSpread = 25;
    public int stepSpread = 5;

    public int iterations = 10;

    public static abstract class Range<T extends Number & Comparable> {
        T start;
        T end;
        T step;
        boolean isRange = false;

        Range(T start, T end, T step, boolean isRange) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.isRange = isRange;
        }

        public Range() {
        }

        public T getStart() { return start; }
        public T getEnd() { return end; }
        public T getStep() { return step; }
        public boolean getIsRange() { return isRange; }


        public void setStart(T val) {
            start = val;
        }
        public void setEnd(T val) {
            end = val;
        }
        public void setStep(T val) {
            step = val;
        }
        void setIsRange(boolean b) { isRange = b; }

        protected abstract T fromString(String s);
        protected abstract void permute(List<T> list);

        void setStart(String s) { start = fromString(s); }
        void setEnd(String s) { end = fromString(s); }
        void setStep(String s) { step = fromString(s); }

        public List<T> permute() {
            ArrayList<T> rv = new ArrayList<>();
            if (isRange) {
                permute(rv);
            } else {
                rv.add(start);
            }
            return rv;
        }
    }

    public static class DoubleRange extends Range<Double> {
        public DoubleRange() {
            super();
        }
        public DoubleRange(Double start, Double end, Double step, boolean isRange) {
            super(start, end, step, isRange);
        }

        @Override
        public Double fromString(String s) {
            return Double.parseDouble(s);
        }

        @Override
        protected void permute(List<Double> list) {
            for (double d = start; d <= end; d += step)
                list.add(d);

        }
    }

    public static class IntRange extends Range<Integer> {
        public IntRange() { super(); }
        public IntRange(Integer start, Integer end, Integer step, boolean range) {
            super(start, end, step, range);
        }

        @Override
        public Integer fromString(String s) {
            return Integer.parseInt(s);
        }

        @Override
        protected void permute(List<Integer> list) {
            for (int d = start; d <= end; d += step)
                list.add(d);
        }
    }
}
