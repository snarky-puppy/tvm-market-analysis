package com.tvm.stg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;


/**
 * Created by horse on 21/1/17.
 */
public class ConfigForm {
    private static final Logger logger = LogManager.getLogger(ConfigForm.class);

    private JButton loadSymbolsButton;
    private JList<String> symbolsList;
    private JTextField dataDirText;
    private JTable compConfigTable;
    private JTable pointsConfigTable;
    private JSpinner pointsSpinner;
    private JTable slopeConfigTable;
    private JPanel panel;
    private ConfigMngrForm configMngr;
    private ConfigBean bean;

    public ConfigForm() {
        configMngr.setBeanCallback(this::applyBean);
    }

    private void applyBean(ConfigBean bean) {
        this.bean = bean;
        System.out.println("Copy bean data to widgets....");
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("Config");
        ConfigForm form = new ConfigForm();
        frame.setContentPane(form.panel);
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

}
