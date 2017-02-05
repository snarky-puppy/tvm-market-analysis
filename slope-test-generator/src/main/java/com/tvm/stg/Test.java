package com.tvm.stg;

import com.tvm.stg.ConfigBean.IntRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by horse on 3/2/17.
 */
public class Test {

    public static List<List<Integer>> merge(List<IntRange> pointDistances) {
        // copy array so original is unmolested
        return mergePrivate(new ArrayList<>(pointDistances));
    }


    public static List<List<Integer>> mergePrivate(List<IntRange> pointDistances) {

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
        List<List<Integer>> permutations = merge(pointDistances);
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




    public static void main(String[] args) {
        List<List<Integer>> points = new ArrayList<>();

        List<IntRange> pointDistances = new ArrayList<>();

        pointDistances.add(new IntRange(1, 2, 1, true));
        pointDistances.add(new IntRange(1, 3, 1, true));
        pointDistances.add(new IntRange(1, 2, 1, true));

        points = merge(pointDistances);

        for(List<Integer> l : points) {
            for(int i = 0; i < l.size(); i++) {
                System.out.print(Integer.toString(l.get(i))+",");
            }
            System.out.print("\n");
        }
    }
}
