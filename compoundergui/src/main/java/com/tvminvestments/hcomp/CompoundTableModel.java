package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by horse on 19/12/2015.
 */
public class CompoundTableModel extends AbstractTableModel {

    private static final Logger logger = LogManager.getLogger(CompoundTableModel.class);
    
    private Compounder compounder;

    final ColumnDef[] columnDefs = new ColumnDef[] {
            new ColumnDef("Symbol", String.class),
            new ColumnDef("Transact", Double.class),
            new ColumnDef("Date", Date.class),
            new ColumnDef("Real Transact", Double.class),
            new ColumnDef("Bank Balance", Double.class),
            new ColumnDef("ROI%", Double.class),
            new ColumnDef("Compound Tally", Double.class)
    };


    CompoundTableModel() {
        compounder = new Compounder();
    }


    public void clearData() {
        compounder.clear();
        fireTableDataChanged();
    }

    public void calculate() {
        logger.info(String.format("Update triggered: spread=%d, percent=%d", compounder.spread, compounder.investPercent));
        compounder.calculate();
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
                return compounder.get(rowIndex).symbol;
            case 1:
                return compounder.get(rowIndex).transact;
            case 2:
                return compounder.get(rowIndex).date;
            case 3:
                return compounder.get(rowIndex).realTransact;
            case 4:
                return compounder.get(rowIndex).bankBalance;
            case 5:
                return compounder.get(rowIndex).roi;
            case 6:
                return compounder.get(rowIndex).compoundTally;
        }
        return null;
    }


    public Compounder getCompounder() {
        return compounder;
    }
}
