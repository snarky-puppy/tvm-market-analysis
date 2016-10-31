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

    public void init() {
        configWindow = new ConfigWindow();

        tabbedPane.addTab("Config", configWindow.panel);
        //tabbedPane1.addTab("Solver", solverWindow.panel1);
        //tabbedPane1.addTab("Debug", DebugWindow.getInstance().panel1);
    }


}
