package com.tvminvestments.zscore.db.file;

import com.tvminvestments.zscore.CloseData;
import com.tvminvestments.zscore.DateUtil;
import com.tvminvestments.zscore.ZScoreEntry;
import com.tvminvestments.zscore.app.Adjustment;
import com.tvminvestments.zscore.app.Conf;
import com.tvminvestments.zscore.importer.CSVFileImporter;
import com.tvminvestments.zscore.RangeBounds;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by horse on 6/02/15.
 */
public class FileDBImpl {

    private static final Logger logger = LogManager.getLogger(FileDBImpl.class);

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors() * 2;



    public final String market;
    FileChannel indexFile;

    // symbol:fileptr
    HashMap<String, BufferedInputStream> dataReads;

    // big write buffer
    HashMap<String, HashMap<Integer, String>> writeData;

    public FileDBImpl(String market) {
        this.market = market;

        dataReads = new HashMap<String, BufferedInputStream>();
        writeData = new HashMap<String, HashMap<Integer, String>>();

        openDirStructure();
    }

    public Path dataFile(String symbol) {
        return Conf.getDataDir(market, symbol).resolve(symbol + ".txt");
    }

    public Path zscoreFile(String symbol, int startDate) {
        return Conf.getZScoreDir(market, symbol).resolve(symbol + "-"+startDate+".txt");
    }

    private synchronized void ensureWriter(String symbol) {
        if(!writeData.containsKey(symbol)) {
            writeData.put(symbol, new HashMap<Integer, String>());
        }

    }

    /**
     * Fresh inserted data
     *  @param symbol
     * @param date
     * @param close
     * @param volume
     * @param open
     */
    public void insertData(String symbol, int date, double close, double volume, double open) {
        ensureWriter(symbol);
        HashMap<Integer, String> map = writeData.get(symbol);
        String str = String.format("%d,%f,%f,%f,%f\n", date, close, volume, close, open);
        synchronized (map) {
            map.put(date, str);
        }
    }

    private FileChannel openZScore(int date) {
        return null;
    }

