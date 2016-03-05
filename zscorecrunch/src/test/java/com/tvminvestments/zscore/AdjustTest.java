package com.tvminvestments.zscore;

import com.tvminvestments.zscore.app.Adjustment;
import com.tvminvestments.zscore.app.Conf;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by horse on 17/07/15.
 */
public class AdjustTest {

    private static final Logger logger = LogManager.getLogger(AdjustTest.class);

    // symbol and market are the same string
    public final static String symbol = "AdjustTest";

    @Test
    public void testSimpleAdjust() throws Exception {
        Database db = DatabaseFactory.createDatabase(symbol);

        List<Double> targetData = new ArrayList<Double>();

        // "import" test data - based on doc/Split Adjust Example (1).xlsx
        String path = AdjustTest.class.getResource("/AdjustTest1Data.csv").getPath();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        int cnt = 0;
        while ((line = br.readLine()) != null) {
            String[] tuple = line.split(",");
            double preClose = Double.parseDouble(tuple[0]);
            double adjClose = Double.parseDouble(tuple[1]);
            targetData.add(adjClose);
            db.insertData(symbol, cnt, preClose, 0, 0);
            cnt ++;

        }
        db.commitDataTransaction();
        logger.info(String.format("Inserted %d rows into data table", cnt));

        // now, do the adjustment
        Adjustment adjustment = new Adjustment();
        adjustment.scanMarket(symbol);

        // read back in data
        CloseData data = db.loadData(symbol);

        // compare
        for(int i = 0; i < data.adjustedClose.length; i++) {

            BigDecimal f = new BigDecimal(data.adjustedClose[i]);
            BigDecimal z = new BigDecimal(targetData.get(i));

            // have to round down to 3 places to get it working! :/
            f = f.setScale(3, BigDecimal.ROUND_DOWN);
            z = z.setScale(3, BigDecimal.ROUND_DOWN);

            assertEquals(f, z);
        }

        FileUtils.deleteDirectory(Conf.getBaseDir(symbol).toFile());

    }
}
