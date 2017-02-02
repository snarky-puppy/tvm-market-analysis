package com.tvm.stg;

import javax.swing.*;

/**
 * Created by matt on 2/02/17.
 */
public class TabbedWindow {
    private JTabbedPane tabbedPane;
    JPanel panel;
    JTextArea logText;

    ConfigForm configForm;
    private ResultsWindow resultsWindow;

    public void init() {
        configForm = new ConfigForm(this);
        resultsWindow = new ResultsWindow();

        tabbedPane.addTab("Config", configForm.panel);
        tabbedPane.addTab("Results", resultsWindow.panel);
    }

    public void runCalc(ConfigBean bean) {
        tabbedPane.setSelectedIndex(1);
        resultsWindow.runCalculation(bean);
    }
}