    public void freshImport() {
        try {
            FileUtils.deleteDirectory(Conf.getBaseDir(market).toFile());
            openDirStructure();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void ensurePathExists(Path p) throws IOException {
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }
    }

    private void openDirStructure() {
        try {
            ensurePathExists(Conf.getBaseDir(market));
            ensurePathExists(Conf.getDataDir(market, null));
            ensurePathExists(Conf.getZScoreDir(market, null));

            //indexFile = FileChannel.open(basePath.resolve(Conf.indexFile));

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void closeAllReads() {
        for(BufferedInputStream b : dataReads.values()) {
            try {
                b.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        dataReads.clear();
    }

    public void closeAllWrites() {
        ExecutorService executorService = Executors.newFixedThreadPool(CSVFileImporter.N_THREADS);

        for(final String symbol : writeData.keySet()) {
            final HashMap<Integer, String> map = writeData.get(symbol);

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        doWrite(symbol, map);
                    } catch (RuntimeException e) {
                        logger.error("closeAllWrites failed: ", e);
                        System.exit(1);
                    } catch (Exception e) {
                        logger.error("closeAllWrites failed: ", e);
                        System.exit(1);
                    }
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        writeData.clear();
    }

    private void doWrite(String symbol, HashMap<Integer, String> map) throws IOException {
        final SortedSet<Integer> keys = new TreeSet<Integer>(map.keySet());
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(String.valueOf(dataFile(symbol)), true));
        for(Integer i : keys) {
            outputStream.write(map.get(i).getBytes());
        }
        outputStream.flush();
        outputStream.close();
    }

    public Set<String> listSymbols() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Set<String> rv = new HashSet<>();
        try {
            Files.walk(Conf.getDataDir(market, null))
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(p -> rv.add(p.getFileName().toString().replace(".txt", "")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        stopWatch.stop();
        logger.info("Collected symbols in " + stopWatch.toString());
        return rv;
    }

    private void parseDataLine(String symbol, CloseData d, int idx, String line) {
        String[] fields = line.split(",");
        if(fields.length < 4)
            logger.error("parse data error: "+market+"/"+symbol+": not enough fields: "+fields.length);
        d.date[idx] = Integer.parseInt(fields[0]);
        d.close[idx] = Double.parseDouble(fields[1]);
        d.volume[idx] = Double.parseDouble(fields[2]);
        d.adjustedClose[idx] = Double.parseDouble(fields[3]);
        d.open[idx] = Double.parseDouble(fields[4]);
    }

    public CloseData loadData(String symbol) {
        logger.info("Loading data "+market+"/"+symbol);
        try {
            List<String> lines = Files.readAllLines(dataFile(symbol));
            CloseData rv = new CloseData(symbol, lines.size());
            int i = 0;
            for(String line : lines) {
                parseDataLine(symbol, rv, i, line);
                i++;
            }
            rv.sanity();
            return rv;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void rewrite(CloseData data) {
        try {
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(String.valueOf(dataFile(data.symbol)), false));
            for(int i = 0; i < data.close.length; i++) {
                StringBuilder builder = new StringBuilder(256);
                builder.append(String.format("%d,%f,%f", data.date[i], data.close[i], data.volume[i]));
                builder.append(String.format(",%f", data.adjustedClose[i]));

                //builder.append(String.format(",%f", data.ratio[i]));
                builder.append(String.format(",%f", data.open[i]));

                builder.append("\n");
                outputStream.write(builder.toString().getBytes());

            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public RangeBounds findDataBounds(String symbol) {

        try {

            int first, last, count = 0;

            RandomAccessFile file = new RandomAccessFile(dataFile(symbol).toString(), "r");
            FileChannel channel = file.getChannel();

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());

            StringBuilder builder = new StringBuilder();

            buffer.rewind();

            byte b;

            // first field in the file is the start date
            while ((b = buffer.get()) != ',')
                builder.append((char) b);

            first = Integer.parseInt(builder.toString());

            while (buffer.hasRemaining())
                if (buffer.get() == '\n')
                    count++;

            // XXX won't work if we ever have files over 2Gb
            int j = (int) file.length() - 2;

            while (j >= 0 && buffer.get(j) != '\n')
                j--;

            builder = new StringBuilder();
            while ((b = buffer.get(++j)) != ',')
                builder.append((char) b);

            channel.close();
            file.close();

            last = Integer.parseInt(builder.toString());

            RangeBounds bounds = new RangeBounds(first, last, count);

            return bounds;

        } catch (Exception e) {
            logger.error("Processing file "+symbol, e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void dropDataFile(String symbol) {
        try {
            FileUtils.forceDelete(dataFile(symbol).toFile());
        } catch(FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private File[] listZScoreFiles(String symbol) {
        final File zScoreDir = Conf.getZScoreDir(market, symbol).toFile();

        final File[] files = zScoreDir.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches( symbol+"-[0-9]*\\.txt" );
            }
        } );

        return files;
    }

    public void dropZScoreFile(String symbol) {
        try {
            final File[] files = listZScoreFiles(symbol);

            for ( final File file : files ) {
                logger.info("Delete: " + file.getName());
                if ( !file.delete() ) {
                    System.err.println( "Can't remove " + file.getAbsolutePath() );
                }
            }

        } finally {

        }

    }

    public int findMaxZScoreDate(String symbol, int startDate) {
        int rv = -1;
        try {

            RandomAccessFile file = new RandomAccessFile(zscoreFile(symbol, startDate).toString(), "r");

            if(file.length() == 0)
                return startDate;

            FileChannel channel = file.getChannel();

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());

            StringBuilder builder = new StringBuilder();

            byte b;

            // XXX won't work if we ever have files over 2Gb
            int j = (int) file.length() - 2;

            while (j>0 && buffer.get(j) != '\n')
                j--;

            if(j == 0) {
                // special case: only 1 line in the file
                j = -1;
            }

            while ((b = buffer.get(++j)) != ',')
                builder.append((char) b);

            rv = Integer.parseInt(builder.toString());

            channel.close();
            file.close();


            return rv;
        } catch(FileNotFoundException e) {
            return startDate;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ZScoreEntry loadZScores(String symbol, int sampleStart) {
        try {
            List<String> lines = Files.readAllLines(zscoreFile(symbol, sampleStart));
            ZScoreEntry rv = new ZScoreEntry(lines.size());

            for (String line : lines) {
                String[] fields = line.split(",");

                rv.addZScore(Integer.parseInt(fields[0]), Double.parseDouble(fields[1]));
            }

            return rv;

        } catch(FileNotFoundException | NoSuchFileException e) {
            return null;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, ZScoreEntry> loadZScores(String symbol) {
        Map<Integer, ZScoreEntry> rv = new HashMap<>();

        final File[] files = listZScoreFiles(symbol);

        for(File file : files) {
            String[] fname = file.getName().replaceAll(".txt", "").split("-");

            int startDate = Integer.parseInt(fname[1]);

            rv.put(startDate, loadZScores(symbol, startDate));
        }

        return rv;
    }

    public void insertZScores(String symbol, Map<Integer, ZScoreEntry> zscores) {
        for (Integer startDate : zscores.keySet()) {
            Path p = zscoreFile(symbol, startDate);
            ZScoreEntry entry = zscores.get(startDate);

            logger.info(String.format("[%s] Inserting %d zscores, start=%d", symbol, (entry == null ? 0 : entry.date.length), startDate));

            if(entry == null || entry.date.length == 0)
                continue;

            entry.sanity();

            try {

                // preallocate file
                final int maxLineLength = 128;
                final int fileSize = maxLineLength * entry.date.length;


                FileChannel channel = new RandomAccessFile(p.toString(), "rw").getChannel();
                channel.position(channel.size());
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                for(int i = 0; i < entry.date.length; i++) {
                    if(entry.date[i] != -1 && entry.date[i] != 0) { // denotes n/a zscore, such as when stdev() returns 0
                        buffer.put(String.format("%d,%f\n", entry.date[i], entry.zscore[i]).getBytes());

                        if(buffer.remaining() <= maxLineLength * 5) {
                            buffer.flip();
                            channel.write(buffer);
                            buffer.clear();
                        }
                    } else
                        logger.debug("empty zscore at "+i);
                }
                if(buffer.hasRemaining()) {
                    buffer.flip();
                    channel.write(buffer);
                    buffer.clear();
                }
                channel.close();

                /*
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(p.toString(), true));
                for(int i = 0; i < entry.date.length; i++) {
                    if(entry.date[i] != -1 && entry.date[i] != 0) { // denotes n/a zscore, such as when stdev() returns 0
                        outputStream.write(String.format("%d,%f\n", entry.date[i], entry.zscore[i]).getBytes());
                    } else
                        logger.debug("empty zscore at "+i);
                }
                outputStream.flush();
                outputStream.close();
                */
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private void doDelete(Path p) {
        try {
            Files.delete(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dropAllZScore() {
        logger.info("Drop all Zscores");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            FileUtils.deleteDirectory(Conf.getZScoreDir(market, null).toFile());
            ensurePathExists(Conf.getZScoreDir(market, null));

            /*
            Files.list(Conf.getZScoreDir(market))
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(p -> doDelete(p));
                    */
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopWatch.stop();
        logger.info("Dropped all ZScore in "+stopWatch.toString());
    }


}
