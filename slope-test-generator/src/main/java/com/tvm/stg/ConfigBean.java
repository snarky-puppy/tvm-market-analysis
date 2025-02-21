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
    public int iterations = 10;
    public double maxDolVol = 1;


    public DoubleRange investPercent = new DoubleRange(10.0, 50.0, 10.0, false);
    public IntRange investSpread = new IntRange(5, 25, 5, false);

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
