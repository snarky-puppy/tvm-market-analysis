package com.tvminvestments.zscore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by horse on 15/11/14.
 */
public class ZScoreEntry {
    private static final Logger logger = LogManager.getLogger(ZScoreEntry.class);

    public int[] date;
    public double[] zscore;
    private int idx;
    private final int size;

    public ZScoreEntry(int size) {
        this.size = size;
        logger.debug("new ZScoreEntry["+size+"]");
        date = new int[size];
        zscore = new double[size];
        idx = 0;
    }

    public int getSize() {
        return idx;
    }

    public void sanity() {
        assert(date.length == zscore.length);
    }

    public int getLastDate() {
        if(date.length == 0)
            return -1;

        int lastDate = date[date.length - 1];

        return lastDate;
    }

    public void addZScore(int date, double zscore) {
        if(idx >= size)
            throw new IndexOutOfBoundsException(String.format("idx=%d, size=%d", idx, size));
        this.date[idx] = date;
        this.zscore[idx] = zscore;
        idx++;
    }


    /**
     * Returns non-negative index if zscore limit is found
     * Otherwise, returns negative position of last position, either trackingEnd or end of array
     *
     *
     * @param startIdx
     * @param limit
     * @param trackingEnd
     * @return
     */
    public int findIndexOfZScoreGTE(int startIdx, double limit, int trackingEnd) {
        if(startIdx >= zscore.length)
            throw new IndexOutOfBoundsException();

        while(startIdx < zscore.length) {
            if(zscore[startIdx] >= limit)
                return startIdx;
            if(date[startIdx] >= trackingEnd) {
                logger.debug("past tracking end");
                return -(startIdx);
            }
            startIdx ++;
        }
        logger.debug("past array end");
        return -(startIdx - 1);
    }

    public int findIndexOfZScoreLTE(int startIdx, double limit, int trackingEnd) {
        if(startIdx >= zscore.length)
            throw new IndexOutOfBoundsException();

        while(startIdx < zscore.length) {
            if(zscore[startIdx] <= limit)
                return startIdx;
            // XXX: this block should come before the above check, TODO: check with tim when we can fix this
            if(date[startIdx] > trackingEnd)
                return -1;
            startIdx ++;
        }
        return -1;
    }

    public int findClosestDateIndex(int target) {
        if(date.length == 0) {
            return -1;
        }
        int i = Arrays.binarySearch(date, target);
        if(i >= 0) {
            return i;
        } else {
            i = (-i) - 1;
            if (i >= date.length) {
                return  -1;
            } else {
                return i;
            }
        }
    }

    public double findZScoreAtDate(int targetDate) {
        int i = Arrays.binarySearch(date, targetDate);
        if(i < 0) {
            return -9999; // most likely the scenario ended, therefore zscore was not calculated
            //throw new ArrayIndexOutOfBoundsException("ZScore date not found: "+targetDate);
        }
        return zscore[i];
    }

    public double getLastZScore(int trackingEnd) {
        if (zscore.length == 0)
            return -1;
        return zscore[zscore.length - 1];
    }
}
