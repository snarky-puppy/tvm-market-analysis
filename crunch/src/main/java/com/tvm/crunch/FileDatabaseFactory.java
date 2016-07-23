package com.tvm.crunch;

/**
 * Created by horse on 23/07/2016.
 */
public class FileDatabaseFactory implements DatabaseFactory {
    @Override
    public Database create() {
        return new FileDatabase();
    }
}
