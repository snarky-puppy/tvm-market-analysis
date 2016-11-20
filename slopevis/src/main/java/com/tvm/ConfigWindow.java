package com.tvm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.awt.event.ActionEvent.ACTION_PERFORMED;

/**
 * Configuration panel
 *
 * Created by horse on 15/10/16.
 */
public class ConfigWindow {
    private static final Logger logger = LogManager.getLogger(ConfigWindow.class);
    private static final Path configFileName = Paths.get("slopevis.json");
    private PointModel pointModel = null;
    private ObjectMapper mapper = new ObjectMapper();

    JPanel panel;
    private JTextField dataDirText;
    private JList<String> symbolsList;
    private JTable paramTable;
    private JPanel pickerPanel;
    private JButton loadButton;
    private JTable slopeConfigTable;
    private JSpinner pointSpinner;
    private JTextField fromDateText;
    private JTextField toDateText;
    private JButton calcBtn;

    ConfigBean bean;
    private TabbedWindow tabbedWindow;

    public ConfigWindow(TabbedWindow tabbedWindow) {
        this.tabbedWindow = tabbedWindow;
        System.out.println("ConfigWindow ctor");

        loadBean();

        pointModel = new PointModel(bean);

        paramTable.setModel(new ConfigParamTableModel(bean));
        slopeConfigTable.setModel(pointModel);
        symbolsList.setModel(new ConfigParamListModel(bean));

        pointSpinner.setValue(bean.pointDistances.size());

        pointSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Integer newVal = (Integer) pointSpinner.getValue();
                if(newVal <= 0) {
                    pointSpinner.setValue(1);
                    newVal = 1;
                }

                while(bean.pointDistances.size() > newVal)
                    bean.pointDistances.remove((int)newVal);

                while(bean.pointDistances.size() < newVal)
                    bean.pointDistances.add(7);

                pointModel.fireTableDataChanged();
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
                    syncBean();
                }
            }
        });

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    loadFile();
                    FileFinder.setSymbols(bean.symbols);
                    //update();
                } catch (IOException | ParseException ex) {
                    logger.error(ex);
                }
            }
        });

        if(bean.fromDate == null)
            bean.fromDate = LocalDate.now();

        if(bean.toDate == null)
            bean.toDate = LocalDate.now().minusYears(5);

        toDateText.setInputVerifier(new DateVerifier(false));
        fromDateText.setInputVerifier(new DateVerifier(true));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        toDateText.setText(formatter.format(bean.toDate));
        fromDateText.setText(formatter.format(bean.fromDate));

        calcBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getID() == ACTION_PERFORMED)
                    tabbedWindow.switchToResults();
            }
        });
    }

    private void loadBean() {

        try {

            if (Files.exists(configFileName)) {
                logger.info("loading previous configuration");
                bean = mapper.readValue(configFileName.toFile(), ConfigBean.class);
                logger.info(bean.toString());
                if (bean.dataDir != null) {
                    dataDirText.setText(bean.dataDir.toString());
                    FileFinder.setBaseDir(bean.dataDir);
                }
                FileFinder.setSymbols(bean.symbols);
            } else {
                logger.info("No previous configuration, resetting to zero");
                bean = new ConfigBean();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }

    }


    public void syncBean() {
        logger.info("Syncing bean");
        try {
            mapper.writeValue(configFileName.toFile(), bean);
            logger.info("done");
        } catch (IOException e1) {
            logger.error(e1);
        }
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

            symbolsList.setModel(new ConfigParamListModel(bean));
        }
    }

    public class ConfigParamListModel implements ListModel<String> {
        private ConfigBean bean;

        ConfigParamListModel(ConfigBean bean) {
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
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int ci) {
            if(ci == 0)
                return "Point "+rowIndex;
            else
                return bean.pointDistances.get(rowIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            logger.info(String.format("Setting param value %d/%d: %s", rowIndex, columnIndex, (String)aValue));
            try {
                bean.pointDistances.set(rowIndex, Integer.parseInt((String) aValue));
            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool");
            }
        }

        @Override
        public String getColumnName(int column) {
            if(column == 0)
                return "Point Number";
            return "Distance day0";

        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0)
                return false;
            return true;
        }
    }

    /**
     * Model backend for config parameters jtable
     *
     * Created by horse on 25/10/16.
     */
    public static class ConfigParamTableModel extends AbstractTableModel {

        private ConfigBean bean;

        ConfigParamTableModel(ConfigBean bean) {
            this.bean = bean;
        }

        @Override
        public int getRowCount() {
            return 6;
        }

        @Override
        public int getColumnCount() {
            return 2;
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
                    return ci == 0 ? "Days Dol Vol" : Double.toString(bean.daysDolVol);

                case 5:
                    return ci == 0 ? "Min Dol Vol" : Double.toString(bean.minDolVol);
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

                }
            } catch(NumberFormatException ex) {
                logger.error("NumberFormatException: "+(String)aValue +" just isn't cool");
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

    private class DateVerifier extends InputVerifier {

        private boolean isFromDate;

        public DateVerifier(boolean isFromDate) {
            this.isFromDate = isFromDate;
        }

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                if(isFromDate)
                    bean.fromDate = LocalDate.parse(text, formatter);
                else
                    bean.toDate = LocalDate.parse(text, formatter);
                logger.info("Date Validated");
                return true;
            } catch (DateTimeParseException e) {
                logger.error("Invalid Date format: "+text+" expecting dd/MM/yyyy");
                return false;
            }
        }
    }
}
