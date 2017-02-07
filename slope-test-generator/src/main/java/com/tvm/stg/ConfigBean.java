package com.tvm.stg;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @JsonIgnore
    public List<List<Integer>> getPointRanges() {
        if(pointDistances.size() == 0)
            return new ArrayList<>();

        // copy array so original is unmolested
        return getPointRangesPrivate(new ArrayList<>(pointDistances));
    }


    private List<List<Integer>> getPointRangesPrivate(List<IntRange> pointDistances) {

        if(pointDistances.size() == 1) {
            List<Integer> list = pointDistances.get(0).permute();
            List<List<Integer>> rv = new ArrayList<>();
            for(int x : list) {
                ArrayList<Integer> l = new ArrayList<>();
                l.add(x);
                rv.add(l);
            }

            return rv;
        }

        IntRange range = pointDistances.remove(0);
        List<List<Integer>> permutations = getPointRangesPrivate(pointDistances);
        List<List<Integer>> rv = new ArrayList<>();

        List<Integer> rangePerms = range.permute();

        if(rangePerms.size() > permutations.size()) {
            for(int x : rangePerms) {
                for(List<Integer> smallerPerm : permutations) {
                    ArrayList<Integer> list = new ArrayList<>();
                    list.add(x);
                    list.addAll(smallerPerm);
                    rv.add(list);
                }

            }

        } else {
            for(List<Integer> biggerPerm : permutations) {
                for(int x : rangePerms) {
                    ArrayList<Integer> list = new ArrayList<>();
                    list.addAll(biggerPerm);
                    list.add(x);
                    rv.add(list);
                }
            }
        }

        return rv;
    }

}
