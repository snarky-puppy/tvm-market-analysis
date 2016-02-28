package com.tvminvestments.hcomp;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;


/**
 * Created by horse on 17/01/2016.
 */
public class SolverResultTableModel extends AbstractTableModel {

    final ColumnDef[] columnDefs = new ColumnDef[] {
            new ColumnDef("File", File.class),
            
            new ColumnDef("Total Average", Double.class),
            new ColumnDef("Total High", Double.class),
            new ColumnDef("Total Low", Double.class),
            new ColumnDef("Total StDev", Double.class),

            new ColumnDef("Cash Average", Double.class),
            new ColumnDef("Cash High", Double.class),
            new ColumnDef("Cash Low", Double.class),
            new ColumnDef("Cash StDev", Double.class),
            
            new ColumnDef("Percent", Integer.class),
            new ColumnDef("Spread", Integer.class)
    };

    private ArrayList<SolverResult> data = new ArrayList<>();

    public void clearResults() {
        data.clear();
    }

    @Override
    public int getRowCount() {
        return data.size();
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
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case 0:                return data.get(rowIndex).file;
            case 1:                return data.get(rowIndex).totalAvg;
            case 2:                return data.get(rowIndex).totalHigh;
            case 3:                return data.get(rowIndex).totalLow;
            case 4:                return data.get(rowIndex).totalStdDev;

            case 5:                return data.get(rowIndex).cashAvg;
            case 6:                return data.get(rowIndex).cashHigh;
            case 7:                return data.get(rowIndex).cashLow;
            case 8:                return data.get(rowIndex).cashStdDev;
        
            case 9:                return data.get(rowIndex).percent;
            case 10:               return data.get(rowIndex).spread;
        }
        return null;
    }

    public void addResult(SolverResult r) {
        data.add(r);
    }
}
