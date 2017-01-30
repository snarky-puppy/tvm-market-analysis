package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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

        pointsSpinner.addChangeListener(e -> {
            Integer newVal = (Integer) pointsSpinner.getValue();
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
                bean.pointDistances.add(new ConfigBean.IntRange(max, max + 7));
            }

            ((AbstractTableModel)pointsConfigTable.getModel()).fireTableDataChanged();

            configMngr.updateBean(bean);
        });

    }

    private void applyBean(ConfigBean bean) {
        this.bean = bean;
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

    public static class SlopeModel extends AbstractTableModel {

        private ConfigBean bean;

        SlopeModel(ConfigBean bean) {
            this.bean = bean;
        }

        @Override
        public int getRowCount() {
            return 8;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int ci) {
            switch (rowIndex) {
                case 0:
                    return ci == 0 ? "Target %" : Double.toString(bean.targetPc);
                case 1:
                    return ci == 0 ? "Stop %" : Double.toString(bean.stopPc);
                case 2:
                    return ci == 0 ? "Max Hold Days" : Double.toString(bean.maxHoldDays);
                case 3:
                    return ci == 0 ? "Slope Cutoff" : Double.toString(bean.slopeCutoff);
                case 4:
                    return ci == 0 ? "Days Dol Vol" : Integer.toString(bean.daysDolVol);

                case 5:
                    return ci == 0 ? "Min Dol Vol" : Double.toString(bean.minDolVol);

                case 6:
                    return ci == 0 ? "Trade Start Days" : Integer.toString(bean.tradeStartDays);

                case 7:
                    return ci == 0 ? "Days Liq Vol" : Integer.toString(bean.daysLiqVol);

                default:
                    return "n/a";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                switch (rowIndex) {
                    case 0: bean.targetPc = Double.parseDouble((String) aValue); break;
                    case 1: bean.stopPc = Double.parseDouble((String) aValue); break;

                    case 2: bean.maxHoldDays = Double.parseDouble((String) aValue); break;
                    case 3: bean.slopeCutoff = Double.parseDouble((String) aValue); break;
                    case 4: bean.daysDolVol = Integer.parseInt((String) aValue); break;

                    case 5: bean.minDolVol = Double.parseDouble((String) aValue); break;

                    case 6: bean.tradeStartDays = Integer.parseInt((String) aValue); break;

                    case 7: bean.daysLiqVol = Integer.parseInt((String) aValue); break;


                }
            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool", ex);
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
        }
    }

    public static class PointModel extends AbstractTableModel {

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
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int ci) {
            if(ci == 0)
                return "Point "+rowIndex;
            else
                if(ci == 1)
                    return bean.pointDistances.get(rowIndex).getStart();
                else
                    return bean.pointDistances.get(rowIndex).getEnd();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                ConfigBean.IntRange range = bean.pointDistances.get(rowIndex);
                int val = Integer.parseInt((String)aValue);
                if(columnIndex == 1) {
                    if(range.getEnd() <= val)
                        logger.error("End range must be greater than start");
                    else {
                        range.setStart(val);
                    }
                } else if(columnIndex == 2) {
                    if(range.getStart() >= val)
                        logger.error("Start range must be less than end");
                    else
                        range.setEnd(val);
                } else
                    logger.error("Invalid column: "+columnIndex);

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
                default: return "???";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0)
                return false;
            return true;
        }
    }
    
}
