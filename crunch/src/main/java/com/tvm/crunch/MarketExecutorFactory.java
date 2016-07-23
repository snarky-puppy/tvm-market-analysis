package com.tvm.crunch;

/**
 * Created by horse on 23/07/2016.
 */
public interface MarketExecutorFactory {
    MarketExecutor create(String market);
}
