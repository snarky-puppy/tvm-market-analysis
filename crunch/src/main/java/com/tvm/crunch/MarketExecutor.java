package com.tvm.crunch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by horse on 21/07/2016.
 */
public abstract class MarketExecutor {

    private static final Logger logger = LogManager.getLogger(MarketExecutor.class);

    private final ResultWriter writer;
    private final ArrayBlockingQueue<Result> queue;
    protected final String market;

    abstract ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue);
    abstract void processSymbol(String symbol);
    abstract Database db();

    public MarketExecutor(String market) {
        queue = new ArrayBlockingQueue<Result>(1024);
        writer = createResultWriter(queue);
        this.market = market;
    }

    protected void execute() {

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Thread writerThread = new Thread(writer);

        try {
            writerThread.start();

            for (final String symbol : db().listSymbols(market)) {
                executorService.submit(new Runnable() {
                    public void run() {
                        processSymbol(symbol);
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                writer.setFinalised();
                writerThread.join();
                writer.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    protected void enqueueResult(Result result) {
        boolean accept = false;
        do {
            accept = queue.offer(result);
            try {
                if(!accept)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        } while (!accept);
    }

}
