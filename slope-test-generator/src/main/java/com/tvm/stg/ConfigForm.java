package com.tvm.stg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    private JButton deleteBtn;
    private JComboBox<String> configCombo;
    private JButton newBtn;
    private JPanel panel;
    private JButton renameButton;

    ObjectMapper mapper = new ObjectMapper();
    ConfigBean bean;

    private static File home;

    static {
        String userHome = System.getProperty("user.home");
        userHome = userHome.replace("\\", "/"); // to support all platforms.
        home = new File(userHome+"/SlopeTestGenerator");
        if(!home.exists()) {
            home.mkdir();
        }
    }

    private File configFileName;

    public ConfigForm() {

        ////////////////////////////////////////////////////////////////////////////////////////
        // configuration files
        updateConfigCombo();
        configCombo.setSelectedIndex(0);

        configCombo.addItemListener(e -> {

            String s = (String) e.getItem();
            System.out.printf("%s: %s\n", e.getStateChange() == ItemEvent.DESELECTED ? "DESELECTED" : "SELECTED", s);
            if(e.getStateChange() == ItemEvent.DESELECTED) {
                saveBean(s);
            } else {
                loadBean(s);
            }
        });

        newBtn.addActionListener(e -> {
            saveBean((String) configCombo.getSelectedItem());
            String s = (String)JOptionPane.showInputDialog(
                    panel,
                    "New configuration name:",
                    "New configuration",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "Untitled Configuration");
            bean = new ConfigBean();
            saveBean(s);

            configCombo.insertItemAt(s, 0);
            configCombo.setSelectedIndex(0);
        });

        deleteBtn.addActionListener(e -> {
            try {
                Files.delete(getConfigFile((String) configCombo.getSelectedItem()).toPath());
            } catch (IOException e1) {
                logger.error(e1);
            }
            configCombo.removeItemAt(configCombo.getSelectedIndex());
        });
    }

    private void updateConfigCombo() {
        List<String> configFiles = new ArrayList<>();

        configFiles.clear();
        try {
            Files.walk(home.toPath(), FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> configFiles.add(p.getFileName().toString().replace(".json", "")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if(configFiles.size() == 0)
            configFiles.add("Untitled Configuration");

        configCombo.removeAllItems();
        for(String s : configFiles)
            configCombo.addItem(s);
    }


    public File getConfigFile(String name) {
        String s = name + ".json";
        return Paths.get(home.toString(), s).toFile();
    }

    private void saveBean(String name) {
        try {
            mapper.writeValue(getConfigFile(name), bean);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void loadBean(String name) {
        try {
            bean = mapper.readValue(getConfigFile(name), ConfigBean.class);
            applyBean();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void applyBean() {
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
