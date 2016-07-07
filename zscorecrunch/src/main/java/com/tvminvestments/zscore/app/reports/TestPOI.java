package com.tvminvestments.zscore.app.reports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by horse on 2/07/2016.
 */
public class TestPOI {

    public static void main(String[] args) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(new File("doc/SampP 500 Historical Components amp Change History SiblisResearch.xlsx"));
        XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> iter = sheet.iterator();
        while(iter.hasNext()) {
            Row row = iter.next();
            Cell cell = row.getCell(0);
            System.out.println(cell.getStringCellValue());
        }
    }
}
