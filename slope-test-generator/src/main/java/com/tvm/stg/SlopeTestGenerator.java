package com.tvm.stg;

import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by matt on 2/02/17.
 */
public class SlopeTestGenerator {
    private static final Logger logger = LogManager.getLogger(SlopeTestGenerator.class);

    public static void main(String[] args) {
        logger.error("Slope Test Generator");

        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println("classPath: "+url.getFile());
        }


        JFrame frame = new JFrame("Slope Test Generator Tool");

        ConfigForm configForm = new ConfigForm();


        GuiAppender.setTextArea(configForm.logText);
        frame.setContentPane(configForm.panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //frame.pack();

        frame.setSize(1024, 768);
        frame.setVisible(true);

        /*
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                tabbedWindow.configWindow.syncBean();
            }
        }, "Shutdown-thread"));
        */
    }
}
