package com.tvminvestments.hcomp;

import javax.swing.*;

/**
 * Created by horse on 19/12/2015.
 */
public class HComp {

    public static void main(String[] args) {
        //MainWindow mainWindow = new MainWindow();
        JFrame frame = new JFrame("TVM Compounder tool");
        //frame.setContentPane(mainWindow.mainPanel);
        TabbedWindow tabbedWindow = new TabbedWindow();
        frame.setContentPane(tabbedWindow.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);

        tabbedWindow.init();

    }
}
