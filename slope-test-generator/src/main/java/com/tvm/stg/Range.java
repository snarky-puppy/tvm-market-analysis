package com.tvm.stg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matt on 10/02/17.
 */
public abstract class Range<T extends Number & Comparable> {
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

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }

    public T getStep() {
        return step;
    }

    public boolean getIsRange() {
        return isRange;
    }


    public void setStart(T val) {
        start = val;
    }

    public void setEnd(T val) {
        end = val;
    }

    public void setStep(T val) {
        step = val;
    }

    void setIsRange(boolean b) {
        isRange = b;
    }

    protected abstract T fromString(String s);

    protected abstract void permute(List<T> list);

    void setStart(String s) {
        start = fromString(s);
    }

    void setEnd(String s) {
        end = fromString(s);
    }

    void setStep(String s) {
        step = fromString(s);
    }

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
