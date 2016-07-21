package com.tvm.crunch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by horse on 21/07/2016.
 */
public abstract class ResultWriter implements Runnable {

    private static final Logger logger = LogManager.getLogger(ResultWriter.class);

    private final BlockingQueue<Result> blockingQueue;
    private boolean finalised = false;
    protected BufferedWriter writer;
    private boolean header = false;

    protected abstract String writeHeader();
    protected abstract String getFileName();

    protected ResultWriter(BlockingQueue<Result> blockingQueue) {
        try {
            this.blockingQueue = blockingQueue;
            writer = new BufferedWriter(new FileWriter(getFileName()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setFinalised() { finalised = true; }

    private void writeResults(Result r) throws IOException {
        if(!header) {
            writeHeader();
            header = true;
        }
        writer.write(r.toString());
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public void run() {
        try {
            Result r = null;
            do {
                r = blockingQueue.poll(1000, TimeUnit.MILLISECONDS);
                if (r != null) {
                    try {
                        writeResults(r);
                    } catch (IOException e) {
                        logger.error(e);
                        finalised = true;
                    }
                } else
                    logger.error("Nothing in queue");

            } while (r != null || finalised == false);

        } catch (InterruptedException e) {

        }
    }


}
