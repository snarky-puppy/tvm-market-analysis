package com.tvm.crunch;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Created by horse on 24/07/2016.
 */
public class DataTest {
    @Test
    public void findDateIndex() throws Exception {

    }

    @Test
    public void findDateIndex1() throws Exception {

    }

    @Test
    public void findPCIncrease() throws Exception {

    }

    @Test
    public void findPCDecrease() throws Exception {

    }

    @Test
    public void findNMonthPoint() throws Exception {

    }

    @Test
    public void findNWeekPoint() throws Exception {

    }

    @Test
    public void findNDayPoint() throws Exception {

    }

    @Test
    public void findMinPriceLimitMonth() throws Exception {

    }

    @Test
    public void findMaxPriceLimitMonth() throws Exception {

    }

    @Test
    public void avgVolumePrev30Days() throws Exception {

    }

    @Test
    public void avgPricePrev30Days() throws Exception {

    }

    @Test
    public void avgVolumePost30Days() throws Exception {

    }

    @Test
    public void avgPricePost30Days() throws Exception {

    }

    @Test
    public void totalVolumePrev30Days() throws Exception {

    }

    @Test
    public void totalPricePrev30Days() throws Exception {

    }

    @Test
    public void totalVolumePost30Days() throws Exception {

    }

    @Test
    public void totalPricePost30Days() throws Exception {

    }

    @Test
    public void findEndOfYearPriceOfIndex() throws Exception {

    }

    @Test
    public void findEndOfYearPrice() throws Exception {

    }

    @Test
    public void ema() throws Exception {

    }

    @Test
    public void simpleMovingAverage() throws Exception {

    }

    @Test
    public void simpleMovingAverage1() throws Exception {

    }

    @Test
    public void slopeDaysPrev() throws Exception {
        ArrayList<Double> expectedResults = new ArrayList<>();

        Data data = loadSheet("Slope10Day", row -> {
            if(row.getCell(2) != null)
                expectedResults.add(row.getCell(2).getNumericCellValue());
            else
                expectedResults.add(null);
        });

        for(int idx = 0; idx < data.date.length; idx++) {
            Double rv = Data.slopeDaysPrev(idx, 10, data.date, data.close);
            if(rv == null)
                assertNull(expectedResults.get(idx));
            else
                assertEquals(rv, expectedResults.get(idx), 4);
        }
    }

    @Test
    public void zscore() throws Exception {

        ArrayList<Double> expectedResults = new ArrayList<>();

        Data data = loadSheet("ZScore10Day", row -> {
            if(row.getCell(2) != null)
                expectedResults.add(row.getCell(2).getNumericCellValue());
            else
                expectedResults.add(null);
        });

        for(int idx = 0; idx < data.date.length; idx++) {
            Double zscore = data.zscore(idx, 10);
            if(zscore == null)
                assertNull(expectedResults.get(idx));
            else
                assertEquals(zscore, expectedResults.get(idx), 4);
        }
    }

    private interface RowVisitor {
        void visit(Row row);
    }

    private Data loadSheet(String sheetName, RowVisitor visitor) {
        XSSFWorkbook workbook = getWorkbook();
        Sheet sheet = workbook.getSheet(sheetName);
        Data data = new Data(sheet.getLastRowNum()); // 0 based so no need to take header row into account

        int idx = 0;
        boolean first = true;
        for(Row row : sheet) {
            if(first) {
                first = false;
                continue;
            }

            if(row.getCell(0) == null) {
                continue;
            }

            int dt = (int)row.getCell(0).getNumericCellValue();
            double cl = row.getCell(1).getNumericCellValue();
            data.date[idx] = dt;
            data.close[idx] = cl;

            visitor.visit(row);

            idx++;
        }

        if(idx != data.date.length) {
            assertEquals("Rows read is not equal to rows expected", data.date.length, idx);
        }

        return data;
    }

    private XSSFWorkbook getWorkbook() {
        try {
            return new XSSFWorkbook(OPCPackage.open(getClass().getClassLoader().getResourceAsStream("DataTest.xlsx")));
        } catch (IOException|InvalidFormatException e) {
            e.printStackTrace();
            throw new RuntimeException("fubar");
        }
    }
}