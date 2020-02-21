package com.tvm.crunch.database;

import java.nio.file.Path;

/**
 * Created by horse on 23/07/2016.
 */
public class FileDatabaseFactory implements DatabaseFactory {

    private Path dbPath;

    public FileDatabaseFactory(Path dbPath) {

        this.dbPath = dbPath;
    }

    @Override
    public Database create() {
        return new FileDatabase(dbPath);
    }
}
