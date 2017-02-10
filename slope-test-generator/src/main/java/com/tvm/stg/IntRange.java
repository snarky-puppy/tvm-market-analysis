package com.tvm.stg;

import java.util.List;

/**
 * Created by matt on 10/02/17.
 */
public class IntRange extends Range<Integer> {
    public IntRange() {
        super();
    }

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
