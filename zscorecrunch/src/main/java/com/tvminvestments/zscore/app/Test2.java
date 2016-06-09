package com.tvminvestments.zscore.app;

/**
 * Created by horse on 9/06/2016.
 */
public class Test2 {
    public static void main(String[] args) {
        int [] data = new int[] { 1, 2, 3, 4, 5};
        int idx = 3;

        for(int r = idx; r % data.length != idx; r++) {
            System.out.println(data[r % data.length]);
        }
    }
}
