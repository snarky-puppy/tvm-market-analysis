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
    private SlopeResultsWindow slopeResultsWindow;

    public void init() {
        configForm = new ConfigForm(this);
        slopeResultsWindow = new SlopeResultsWindow();

        tabbedPane.addTab("Config", configForm.panel);
        tabbedPane.addTab("Slope Results", slopeResultsWindow.panel);
    }

    public void runCalc(ConfigBean bean) {
        tabbedPane.setSelectedIndex(1);
        slopeResultsWindow.runCalculation(bean);
    }
}
