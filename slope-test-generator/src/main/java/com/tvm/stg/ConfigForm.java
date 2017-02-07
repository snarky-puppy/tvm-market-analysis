package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.OptionalInt;


/**
 * Created by horse on 21/1/17.
 */
public class ConfigForm {
    private static final Logger logger = LogManager.getLogger(ConfigForm.class);
    public static long MAX_COMBOS = 100000;


    private JButton loadSymbolsButton;
    private JList<String> symbolsList;
    private JTextField dataDirText;
    private JTable compConfigTable;
    private JTable pointsConfigTable;
    private JSpinner pointsSpinner;
    private JTable slopeConfigTable;
    protected JPanel panel;
    private ConfigMngrForm configMngr;
    private JButton goBtn;
    private JPanel goPanel;
    private JTable table1;
    private JLabel goLabel;
    private ConfigBean bean;
    private TabbedWindow tabbedWindow;

    ConfigForm(TabbedWindow tabbedWindow) {
        this.tabbedWindow = tabbedWindow;
        configMngr.setBeanCallback(this::applyBean);
        pointsSpinner.addChangeListener(new SlopeSpinnerListener());

        loadSymbolsButton.addActionListener(e -> {
            try {
                loadFile();
                FileFinder.setSymbols(bean.symbols);
            } catch (IOException | ParseException ex) {
                logger.error(ex);
            }
        });

        dataDirText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int rv = chooser.showOpenDialog(panel);
                if (rv == JFileChooser.APPROVE_OPTION) {
                    bean.dataDir = chooser.getSelectedFile().toPath();
                    FileFinder.setBaseDir(bean.dataDir);
                    dataDirText.setText(bean.dataDir.toString());
                    updateBean();
                }
            }
        });

        goBtn.addActionListener(e -> {
            tabbedWindow.runCalc(bean);
        });
    }

    private void applyBean(ConfigBean bean) {
        if(bean == null)
            bean = new ConfigBean();
        this.bean = bean;

        if(bean.dataDir == null)
            dataDirText.setText(null);
        else
            dataDirText.setText(bean.dataDir.toString());

        pointsSpinner.setValue(new Integer(bean.pointDistances.size()));

        pointsConfigTable.setModel(new PointModel(bean));
        slopeConfigTable.setModel(new SlopeModel(bean));
        symbolsList.setModel(new SymbolListModel(bean));
        compConfigTable.setModel(new CompounderModel(bean));
        FileFinder.setSymbols(bean.symbols);

        updateGoState();
    }

    private void updateGoState() {
        if(bean.pointDistances == null || bean.pointDistances.size() == 0) {
            goLabel.setText("No point distances");
            goLabel.setForeground(Color.RED);
            goBtn.setEnabled(false);
            return;
        }

        BigInteger cnt = BigInteger.valueOf(bean.getPointRanges().size());
        cnt = cnt.multiply(BigInteger.valueOf(bean.targetPc.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.stopPc.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.maxHoldDays.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.slopeCutoff.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.daysDolVol.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.minDolVol.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.tradeStartDays.permute().size()));
        cnt = cnt.multiply(BigInteger.valueOf(bean.daysLiqVol.permute().size()));
        logger.info("Combos: "+cnt.longValue());
        if(cnt.compareTo(BigInteger.valueOf(MAX_COMBOS)) > 0) {
            String s = String.format("Max combos reached (%d). Try reducing the variation.", cnt);
            goLabel.setText(s);
            goLabel.setForeground(Color.RED);
            goBtn.setEnabled(false);
            return;
        }
        goLabel.setText("Everything seems OK");
        goLabel.setForeground(Color.GREEN.darker());
        goBtn.setEnabled(true);
    }

    private void loadFile() throws IOException, ParseException {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        chooser.setMultiSelectionEnabled(false);
        int rv = chooser.showOpenDialog(panel);

        if (rv == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            if(bean.symbols != null)
                bean.symbols.clear();

            HashSet<String> hashSet = new HashSet<>();

            while((line = br.readLine()) != null) {
                String[] data = line.split("[,\\t]");
                if(data[0] != null)
                    hashSet.add(data[0]);
            }

            logger.info("Read "+hashSet.size()+" symbols");
            bean.symbols = new ArrayList<>(hashSet);
            updateBean();

            symbolsList.setModel(new SymbolListModel(bean));
            FileFinder.setSymbols(bean.symbols);
        }
    }

    public class SymbolListModel implements ListModel<String> {
        private ConfigBean bean;

        SymbolListModel(ConfigBean bean) {
            this.bean = bean;
        }

        @Override
        public int getSize() {
            if(bean.symbols == null)
                return 0;
            return bean.symbols.size();
        }

        @Override
        public String getElementAt(int index) {
            if(bean.symbols == null)
                return null;
            return bean.symbols.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
        }
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
            return 5;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex == 4)
                return Boolean.class;
            else
                return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return defs[rowIndex].name;
            if(columnIndex == 1)
                return defs[rowIndex].val.getStart();
            if(columnIndex == 4)
                return defs[rowIndex].val.getIsRange();

            if(defs[rowIndex].val.getIsRange()) {
                switch (columnIndex) {
                    case 2:
                        return defs[rowIndex].val.getEnd();
                    case 3:
                        return defs[rowIndex].val.getStep();
                    default: return "?";
                }
            } else
                return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %s/%d: %s", defs[rowIndex].name, columnIndex, aValue));
            try {
                switch (columnIndex) {
                    case 1: defs[rowIndex].val.setStart((String)aValue);
                        break;
                    case 2: defs[rowIndex].val.setEnd((String)aValue);
                        break;
                    case 3: defs[rowIndex].val.setStep((String)aValue);
                        break;
                    case 4: defs[rowIndex].val.setIsRange((Boolean)aValue);
                            fireTableDataChanged();
                        break;
                }
                updateBean();

            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+aValue +" just isn't cool", ex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Parameter";
                case 1: return "Start Range";
                case 2: return "End Range";
                case 3: return "Step";
                case 4: return "Is Range";
                default: return "?";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0)
                return false;
            if(columnIndex == 1 || columnIndex == 4)
                return true;
            return defs[rowIndex].val.getIsRange();
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
                            logger.error("End isRange must be greater than start");
                        } else {
                            range.setStart(val);
                        }
                        break;

                    case 2:
                        if(range.getStart() >= val)
                            logger.error("Start isRange must be less than end");
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
                updateBean();

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
            return columnIndex != 0;
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
                bean.pointDistances.add(new ConfigBean.IntRange(max, max + 7, 1, true));
            }

            ((AbstractTableModel)pointsConfigTable.getModel()).fireTableDataChanged();

            updateBean();
        }
    }

    public class CompounderModel extends AbstractTableModel {

        private ConfigBean bean;

        public CompounderModel(ConfigBean bean) {
            this.bean = bean;
        }

        @Override
        public int getRowCount() {
            return 9;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int ci) {

            switch(rowIndex) {
                case 0: return ci == 0 ? "Start Bank" : Integer.toString(bean.startBank);
                case 1: return ci == 0 ? "Profit Roll" : Integer.toString(bean.profitRollover);

                case 2: return ci == 0 ? "Min %" : Double.toString(bean.minPercent);
                case 3: return ci == 0 ? "Max %" : Double.toString(bean.maxPercent);
                case 4: return ci == 0 ? "Step %" : Double.toString(bean.stepPercent);

                case 5: return ci == 0 ? "Min Spread" : Integer.toString(bean.minSpread);
                case 6: return ci == 0 ? "Max Spread" : Integer.toString(bean.maxSpread);
                case 7: return ci == 0 ? "Step Spread" : Integer.toString(bean.stepSpread);

                case 8: return ci == 0 ? "Iterations" : Integer.toString(bean.iterations);

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
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                switch (rowIndex) {
                    case 0: bean.startBank = Integer.parseInt((String) aValue); break;
                    case 1: bean.profitRollover = Integer.parseInt((String) aValue); break;


                    case 2: bean.minPercent = Double.parseDouble((String) aValue); break;
                    case 3: bean.maxPercent = Double.parseDouble((String) aValue); break;
                    case 4: bean.stepPercent = Double.parseDouble((String) aValue); break;

                    case 5: bean.minSpread = Integer.parseInt((String) aValue); break;
                    case 6: bean.maxSpread = Integer.parseInt((String) aValue); break;
                    case 7: bean.stepSpread = Integer.parseInt((String) aValue); break;
                    case 8: bean.iterations = Integer.parseInt((String) aValue); break;

                }
                updateBean();
            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool");
            }
        }
    }

    private void updateBean() {
        configMngr.updateBean(bean);
        updateGoState();
    }
}
