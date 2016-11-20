package com.tvm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by horse on 25/10/16.
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.error("SlopeVis");

        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }


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
