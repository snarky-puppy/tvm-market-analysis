package com.tvm.crunch;

import java.util.List;
import java.util.Set;

/**
 * Created by horse on 21/07/2016.
 */
public interface Database {
    Set<String> listSymbols(String market);
    Data loadData(String market, String symbol);

    List<String> listMarkets();
}
