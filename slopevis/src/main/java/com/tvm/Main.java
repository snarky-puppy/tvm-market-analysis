package com.tvm;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

/**
 * Created by horse on 25/10/16.
 */
public class Main {

    public static void main(String[] args) {
        //MainWindow mainWindow = new MainWindow();
        JFrame frame = new JFrame("TVM Slope Vis tool");
        //frame.setContentPane(mainWindow.mainPanel);
        TabbedWindow tabbedWindow = new TabbedWindow();
        GuiAppender.setTabbedWindow(tabbedWindow);
        frame.setContentPane(tabbedWindow.panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //frame.pack();

        frame.setSize(1024, 768);
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                tabbedWindow.configWindow.syncBean();
            }
        }, "Shutdown-thread"));

        tabbedWindow.init();
    }
}
