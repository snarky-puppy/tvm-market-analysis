package com.tvminvestments.hcomp;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by horse on 11/02/2016.
 */
public class TabbedWindow implements ActionListener {
    private JTabbedPane tabbedPane1;
    public JPanel mainPanel;

    public void init() {
        // TODO: place custom component creation code here
        MainWindow mainWindow = new MainWindow();
        SolverWindow solverWindow = new SolverWindow();
        tabbedPane1.addTab("Main", mainWindow.mainPanel);
        tabbedPane1.addTab("Solver", solverWindow.panel1);
        tabbedPane1.addTab("Debug", DebugWindow.getInstance().panel1);


    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
