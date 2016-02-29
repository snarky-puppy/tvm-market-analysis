package com.tvminvestments.hcomp;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * Created by horse on 17/01/2016.
 */
public class SolverWindow implements ActionListener {
    private static final Logger logger = LogManager.getLogger(SolverWindow.class);
    private JTable resultsTable;
    private JButton runCalculationButton;
    private JButton loadFilesButton;
    private JTable paramTable;
    private JList<File> fileList;
    public JPanel panel1;
    private JSplitPane splitPane;
    private JButton clearFilesListButton;
    private JProgressBar progressBar;
    private JButton showWorkingOutButton;

    DefaultListModel<File> listModel;
    SolveParamTableModel paramTableModel;
    SolverResultTableModel resultTableModel;

    private static final int N_THREADS = (int) Math.floor(Runtime.getRuntime().availableProcessors() * 2);
    private int totalTasks;
    private int doneTasks;

    public SolverWindow()
    {
        runCalculationButton.addActionListener(this);
        loadFilesButton.addActionListener(this);
        clearFilesListButton.addActionListener(this);
        showWorkingOutButton.addActionListener(this);

        listModel = new DefaultListModel<File>();
        fileList.setModel(listModel);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                JLabel l = (JLabel) c;
                File f = (File) value;
                l.setText(f.getName());
                l.setIcon(FileSystemView.getFileSystemView().getSystemIcon(f));
                //if (pad) {
                //    l.setBorder(padBorder);
                //}
                return l;
            }
        });

        paramTableModel = new SolveParamTableModel();
        paramTable.setModel(paramTableModel);

        resultTableModel = new SolverResultTableModel();
        resultsTable.setModel(resultTableModel);
        resultsTable.setAutoCreateRowSorter(true);

        splitPane.setDividerLocation(200);

        runCalculationButton.setEnabled(false);
        progressBar.setVisible(false);
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
    }

    private void loadFiles() throws IOException, ParseException {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        chooser.setMultiSelectionEnabled(true);
        int rv = chooser.showOpenDialog(panel1);

        if (rv == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            for(File f : files) {
                listModel.addElement(f);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == runCalculationButton) {
            resultTableModel.clearResults();
            DebugWindow.getInstance().reset();
            progressBar.setVisible(true);
            runCalculationButton.setEnabled(false);
            loadFilesButton.setEnabled(false);
            clearFilesListButton.setEnabled(false);
            runCalculation();
        }

        if(e.getSource() == loadFilesButton) {
            try {
                loadFiles();
                runCalculationButton.setEnabled(listModel.size() > 0);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(panel1, ex);
            } catch (ParseException e1) {
                JOptionPane.showMessageDialog(panel1, "Parse error, date format invalid (should be dd/MM/YYYY): " + e1.toString());
            }
        }

        if(e.getSource() == clearFilesListButton) {
            listModel.clear();
            runCalculationButton.setEnabled(false);
        }

        if(e.getSource() == showWorkingOutButton) {
            CompounderLogResults.openResults();
        }
    }


    class Combination {
        public int percent;
        public int spread;
        public Combination(int p, int s) {
            percent = p;
            spread = s;
        }
    }

    private class CalculationTask extends SwingWorker<Object, SolverResult> {

        private final double bank;
        private final int percent;
        private final int spread;
        private final File file;
        private int iterations;

        public CalculationTask(File file, int spread, int investPercent, double totalBank, int iterations) {
            this.file = file;
            this.spread = spread;
            this.percent = investPercent;
            this.bank = totalBank;
            this.iterations = iterations;
        }

        @Override
        protected SolverResult doInBackground() throws Exception {
            SolverResult rv = null;
            try {

                Compounder compounder = new Compounder();

                compounder.loadFile(file);

                rv =  new SolverResult();

                SummaryStatistics cash = new SummaryStatistics();
                SummaryStatistics total = new SummaryStatistics();

                if(spread == 0 || percent == 0) {
                    logger.info("Parameters failed sanity check. Zero action means zero result. (will only run a single iteratrion");
                    iterations = 0;
                }

                int iteration = 1;
                do {
                    compounder.spread = spread;
                    compounder.investPercent = percent;
                    compounder.totalBank = bank;

                    compounder.shuffle();
                    compounder.calculate(iteration);

                    cash.addValue(compounder.balanceCash);
                    total.addValue(compounder.balanceTotal);

                    iteration++;
                } while(iteration <= iterations);

                rv.file = file;
                rv.percent = percent;
                rv.spread = spread;

                rv.cashHigh = cash.getMax();
                rv.cashLow = cash.getMin();
                rv.cashAvg = cash.getMean();
                rv.cashStdDev = cash.getStandardDeviation();

                rv.totalHigh = total.getMax();
                rv.totalLow = total.getMin();
                rv.totalAvg = total.getMean();
                rv.totalStdDev = total.getStandardDeviation();



            } catch(Exception ex) {
                logger.error("Uncaught exception", ex);
            }
            publish(rv);
            return rv;
        }

        @Override
        protected void process(List<SolverResult> chunks) {
            for(SolverResult r : chunks) {
                resultTableModel.addResult(r);
            }
            doneTasks += chunks.size();
            if(doneTasks == totalTasks) {
                progressBar.setVisible(false);

                runCalculationButton.setEnabled(true);
                loadFilesButton.setEnabled(true);
                clearFilesListButton.setEnabled(true);
                resultTableModel.fireTableDataChanged();
            } else {

                Double d = ((float)doneTasks / totalTasks) * 100.00;
                int progress = d.intValue();

                progressBar.setValue(progress);
            }
        }
    }

    private void runCalculation() {
        SolveParamTableModel solverParameters = paramTableModel;

        CompounderLogResults.reset();

        ArrayList<Combination> options = new ArrayList<>();

        for(int p = solverParameters.minPercent; p <= solverParameters.maxPercent; p += solverParameters.stepPercent) {
            for(int s = solverParameters.minSpread; s <= solverParameters.maxSpread; s += solverParameters.stepSpread) {
                logger.info(String.format("Step combo: percent=%d, spread=%d", p, s));
                options.add(new Combination(p, s));
            }
        }

        totalTasks = listModel.size() * options.size();
        doneTasks = 0;
        progressBar.setValue(0);

        for(int i = 0; i < listModel.size(); i++) {
            File f = listModel.getElementAt(i);

            logger.info(String.format("Total Combinations: %d", options.size()));

            for(Combination x : options) {
                int p = x.percent;
                int s = x.spread;
                logger.info(String.format("+++++ Solving: %s, bank=%d, percent=%d, spread=%d", f.toString(), solverParameters.startBank, p, s));

                CalculationTask task = new CalculationTask(f, s, p, solverParameters.startBank, solverParameters.iterations);
                task.execute();

                logger.info("+++++ Solver iteration finished");
            }
        }
    }
}
