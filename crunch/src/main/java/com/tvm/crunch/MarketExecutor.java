package com.tvm.crunch;

import com.tvm.crunch.database.Database;
import com.tvm.crunch.database.DatabaseFactory;
import com.tvm.crunch.database.FileDatabaseFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manage market/symbol processing threads
 * Created by horse on 21/07/2016.
 */
public abstract class MarketExecutor {

    private static final Logger logger = LogManager.getLogger(MarketExecutor.class);



    //<editor-fold desc="Timings">
/*
m=1 s=2 -> 00:00:41.888
m=1 s=4 -> 00:00:21.890
m=1 s=6 -> 00:00:20.248
m=1 s=8 -> 00:00:22.735
m=1 s=10 -> 00:00:22.307
m=1 s=12 -> 00:00:26.058
m=1 s=14 -> 00:00:24.106
m=1 s=16 -> 00:00:24.950
m=2 s=2 -> 00:00:28.136
m=2 s=4 -> 00:00:18.405
m=2 s=6 -> 00:00:16.689
m=2 s=8 -> 00:00:16.226
m=2 s=10 -> 00:00:17.972
m=2 s=12 -> 00:00:16.757
m=2 s=14 -> 00:00:18.067
m=2 s=16 -> 00:00:16.868
m=3 s=2 -> 00:00:28.928
m=3 s=4 -> 00:00:18.660
m=3 s=6 -> 00:00:16.645
m=3 s=8 -> 00:00:15.187
m=3 s=10 -> 00:00:17.866
m=3 s=12 -> 00:00:15.663
m=3 s=14 -> 00:00:16.052
m=3 s=16 -> 00:00:16.003
m=4 s=2 -> 00:00:29.377
m=4 s=4 -> 00:00:19.411
m=4 s=6 -> 00:00:16.628
m=4 s=8 -> 00:00:15.660
m=4 s=10 -> 00:00:16.102
m=4 s=12 -> 00:00:15.749
m=4 s=14 -> 00:00:16.279
m=4 s=16 -> 00:00:16.735
m=5 s=2 -> 00:00:29.468
m=5 s=4 -> 00:00:19.490
m=5 s=6 -> 00:00:16.688
m=5 s=8 -> 00:00:15.980
m=5 s=10 -> 00:00:16.249
m=5 s=12 -> 00:00:17.333
m=5 s=14 -> 00:00:17.079
m=5 s=16 -> 00:00:16.175
m=6 s=2 -> 00:00:29.753
m=6 s=4 -> 00:00:20.358
m=6 s=6 -> 00:00:17.036
m=6 s=8 -> 00:00:15.992
m=6 s=10 -> 00:00:16.688
m=6 s=12 -> 00:00:17.817
m=6 s=14 -> 00:00:15.794
m=6 s=16 -> 00:00:16.590
m=7 s=2 -> 00:00:29.756
m=7 s=4 -> 00:00:19.409
m=7 s=6 -> 00:00:16.866
m=7 s=8 -> 00:00:15.455
m=7 s=10 -> 00:00:15.445
m=7 s=12 -> 00:00:16.102
m=7 s=14 -> 00:00:16.768
m=7 s=16 -> 00:00:16.498
m=8 s=2 -> 00:00:30.641
m=8 s=4 -> 00:00:19.621
m=8 s=6 -> 00:00:16.607
m=8 s=8 -> 00:00:15.483
m=8 s=10 -> 00:00:16.220
m=8 s=12 -> 00:00:15.707
m=8 s=14 -> 00:00:17.736
m=8 s=16 -> 00:00:16.926
*/
// </editor-fold>

    /**
     * Timed using TrendContBackTest and Design10
     * m=3 s=8 is technically the fastest (00:15.2)
     * but I'm choosing m=8 s=12 (00:15.7) for more threads
     */
    public static int MARKET_THREADS = 8;
    public static int SYMBOL_THREADS = 12;

    public static int QUEUE_SIZE = 4096;

    private final ResultWriter writer;
    private final ArrayBlockingQueue<Result> queue;
    protected final String market;
    private DatabaseFactory databaseFactory = new FileDatabaseFactory();

    protected abstract ResultWriter createResultWriter(ArrayBlockingQueue<Result> queue);
    protected abstract void processSymbol(String symbol);


    public MarketExecutor(String market) {
        this.market = market;
        queue = new ArrayBlockingQueue<Result>(4096);
        writer = createResultWriter(queue);
    }

    protected static void executeAllMarkets(DatabaseFactory databaseFactory, MarketExecutorFactory marketExecutorFactory) {

        ExecutorService executorService = Executors.newFixedThreadPool(MARKET_THREADS);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            for (final String market : databaseFactory.create().listMarkets()) {
                executorService.submit(new Runnable() {
                    public void run() {
                        try {
                            MarketExecutor executor = marketExecutorFactory.create(market);
                            executor.execute();
                        } catch(Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        stopWatch.stop();
        System.out.println(String.format("Finished in %s", stopWatch.toString()));
    }

    protected void execute() {
        ExecutorService executorService = Executors.newFixedThreadPool(SYMBOL_THREADS);
        Thread writerThread = new Thread(writer);

        try {
            writerThread.start();

            for (final String symbol : db().listSymbols(market)) {
                executorService.submit(new Runnable() {
                    public void run() {
                        try {
                            processSymbol(symbol);
                        } catch(Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
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
                    Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!accept);
    }

    protected void setDatabaseFactory(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    protected Database db() {
        return databaseFactory.create();
    }
}
