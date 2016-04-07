package com.tvmresearch.lotus;

import java.util.ArrayList;

/**
 * Created by horse on 23/03/2016.
 */
public class Test {

    private static void test(int i) {
        System.out.println(i);
    }

    static class T {
        public int i;
        public T(int i) { this.i = i; }
    }

    public static void main(String[] args) {
        ArrayList<T> arr = new ArrayList<>();
        arr.add(new T(1));
        arr.add(new T(2));
        arr.add(new T(3));



        int sum = arr.stream().mapToInt(t -> t.i).sum();
        System.out.println(sum);

    }
}
