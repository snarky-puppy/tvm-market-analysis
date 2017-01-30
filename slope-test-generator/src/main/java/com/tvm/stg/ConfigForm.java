package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import java.util.OptionalInt;


/**
 * Created by horse on 21/1/17.
 */
public class ConfigForm {
    private static final Logger logger = LogManager.getLogger(ConfigForm.class);

    private JButton loadSymbolsButton;
    private JList<String> symbolsList;
    private JTextField dataDirText;
    private JTable compConfigTable;
    private JTable pointsConfigTable;
    private JSpinner pointsSpinner;
    private JTable slopeConfigTable;
    private JPanel panel;
    private ConfigMngrForm configMngr;
    private ConfigBean bean;

    private ConfigForm() {
        configMngr.setBeanCallback(this::applyBean);
        pointsSpinner.addChangeListener(new SlopeSpinnerListener());
    }

    private void applyBean(ConfigBean bean) {
        if(bean == null)
            bean = new ConfigBean();
        this.bean = bean;

        pointsSpinner.setValue(new Integer(bean.pointDistances.size()));

        pointsConfigTable.setModel(new PointModel(bean));
        slopeConfigTable.setModel(new SlopeModel(bean));
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("Config");
        ConfigForm form = new ConfigForm();
        frame.setContentPane(form.panel);
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

    public class SlopeModel extends AbstractTableModel {

        private ConfigBean bean;

        class ColumnDef {
            String name;
            ConfigBean.Range<?> val;

            public ColumnDef(String name, ConfigBean.Range<?> val) {
                this.name = name;
                this.val = val;
            }
        }

        ColumnDef[] defs;

        SlopeModel(ConfigBean bean) {
            this.bean = bean;

            defs = new ColumnDef[]{
                    new ColumnDef("Target %", bean.targetPc),
                    new ColumnDef("Stop %", bean.stopPc),
                    new ColumnDef("Max Hold Days", bean.maxHoldDays),
                    new ColumnDef("Slope Cutoff", bean.slopeCutoff),
                    new ColumnDef("Days Dol Vol", bean.daysDolVol),
                    new ColumnDef("Min Dol Vol", bean.minDolVol),
                    new ColumnDef("Trade Start Days", bean.tradeStartDays),
                    new ColumnDef("Days Liq Vol", bean.daysLiqVol),
            };
        }

        @Override
        public int getRowCount() {
            return defs.length;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return defs[rowIndex].name;
                case 1: return defs[rowIndex].val.getStart();
                case 2: return defs[rowIndex].val.getEnd();
                case 3: return defs[rowIndex].val.getStep();
                default: return "?";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                switch (columnIndex) {
                    case 1: defs[rowIndex].val.setStart((String)aValue);
                        break;
                    case 2: defs[rowIndex].val.setEnd((String)aValue);
                        break;
                    case 3: defs[rowIndex].val.setStep((String)aValue);
                        break;
                }
                configMngr.updateBean(bean);

            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool", ex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Parameter";
                case 1: return "Start Range";
                case 2: return "End Range";
                case 3: return "Step";
                default: return "?";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0)
                return false;
            return true;
        }
    }

    public class PointModel extends AbstractTableModel {

        private ConfigBean bean;

        PointModel(ConfigBean bean) {
            this.bean = bean;
        }

        @Override
        public int getRowCount() {
            return bean.pointDistances.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int ci) {
            switch (ci) {
                case 0: return "Point "+Integer.toString(rowIndex+1);
                case 1: return bean.pointDistances.get(rowIndex).getStart();
                case 2: return bean.pointDistances.get(rowIndex).getEnd();
                case 3: return bean.pointDistances.get(rowIndex).getStep();
                default: return "?";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                ConfigBean.IntRange range = bean.pointDistances.get(rowIndex);
                int val = Integer.parseInt((String)aValue);
                switch(columnIndex) {
                    case 1:
                        if(range.getEnd() <= val) {
                            logger.error("End range must be greater than start");
                        } else {
                            range.setStart(val);
                        }
                        break;

                    case 2:
                        if(range.getStart() >= val)
                            logger.error("Start range must be less than end");
                        else
                            range.setEnd(val);
                        break;

                    case 3:
                        if(val <= 0)
                            logger.error("Negative step make no sense");
                        else
                            range.setStep(val);
                        break;
                    default:
                        logger.error("Invalid column: "+columnIndex);
                        break;
                }
                configMngr.updateBean(bean);

            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool");
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Point Number";
                case 1: return "Start";
                case 2: return "End";
                case 3: return "Step";
                default: return "?";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0)
                return false;
            return true;
        }
    }

    class SlopeSpinnerListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            Integer newVal = (Integer) pointsSpinner.getValue();
            logger.info("spinner new value: "+newVal);
            if(newVal <= 0) {
                pointsSpinner.setValue(1);
                newVal = 1;
            }

            int max = 0;
            OptionalInt optionalInt = bean.pointDistances.stream().mapToInt(ConfigBean.IntRange::getStart).max();
            if(optionalInt.isPresent())
                max = optionalInt.getAsInt();

            while(bean.pointDistances.size() > newVal)
                bean.pointDistances.remove((int)newVal);

            while(bean.pointDistances.size() < newVal) {
                max += 7;
                bean.pointDistances.add(new ConfigBean.IntRange(max, max + 7, 1));
            }

            ((AbstractTableModel)pointsConfigTable.getModel()).fireTableDataChanged();

            configMngr.updateBean(bean);
        }
    }
    
}
