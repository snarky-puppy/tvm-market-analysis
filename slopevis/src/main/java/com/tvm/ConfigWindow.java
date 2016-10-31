package com.tvm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdatepicker.JDateComponentFactory;
import org.jdatepicker.JDatePicker;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration panel
 *
 * Created by horse on 15/10/16.
 */
public class ConfigWindow {
    private static final Logger logger = LogManager.getLogger(ConfigWindow.class);
    private static final Path configFileName = Paths.get("slopevis.json");
    private ObjectMapper mapper;

    JPanel panel;
    private JTextField dataDirText;
    private JList<String> symbolsList;
    private JTable paramTable;
    private JPanel pickerPanel;
    private JButton loadButton;

    JDatePicker pickerTo, pickerFrom;

    ConfigBean bean;
    ConfigParamListModel listModel;

    public ConfigWindow() {
        try {
            mapper = new ObjectMapper();

            if (Files.exists(configFileName)) {
                logger.info("loading previous configuration");
                bean = mapper.readValue(configFileName.toFile(), ConfigBean.class);
                logger.info(bean.toString());
                if(bean.dataDir != null)
                    dataDirText.setText(bean.dataDir.toString());
            } else {
                logger.info("No previous configuration, resetting to zero");
                bean = new ConfigBean();
            }

            listModel = new ConfigParamListModel(bean);

            paramTable.setModel(new ConfigParamTableModel(bean));
            symbolsList.setModel(listModel);



            dataDirText.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int rv = chooser.showOpenDialog(panel);
                    if (rv == JFileChooser.APPROVE_OPTION) {
                        bean.dataDir = chooser.getSelectedFile().toPath();
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
                        //update();
                    } catch (IOException | ParseException ex) {
                        logger.error(ex);
                    }
                }
            });

            if(bean.fromDate == null)
                bean.fromDate = Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            if(bean.toDate == null)
                bean.toDate = Date.from(LocalDate.now().minusYears(5).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            pickerPanel.setLayout(new FlowLayout());

            JPanel DatePanel = new JPanel();
            DatePanel.setLayout(new BorderLayout());

            JPanel jPanel = new JPanel(new FlowLayout());
            pickerFrom = new JDateComponentFactory().createJDatePicker(bean.fromDate);
            pickerFrom.setTextEditable(true);
            pickerFrom.setShowYearButtons(true);
            jPanel.add(new JLabel("Date From:"));
            jPanel.add((JComponent)pickerFrom);
            DatePanel.add(jPanel, BorderLayout.NORTH);

            jPanel = new JPanel(new FlowLayout());
            pickerTo = new JDateComponentFactory().createJDatePicker(bean.toDate);
            pickerTo.setTextEditable(true);
            pickerTo.setShowYearButtons(true);
            jPanel.add(new JLabel("Date To:"));
            jPanel.add((JComponent)pickerTo);
            DatePanel.add(jPanel, BorderLayout.SOUTH);

            pickerPanel.add(DatePanel);

            pickerTo.getModel().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals("value")) {
                        bean.toDate = (Date)evt.getNewValue();
                    }
                }
            });

            pickerFrom.getModel().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals("value")) {
                        bean.fromDate = (Date)evt.getNewValue();
                    }
                }
            });


        } catch(IOException e) {
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
            bean.symbols = new ArrayList<>(hashSet);

            symbolsList.setModel(listModel);
        }
    }

    public class ConfigParamListModel implements ListModel<String> {
        //private static final Logger logger = LogManager.getLogger(ConfigParamListModel.class);

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
            logger.info(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            logger.info(l);
        }
    }

    /**
     * Model backend for config parameters jtable
     *
     * Created by horse on 25/10/16.
     */
    public static class ConfigParamTableModel extends AbstractTableModel {

        private static final Logger logger = LogManager.getLogger(ConfigParamTableModel.class);

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
                    case 4: bean.daysDolVol = Double.parseDouble((String) aValue); break;

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
}
