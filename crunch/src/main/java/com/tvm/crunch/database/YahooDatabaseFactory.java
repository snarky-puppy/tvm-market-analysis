package com.tvm.crunch.database;

import java.io.IOException;

/**
 * Created by horse on 1/11/16.
 */
public class YahooDatabaseFactory implements DatabaseFactory {

    @Override
    public Database create() {
        return new YahooDatabase();
    }

    public void updateFromWeb() throws IOException {
        new YahooDatabase().update();
    }
}
