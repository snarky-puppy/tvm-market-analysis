package com.tvm;

import javax.swing.*;

/**
 * Created by horse on 15/10/16.
 */
public class TabbedWindow {
    private JTabbedPane tabbedPane;
    public JPanel panel;
    JTextArea logText;

    ConfigWindow configWindow;
    private ResultsWindow resultsWindow;

    public void init() {
        configWindow = new ConfigWindow(this);
        resultsWindow = new ResultsWindow();

        tabbedPane.addTab("Config", configWindow.panel);
        tabbedPane.addTab("Results", resultsWindow.panel);
    }

    public void switchToResults() {
        tabbedPane.setSelectedIndex(1);
        resultsWindow.runCalculation(configWindow.bean);
    }
}
