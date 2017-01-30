package com.tvm.stg;

import sun.misc.FDBigInteger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 21/1/17.
 */
public class ConfigBean {
    public String configName;

    // slope items
    public List<IntRange> pointDistances = new ArrayList<>();
    public DoubleRange targetPc = new DoubleRange(10, 20, 1);
    public DoubleRange stopPc = new DoubleRange(5, 10, 1);
    public DoubleRange maxHoldDays = new DoubleRange(30, 50, 2);
    public DoubleRange slopeCutoff = new DoubleRange(-0.1, -0.3, 0.1);
    public IntRange daysDolVol = new IntRange(30, 60, 10);
    public DoubleRange minDolVol = new DoubleRange(1000000,10000000, 1000000);
    public IntRange tradeStartDays = new IntRange(10, 30, 5);
    public IntRange daysLiqVol = new IntRange(10, 30, 5);

    static interface Range<T> {
        T getStart();
        T getEnd();
        T getStep();

        void setStart(String s);
        void setEnd(String s);
        void setStep(String s);
    }

    static class DoubleRange implements Range<Double> {
        private double start;
        private double end;
        private double step;

        public DoubleRange() {
        }

        public DoubleRange(double start, double end, double step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }




        public Double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public Double getEnd() {
            return end;
        }

        @Override
        public Double getStep() {
            return step;
        }

        @Override
        public void setStart(String s) {
            start = Double.parseDouble(s);
        }

        @Override
        public void setEnd(String s) {
            end = Double.parseDouble(s);
        }

        @Override
        public void setStep(String s) {
            step = Double.parseDouble(s);
        }

        public void setEnd(double end) {
            this.end = end;
        }

        public void setStep(double step) {
            this.step = step;
        }
    }

    static class IntRange implements Range<Integer> {
        private int start;
        private int end;
        private int step;

        public IntRange() {
        }

        IntRange(int start, int end, int step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }

        public Integer getStart() { return start; }
        public Integer getEnd() { return end; }

        @Override
        public Integer getStep() {
            return step;
        }

        @Override
        public void setStart(String s) {
            start = Integer.parseInt(s);
        }

        @Override
        public void setEnd(String s) {
            end = Integer.parseInt(s);
        }

        @Override
        public void setStep(String s) {
            step = Integer.parseInt(s);
        }

        public void setStart(int start) {
            this.start = start;
        }

        public void setEnd(int end) {
            this.end = end;
        }
        public void setStep(int step) {
            this.step = step;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("IntRange{");
            sb.append("start=").append(start);
            sb.append(", end=").append(end);
            sb.append(", step=").append(step);
            sb.append('}');
            return sb.toString();
        }
    }
}
