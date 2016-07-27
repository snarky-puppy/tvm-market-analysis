package com.tvm.crunch;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.formula.functions.Intercept;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
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

        // k=index v=value
        Map<Integer, Double> expectedResults = new HashMap<>();

        Data data = loadSheet("MaxLimitMonth", false, new RowVisitor() {
            int idx = 0;
            @Override
            public void visit(Row row) {
                Cell cell = row.getCell(2);
                assertNotNull(cell);
                expectedResults.put(idx, cell.getNumericCellValue());
                idx++;
            }
        });

        // check test data
        assertNotEquals(0, expectedResults.size());

        // check bad inputs
        assertNull(data.findMaxPriceLimitMonth(-1, -1, data.close));
        assertNull(data.findMaxPriceLimitMonth(data.date.length, 12, data.close));

        assertEquals(data.date.length, data.close.length);

        for(int idx : expectedResults.keySet()) {
            System.out.println(String.format("idx=%d expecting=%f", idx, expectedResults.get(idx)));
            Point p = data.findMaxPriceLimitMonth(idx, 1, data.close);
            assertNotNull(p);
            assertEquals(expectedResults.get(idx), p.close);
        }
    }

    @Test
    public void avgVolumePrev30Days() throws Exception {
        thirtyDayTest(Data::avgVolumePrev30Days,
                new PreVisitor<Double>(11, new TDHelperDouble()));
    }

    @Test
    public void avgPricePrev30Days() throws Exception {
        thirtyDayTest(Data::avgPricePrev30Days,
                new PreVisitor<Double>(10, new TDHelperDouble()));
    }

    @Test
    public void avgVolumePost30Days() throws Exception {
        thirtyDayTest(Data::avgVolumePost30Days,
                new PostVisitor<Double>(7, new TDHelperDouble()));
    }

    @Test
    public void avgPricePost30Days() throws Exception {
        thirtyDayTest(Data::avgPricePost30Days,
            new PostVisitor<>(6, new TDHelperDouble()));
    }

    @Test
    public void totalVolumePrev30Days() throws Exception {
        thirtyDayTest(Data::totalVolumePrev30Days,
                new PreVisitor<Long>(9, new TDHelperLong()));
    }

    @Test
    public void totalPricePrev30Days() throws Exception {
        thirtyDayTest(Data::totalPricePrev30Days,
                new PreVisitor<Double>(8, new TDHelperDouble()));
    }

    @Test
    public void totalVolumePost30Days() throws Exception {
        thirtyDayTest(Data::totalVolumePost30Days,
                new PostVisitor<Long>(5, new TDHelperLong()));
    }

    @Test
    public void totalPricePost30Days() throws Exception {
        thirtyDayTest(Data::totalPricePost30Days,
                new PostVisitor<Double>(4, new TDHelperDouble()));
    }

    /**
     * This is horrendous
     *
     * @param <T>
     */
    private abstract class TDHelper<T> {
        public HashMap<Integer, T> results = new HashMap<>();
        abstract void put(Integer idx, Double value);
        abstract void assertEquals(int expected, T actual);
    }

    private class TDHelperLong extends TDHelper<Long> {
        @Override
        public void put(Integer idx, Double value) {
            results.put(idx, value.longValue());
        }
        @Override
        public void assertEquals(int expected, Long actual) {
            Assert.assertEquals(results.get(expected).longValue(), actual.longValue());
        }
    }

    private class TDHelperDouble extends TDHelper<Double> {
        @Override
        public void put(Integer idx, Double value) {
            results.put(idx, value);
        }
        @Override
        public void assertEquals(int expected, Double actual) {
            Assert.assertEquals(results.get(expected), actual, 4);
        }
    }

    private interface ThirtyDayFunc<T> {
        T func(Data data, int idx);
    }

    private <T> void thirtyDayTest(ThirtyDayFunc<T> func, ThirtyDayVisitor<T> visitor) {


        Data data = loadSheet("AA-", true, visitor);

        // check test data
        assertNotEquals(0, visitor.results().size());

        // check bad inputs
        assertNull(func.func(data, -1));
        assertNull(func.func(data, data.date.length));


        for(int idx : visitor.results().keySet()) {
            T rv = func.func(data, idx);
            assertNotNull(rv);
            visitor.assertEquals(idx, rv);
        }
    }

    private abstract class ThirtyDayVisitor<T> implements RowVisitor {
        int dataIdx;
        TDHelper<T> helper;

        ThirtyDayVisitor(int dataIdx, TDHelper<T> helper) {
            this.dataIdx = dataIdx;
            this.helper = helper;
        }

        Map<Integer, T> results() {
            return helper.results;
        }

        public abstract void visit(Row row);

        public void assertEquals(int idx, T rv) {
            helper.assertEquals(idx, rv);
        }
    }

    // Visitor for *Pre30Day tests
    private class PreVisitor<T> extends ThirtyDayVisitor<T> {
        int idx = 0;
        Double target = 0.0;
        boolean start = false;

        PreVisitor(int dataIdx, TDHelper<T> helper) {
            super(dataIdx, helper);
        }

        @Override
        public void visit(Row row) {
            Cell cell = row.getCell(dataIdx);
            if(cell == null) {
                idx++;
                return;
            }

            switch(cell.getCellType()) {
                case Cell.CELL_TYPE_FORMULA:
                case Cell.CELL_TYPE_NUMERIC:
                    assertFalse(start);
                    target = row.getCell(dataIdx).getNumericCellValue();
                    start = true;
                    break;

                case Cell.CELL_TYPE_STRING:
                    assertTrue(start);
                    Assert.assertEquals("start", cell.getStringCellValue());
                    start = false;
                    helper.put(idx, target);
                    break;
            }
            idx++;
        }
    }

    // Visitor for *Post30Day tests
    private class PostVisitor<T> extends ThirtyDayVisitor<T> {
        int idx = 0;
        int startIdx = 0;
        boolean start = false;

        PostVisitor(int dataIdx, TDHelper<T> helper) {
            super(dataIdx, helper);
        }

        @Override
        public void visit(Row row) {
            Cell cell = row.getCell(dataIdx);
            if(cell == null) {
                idx++;
                return;
            }

            switch(cell.getCellType()) {
                case Cell.CELL_TYPE_FORMULA:
                case Cell.CELL_TYPE_NUMERIC:
                    assertTrue(start);
                    Double d = row.getCell(dataIdx).getNumericCellValue();
                    helper.put(startIdx, d);
                    start = false;
                    break;

                case Cell.CELL_TYPE_STRING:
                    assertFalse(start);
                    Assert.assertEquals("start", cell.getStringCellValue());
                    startIdx = idx;
                    start = true;
                    break;
            }
            idx++;
        }
    }


    @Test
    public void findEndOfYearPrice() throws Exception {
        // k=year v=date
        Map<Integer, Integer> eoyResults = new HashMap<>();

        Data data = loadSheet("AA-", true, new RowVisitor() {
            @Override
            public void visit(Row row) {
                if(row.getCell(3) != null) {
                    int dt = (int)row.getCell(0).getNumericCellValue();
                    int y = DateUtil.getYear(dt);
                    eoyResults.put(y, dt);
                }
            }
        });

        assertNotEquals(0, eoyResults.size());

        OptionalInt optInt = eoyResults.keySet().stream().mapToInt(Integer::intValue).min();
        assertTrue(optInt.isPresent());
        int earliestYear =  optInt.getAsInt();

        earliestYear --;
        assertNull(data.findEndOfYearPrice(earliestYear));

        for(int y : eoyResults.keySet()) {
            Point p = data.findEndOfYearPrice(y);
            assertNotNull("findEndOfYearPrice returned null: "+y, p);
            assertEquals(eoyResults.get(y), p.date);
        }
    }

    @Test
    public void ema() throws Exception {
        checkValidatedRowTest("EMA10Day", (idx, data) -> Data.simpleMovingAverage(idx, 10, data.close));
    }

    @Test
    public void simpleMovingAverage1() throws Exception {
        checkValidatedRowTest("SMA10Day", (idx, data) -> Data.simpleMovingAverage(idx, 10, data.close));
    }

    @Test
    public void slopeDaysPrev() throws Exception {
        checkValidatedRowTest("Slope10Day", (idx, data) -> Data.slopeDaysPrev(idx, 10, data.date, data.close));
    }

    @Test
    public void zscore() throws Exception {
        checkValidatedRowTest("ZScore10Day", (idx, data) -> data.zscore(idx, 10));
    }

    private interface ValidateFunction {
        Double function(int idx, Data data);
    }

    private void checkValidatedRowTest(String sheetName, ValidateFunction func) {
        ArrayList<Double> expectedResults = new ArrayList<>();

        Data data = loadSheet(sheetName, row -> {
            if(row.getCell(2) != null)
                expectedResults.add(row.getCell(2).getNumericCellValue());
            else
                expectedResults.add(null);
        });

        for(int idx = 0; idx < data.date.length; idx++) {
            Double rv = func.function(idx, data);
            if(expectedResults.get(idx) == null)
                assertNull(rv);
            else {
                assertNotNull(String.format("Returned null, expected %f", expectedResults.get(idx)), rv);
                assertEquals(expectedResults.get(idx), rv, 4);
            }
        }
    }

    private interface RowVisitor {
        void visit(Row row);
    }

    private Data loadSheet(String sheetName, RowVisitor visitor) {
        return loadSheet(sheetName, false, visitor);
    }

    private Data loadSheet(String sheetName, boolean hasVolume, RowVisitor visitor) {
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

            if(hasVolume) {
                long volume = (long)row.getCell(2).getNumericCellValue();
                data.volume[idx] = volume;
            }

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