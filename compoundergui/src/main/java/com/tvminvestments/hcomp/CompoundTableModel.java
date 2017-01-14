package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by horse on 19/12/2015.
 */
public class CompoundTableModel extends AbstractTableModel {

    private static final Logger logger = LogManager.getLogger(CompoundTableModel.class);

    private Compounder compounder;

    final ColumnDef[] columnDefs = new ColumnDef[] {
            new ColumnDef("Date", Date.class),
            new ColumnDef("Symbol", String.class),
            new ColumnDef("Liquidity", Double.class),
            new ColumnDef("Transact", Double.class),
            new ColumnDef("Real Transact", Double.class),
            new ColumnDef("ROI%", Double.class),
            new ColumnDef("Compound Tally", Double.class),
            new ColumnDef("Bank Balance", Double.class),
            new ColumnDef("Total Assets", Double.class),
            new ColumnDef("Note", String.class)
    };


    CompoundTableModel() {
        compounder = new Compounder();
    }


    public void clearData() {
        compounder.clear();
        fireTableDataChanged();
    }

    public void calculate() {
        logger.info(String.format("Update triggered: spread=%d, percent=%.2f", compounder.spread, compounder.investPercent));
        compounder.calculate(0);
        fireTableDataChanged();
    }

    
    @Override
    public int getRowCount() {
        return compounder.size();
    }

    @Override
    public int getColumnCount() {
        return columnDefs.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnDefs[columnIndex].name;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnDefs[columnIndex].type;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return !(rowIndex == 0 || columnIndex > 2);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case 0:
                return compounder.get(rowIndex).date;
            case 1:
                return compounder.get(rowIndex).symbol;
            case 2:
                return compounder.get(rowIndex).liquidity;
            case 3:
                return compounder.get(rowIndex).transact;
            case 4:
                return compounder.get(rowIndex).compTransact;
            case 5:
                return compounder.get(rowIndex).roi;
            case 6:
                return compounder.get(rowIndex).compoundTally;
            case 7:
                return compounder.get(rowIndex).bankBalance;
            case 8:
                return compounder.get(rowIndex).totalAssets;
            case 9:
                return compounder.get(rowIndex).note;

        }
        return null;
    }


    public Compounder getCompounder() {
        return compounder;
    }

    public int findMatchingRow(int row) {
        String symbol = compounder.get(row).symbol;
        double trans = compounder.get(row).transact;
        return compounder.findMatch(symbol, row, trans > 0);
    }
}
