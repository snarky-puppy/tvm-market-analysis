package com.tvminvestments.hcomp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;


/**
 * Created by horse on 19/12/2015.
 */
public class MainWindow implements ActionListener, ListSelectionListener {
    private static final Logger logger = LogManager.getLogger(MainWindow.class);
    public JPanel mainPanel;
    private JButton loadButton;
    private JTable table;
    private JTextField spreadText;
    private JTextField investPercentText;
    private JTextField bankText;
    private JButton recalcButton;
    private JTextField balanceCash;
    private JTextField balanceTrades;
    private JTextField balanceTotal;
    private JButton showWorkingOutButton;
    private JCheckBox shuffleCheckBox;
    private JTextField rollover;

    int selectedRow = -1;
    int matchingRow = -1;

    NumberFormat currency = NumberFormat.getCurrencyInstance();
    static NumberFormat percent = NumberFormat.getPercentInstance();


    private CompoundTableModel model;

    public MainWindow() {
        percent.setMaximumFractionDigits(2);
        loadButton.addActionListener(this);
        recalcButton.addActionListener(this);
        showWorkingOutButton.addActionListener(this);

        model = new CompoundTableModel();

        spreadText.setText(Integer.toString(model.getCompounder().spread));
        investPercentText.setText(Double.toString(model.getCompounder().investPercent));
        bankText.setText(Double.toString(model.getCompounder().startBank));
        rollover.setText(Integer.toString(model.getCompounder().profitRollover));

/*
        DefaultTableCellRenderer dateRender = new DefaultTableCellRenderer() {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yy");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                value = f.format(value);
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
        dateRender.setHorizontalAlignment(SwingConstants.RIGHT);
*/
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {

            public Color oldBg = null;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if(value != null) {
                    if(value instanceof Double) {
                        switch(column) {
                            case 1:
                            case 3:
                            case 4:
                            case 6:
                            case 7:
                                value = currency.format(value);
                                break;
                            case 5:
                                value = percent.format(value);
                                break;
                        }
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };

        table.setModel(model);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setDefaultRenderer(Double.class, cellRenderer);
        table.getSelectionModel().addListSelectionListener(this);

        logger.info("Initialised");

    }

    private void loadFile() throws IOException, ParseException {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        chooser.setMultiSelectionEnabled(false);
        int rv = chooser.showOpenDialog(mainPanel);

        if (rv == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            model.getCompounder().loadFile(file);
        }
    }



    private void update() {
        NumberFormat currency = NumberFormat.getCurrencyInstance();

        model.getCompounder().spread = Integer.parseInt(spreadText.getText());
        model.getCompounder().investPercent = Double.parseDouble(investPercentText.getText());
        model.getCompounder().startBank = Double.parseDouble(bankText.getText());
        model.getCompounder().profitRollover = Integer.parseInt(rollover.getText());

        if(shuffleCheckBox.isSelected())
            model.getCompounder().shuffle();

        model.calculate();

        balanceCash.setText(currency.format(model.getCompounder().balanceCash));
        balanceTrades.setText(currency.format(model.getCompounder().balanceTrades));
        balanceTotal.setText(currency.format(model.getCompounder().balanceTotal));
    }


    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == showWorkingOutButton) {
            CompounderLogResults.openResults();
        }

        if (e.getSource() == recalcButton) {
            CompounderLogResults.reset();
            DebugWindow.getInstance().reset();
            update();
        }

        if (e.getSource() == loadButton) {
            try {
                loadFile();
                update();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainPanel, ex);
            } catch (ParseException e1) {
                JOptionPane.showMessageDialog(mainPanel, "Parse error, date format invalid (should be dd/MM/yyyy): " + e1.toString());
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting())
            return;

        int selectedOld = selectedRow, matchingOld = matchingRow;
        int min, max;

        int selected = table.getSelectedRow();
        if(selected != -1) {
            int matching = model.findMatchingRow(selected);
            if(matching != -1) {

                selectedRow = selected;
                matchingRow = matching;

                min = Math.min(selectedRow, matchingRow);
                max = Math.max(selectedRow, matchingRow);

                if (selectedOld != -1 && matchingOld != -1) {
                    if (selectedOld > matchingOld) {
                        max = Math.max(max, selectedOld);
                        min = Math.min(min, matchingOld);
                    } else {
                        max = Math.max(max, matchingOld);
                        min = Math.min(min, selectedOld);
                    }
                }
                model.fireTableRowsUpdated(min, max);
            }
        }
    }

    private void createUIComponents() {
        table = new JTable()
        {
            public Component prepareRenderer(
                    TableCellRenderer renderer, int row, int column)
            {
                Component c = super.prepareRenderer(renderer, row, column);
                JComponent jc = (JComponent)c;

                if(row == selectedRow || row == matchingRow) {
                    //System.out.printf("PrepRend: %d (%d %d)\n", row, selectedRow, matchingRow);
                    jc.setBorder(new MatteBorder(1, 0, 1, 0, Color.RED));
                }

                return c;
            }
        };
    }
}
