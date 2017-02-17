package com.tvm.stg;

import com.tvm.stg.DataCruncher.SimulationBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.tvm.stg.DataCruncher.SlopeResult;

/**
 * Created by matt on 13/02/17.
 */
public class ResultWriter implements Runnable {
    private static final Logger logger = LogManager.getLogger(ResultWriter.class);

    private ArrayBlockingQueue<Object> queue;
    private boolean finalised = false;

    private static final String[] simTitles = {
            "id",
            "minDolVol",
            "daysDolVol",
            "slopeCutoff",
            "maxHoldDays",
            "daysLiqVol",
            "stopPc",
            "targetPc",
            "tradeStartDays",
            "investPc",
            "investSpread",
            "pointDistances" };

    private static final String[] slopeTitles = {
            "simId",
            "slopeId",
            "symbol",
            "entryDate",
            "entryOpen",
            "exitDate",
            "exitOpen",
            "exitReason",
            "slope",
            "dollarVolume",
            "target",
            "stop",
            "liquidity" };

    private static final String[] compTitles = {
            "simId",
            "slopeId",
            "iteration",
            "Date",
            "Symbol",
            "Liquidity",
            "Transact",
            "Real Transact",
            "ROI%",
            "Compound Tally",
            "Bank Balance",
            "Total Assets",
            "Note"
    };

    public ResultWriter(ArrayBlockingQueue<Object> queue) {
        this.queue = queue;
    }

    public void enqueue(Object obj) {
        boolean accept = false;
        do {
            accept = queue.offer(obj);
            try {
                if(!accept)
                    Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                accept = true;
            }
        } while (!accept);
    }

