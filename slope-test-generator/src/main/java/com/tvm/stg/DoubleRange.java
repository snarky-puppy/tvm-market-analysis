package com.tvm.stg;

import java.util.List;

/**
 * Created by matt on 10/02/17.
 */
public class DoubleRange extends Range<Double> {
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
