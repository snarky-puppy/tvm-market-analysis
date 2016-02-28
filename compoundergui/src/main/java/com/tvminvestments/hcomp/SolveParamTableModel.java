package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.table.AbstractTableModel;

/**
 * Created by horse on 17/01/2016.
 */
public class SolveParamTableModel extends AbstractTableModel {

    private static final Logger logger = LogManager.getLogger(SolveParamTableModel.class);

    int startBank = 200000;

    int minPercent = 0;
    int maxPercent = 50;
    int stepPercent = 10;

    int minSpread = 0;
    int maxSpread = 25;
    int stepSpread = 5;

    int iterations = 10;

    @Override
    public int getRowCount() {
        return 8;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int ci) {

        switch(rowIndex) {
            case 0: return ci == 0 ? "Start Bank" : Integer.toString(startBank);

            case 1: return ci == 0 ? "Min %" : Integer.toString(minPercent);
            case 2: return ci == 0 ? "Max %" : Integer.toString(maxPercent);
            case 3: return ci == 0 ? "Step %" : Integer.toString(stepPercent);

            case 4: return ci == 0 ? "Min Spread" : Integer.toString(minSpread);
            case 5: return ci == 0 ? "Max Spread" : Integer.toString(maxSpread);
            case 6: return ci == 0 ? "Step Spread" : Integer.toString(stepSpread);

            case 7: return ci == 0 ? "Iterations" : Integer.toString(iterations);

            default: return "n/a";
        }
    }

    @Override
    public String getColumnName(int column) {
        if(column == 0)
            return "Parameter";
        return "Value";

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if(columnIndex == 0)
            return false;
        return true;
        //return super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        logger.info(String.format("param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
        try {
            switch (rowIndex) {
                case 0: startBank = Integer.parseInt((String) aValue); break;

                case 1: minPercent = Integer.parseInt((String) aValue); break;
                case 2: maxPercent = Integer.parseInt((String) aValue); break;
                case 3: stepPercent = Integer.parseInt((String) aValue); break;

                case 4: minSpread = Integer.parseInt((String) aValue); break;
                case 5: maxSpread = Integer.parseInt((String) aValue); break;
                case 6: stepSpread = Integer.parseInt((String) aValue); break;
                case 7: iterations = Integer.parseInt((String) aValue); break;

            }
        } catch(NumberFormatException ex) {
            logger.error("NumberFormatException: "+(String)aValue +" just isn't cool");
        }
    }
}