    private void writeHeaders(Sheet sheet, String[] titles, Map<String, CellStyle> styles) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(styles.get("header"));
        }
        sheet.createFreezePane(0, 1);
    }

    @Override
    public void run() {
        Workbook wb = new XSSFWorkbook();
        Sheet simSheet = wb.createSheet("Simulation");
        Sheet slopeSheet = wb.createSheet("Slope");
        Sheet compSheet = wb.createSheet("Compound");

        int simRow = 1, slopeRow = 1, compRow = 1;

        Map<String, CellStyle> styles = createStyles(wb);
        writeHeaders(simSheet, simTitles, styles);
        writeHeaders(slopeSheet, slopeTitles, styles);
        writeHeaders(compSheet, compTitles, styles);

        try {
            Object obj;
            do {
                obj = queue.poll(1000, TimeUnit.MILLISECONDS);

                if (obj != null) {
                    if(obj instanceof SimulationBean) {
                        SimulationBean bean = (SimulationBean) obj;
                        int c = 0;
                        Row row = simSheet.createRow(simRow++);
                        row.createCell(c++).setCellValue(bean.id);
                        row.createCell(c++).setCellValue(bean.minDolVol);
                        row.createCell(c++).setCellValue(bean.daysDolVol);
                        row.createCell(c++).setCellValue(bean.slopeCutoff);
                        row.createCell(c++).setCellValue(bean.maxHoldDays);
                        row.createCell(c++).setCellValue(bean.daysLiqVol);
                        row.createCell(c++).setCellValue(bean.stopPc);
                        row.createCell(c++).setCellValue(bean.targetPc);
                        row.createCell(c++).setCellValue(bean.tradeStartDays);
                        row.createCell(c++).setCellValue(bean.investPc);
                        row.createCell(c++).setCellValue(bean.investSpread);
                        String points = bean.pointDistances.stream().map(Object::toString)
                                .collect(Collectors.joining(","));
                        row.createCell(c++).setCellValue(points);

                    } else if(obj instanceof SlopeResult) {
                        SlopeResult result = (SlopeResult) obj;
                        int c = 0;
                        Row row = slopeSheet.createRow(slopeRow++);
                        row.createCell(c++).setCellValue(result.simId);
                        row.createCell(c++).setCellValue(result.slopeId);
                        row.createCell(c++).setCellValue(result.symbol);
                        row.createCell(c++).setCellValue(result.entryDate);
                        row.createCell(c++).setCellValue(result.entryOpen);
                        row.createCell(c++).setCellValue(result.exitDate);
                        row.createCell(c++).setCellValue(result.exitOpen);
                        row.createCell(c++).setCellValue(result.exitReason.toString());
                        row.createCell(c++).setCellValue(result.slope);
                        row.createCell(c++).setCellValue(result.dollarVolume);
                        row.createCell(c++).setCellValue(result.target);
                        row.createCell(c++).setCellValue(result.stop);
                        row.createCell(c++).setCellValue(result.liquidity);
                    } else if(obj instanceof Compounder.Row) {
                        Compounder.Row r = (Compounder.Row)obj;
                        int c = 0;
                        Row row = compSheet.createRow(compRow++);
                        row.createCell(c++).setCellValue(r.simId);
                        row.createCell(c++).setCellValue(r.slopeId);
                        row.createCell(c++).setCellValue(r.iteration);
                        row.createCell(c++).setCellValue(DateUtil.toInteger(r.date));
                        row.createCell(c++).setCellValue(r.symbol);
                        if(r.transact > 0)
                            row.createCell(c++).setCellValue(r.liquidity);
                        else
                            c++;
                        row.createCell(c++).setCellValue(r.transact);
                        if(r.compTransact != null)
                            row.createCell(c++).setCellValue(r.compTransact);
                        else
                            c++;
                        if(r.roi != null)
                            row.createCell(c++).setCellValue(r.roi);
                        else
                            c++;
                        if(r.compoundTally != null)
                            row.createCell(c++).setCellValue(r.compoundTally);
                        else
                            c++;
                        row.createCell(c++).setCellValue(r.bankBalance);
                        row.createCell(c++).setCellValue(r.totalAssets);
                        row.createCell(c++).setCellValue(r.note);
                    }
                }

            } while (obj != null || !finalised);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        resetColumnSize(simSheet);
        resetColumnSize(slopeSheet);
        resetColumnSize(compSheet);

        try {
            File file = File.createTempFile("STG", ".xlsx");
            FileOutputStream out = null;
            out = new FileOutputStream(file);
            wb.write(out);
            out.close();
            wb.close();
            file.deleteOnExit();
            logger.info("Opening "+file.toString());
            Desktop.getDesktop().open(file);

        } catch (IOException e) {
            logger.error("Error writing output file", e);
            e.printStackTrace();
        }
    }

    private void resetColumnSize(Sheet sheet) {
        for(int i = 0; i < 50; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * create a library of cell styles
     */
    private static Map<String, CellStyle> createStyles(Workbook wb){
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        DataFormat df = wb.createDataFormat();

        CellStyle style;
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(headerFont);
        styles.put("header", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(headerFont);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("header_date", style);

        Font font1 = wb.createFont();
        font1.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font1);
        styles.put("cell_b", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFont(font1);
        styles.put("cell_b_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_b_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_g", style);

        Font font2 = wb.createFont();
        font2.setColor(IndexedColors.BLUE.getIndex());
        font2.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font2);
        styles.put("cell_bb", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_bg", style);

        Font font3 = wb.createFont();
        font3.setFontHeightInPoints((short)14);
        font3.setColor(IndexedColors.DARK_BLUE.getIndex());
        font3.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font3);
        style.setWrapText(true);
        styles.put("cell_h", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setWrapText(true);
        styles.put("cell_normal", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        styles.put("cell_normal_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setWrapText(true);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_normal_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setIndention((short)1);
        style.setWrapText(true);
        styles.put("cell_indented", style);

        style = createBorderedStyle(wb);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("cell_blue", style);

        return styles;
    }

    private static CellStyle createBorderedStyle(Workbook wb){
        BorderStyle thin = BorderStyle.THIN;
        short black = IndexedColors.BLACK.getIndex();

        CellStyle style = wb.createCellStyle();
        style.setBorderRight(thin);
        style.setRightBorderColor(black);
        style.setBorderBottom(thin);
        style.setBottomBorderColor(black);
        style.setBorderLeft(thin);
        style.setLeftBorderColor(black);
        style.setBorderTop(thin);
        style.setTopBorderColor(black);
        return style;
    }

    public void setFinalised(boolean finalised) {
        this.finalised = finalised;
    }
}
