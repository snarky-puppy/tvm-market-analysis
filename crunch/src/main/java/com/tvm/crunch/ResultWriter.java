package com.tvm.crunch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SyslogAppender;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by horse on 21/07/2016.
 */
public abstract class ResultWriter implements Runnable {

    private static final Logger logger = LogManager.getLogger(ResultWriter.class);

    private final BlockingQueue<Result> blockingQueue;
    private boolean finalised = false;
    private boolean header = false;

    private int fileNumber = 1;
    private int bytesWritten = 0;

    // 25MB
    private final static int MAX_FILE_SIZE = 1024*1024*25;
    private ZipOutputStream zipOutputStream;

    protected abstract String getHeader();
    protected abstract String getProjectName();
    protected abstract String getMarket();

    private String getActualFileName() {
        return String.format("%s-%s-pt%d.csv", getProjectName(), getMarket(), fileNumber);
    }

    private String getZipFileName() {
        return String.format("%s-%s-%d.zip", getProjectName(), getMarket(), DateUtil.timestamp());
    }

    protected ResultWriter(BlockingQueue<Result> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    public void setFinalised() { finalised = true; }

    private void writeResults(Result r) throws IOException {
        if(!header) {
            write(getHeader());
            header = true;
        }
        write(r.toString());
    }

    public void close() {
        try {
            zipOutputStream.closeEntry();
            zipOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void rotate() {
        try {
            header = false;
            bytesWritten = 0;
            fileNumber ++;
            zipOutputStream.closeEntry();
            ZipEntry zipEntry = new ZipEntry(getActualFileName());
            zipOutputStream.putNextEntry(zipEntry);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private ZipOutputStream getWriter() {
        if(zipOutputStream == null) {
            try {
                zipOutputStream = new ZipOutputStream(new FileOutputStream(getZipFileName()));
                zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
                ZipEntry zipEntry = new ZipEntry(getActualFileName());
                zipOutputStream.putNextEntry(zipEntry);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return zipOutputStream;
    }

    private void write(String s) {
        ZipOutputStream out = getWriter();
        try {
            out.write(s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        bytesWritten += s.length();
        if(bytesWritten > MAX_FILE_SIZE)
            rotate();
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