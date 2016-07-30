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
 * Threaded Read Results from a queue and write to a zip file.
 *
 * Splits up zip file entries to be kinder to Tim and Vince's PCs
 *
 * Created by horse on 21/07/2016.
 */
public abstract class ResultWriter implements Runnable {

    // 50MB
    private final static int MAX_FILE_SIZE = 1024*1024*50;

    private static final Logger logger = LogManager.getLogger(ResultWriter.class);

    private final BlockingQueue<Result> blockingQueue;
    private boolean finalised = false;
    private boolean header = false;

    private int fileNumber = 1;
    private int bytesWritten = 0;

    private ZipOutputStream zipOutputStream;

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
            write(r.getHeader());
            header = true;
        }
        write(r.toString());
    }

    public void close() {
        try {
            if(zipOutputStream != null) {
                zipOutputStream.closeEntry();
                zipOutputStream.close();
            }
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
            Result r;
            do {
                r = blockingQueue.poll(1000, TimeUnit.MILLISECONDS);
                if (r != null) {
                    try {
                        writeResults(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e);
                        System.exit(1);
                    }
                }
            } while (r != null || !finalised);

        } catch (InterruptedException e) {

        }
    }
}