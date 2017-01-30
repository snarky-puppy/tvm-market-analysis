package com.tvm.stg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 21/1/17.
 */
public class ConfigBean {
    public String configName;
    public List<SlopePointRange> pointDistances = new ArrayList<>();

    static class SlopePointRange {
        public int start;
        public int end;

        public SlopePointRange() {
        }

        public SlopePointRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() { return start; }
        public int getEnd() { return end; }

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
