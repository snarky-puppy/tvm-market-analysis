package com.tvminvestments.hcomp;

/**
 * Created by horse on 19/12/2015.
 */
class ColumnDef {
    public String name;
    public Class<?> type;

    public ColumnDef(String symbol, Class<?> klass) {
        name = symbol;
        type = klass;
    }
}
