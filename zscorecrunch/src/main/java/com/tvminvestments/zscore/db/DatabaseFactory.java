package com.tvminvestments.zscore.db;


/**
 * Created by horse on 29/01/15.
 */
public class DatabaseFactory {

    public static Database createDatabase(String market) throws Exception {
        Database d = new FileDB();
        d.init(market);
        return d;
    }
}
