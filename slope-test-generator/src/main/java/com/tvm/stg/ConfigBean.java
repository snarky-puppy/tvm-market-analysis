package com.tvm.stg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 21/1/17.
 */
public class ConfigBean {
    public String configName;
    public List<IntRange> pointDistances = new ArrayList<>();



    static class DoubleRange {
        private double start;
        private double end;

        public DoubleRange() {
        }

        public DoubleRange(double start, double end) {
            this.start = start;
            this.end = end;
        }

        public double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public double getEnd() {
            return end;
        }

        public void setEnd(double end) {
            this.end = end;
        }
    }

    static class IntRange {
        private int start;
        private int end;

        public IntRange() {
        }

        IntRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        int getStart() { return start; }
        int getEnd() { return end; }

        public void setStart(int start) {
            this.start = start;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SlopePointRange{");
            sb.append("start=").append(start);
            sb.append(", end=").append(end);
            sb.append('}');
            return sb.toString();
        }
    }
}
