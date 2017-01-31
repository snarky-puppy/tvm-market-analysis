package com.tvm.stg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matt on 27/01/2017.
 */
public class ConfigMngrForm extends JPanel {
    private static final Logger logger = LogManager.getLogger(ConfigMngrForm.class);

    private static File home;
    ObjectMapper mapper = new ObjectMapper();

    static {
        String userHome = System.getProperty("user.home");
        userHome = userHome.replace("\\", "/"); // to support all platforms.
        home = new File(userHome+"/SlopeTestGenerator");
        if(!home.exists()) {
            home.mkdir();
        }
    }

    interface BeanCallback {
        void updateBean(ConfigBean b);
    }

    BeanCallback beanCallback = null;

    private JComboBox<String> configCombo;
    private JButton newBtn;
    private JButton renameBtn;
    private JButton deleteBtn;
    private JPanel panel;

    ConfigMngrForm() {
        updateConfigCombo();

        configCombo.addItemListener(e -> {
            String s = (String) e.getItem();
            System.out.printf("%s: %s\n", e.getStateChange() == ItemEvent.DESELECTED ? "DESELECTED" : "SELECTED", s);
            if(e.getStateChange() == ItemEvent.SELECTED) {
                loadBean(s);
            }
        });

        newBtn.addActionListener(e -> {
            String s = (String)JOptionPane.showInputDialog(
                    panel,
                    "New configuration name:",
                    "New configuration",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "Untitled Configuration");
            ConfigBean bean = new ConfigBean();
            saveBean(s, bean);
            beanCallback.updateBean(bean);

            configCombo.insertItemAt(s, 0);
            configCombo.setSelectedIndex(0);
        });

        renameBtn.addActionListener(e -> {
            String oldName = (String)configCombo.getSelectedItem();
            if(oldName == null)
                return;
            String newName = (String)JOptionPane.showInputDialog(
                    panel,
                    "New configuration name:",
                    "New configuration",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    oldName);
            System.out.println("old="+oldName+" new="+newName);
            if(newName == null || newName.equals(oldName))
                return;
            ConfigBean bean = null;
            try {
                bean = mapper.readValue(getConfigFile(oldName), ConfigBean.class);
                Files.delete(getConfigFile(oldName).toPath());
            } catch (IOException e1) {
                logger.error(e1);
            }
            if(bean != null) {
                saveBean(newName, bean);
                configCombo.removeItem(oldName);
                configCombo.insertItemAt(newName, 0);
                configCombo.setSelectedIndex(0);
            }
        });

        deleteBtn.addActionListener(e -> {
            String oldName = (String)configCombo.getSelectedItem();
            try {
                Files.delete(getConfigFile(oldName).toPath());
            } catch (IOException e1) {
                logger.error(e1);
            }
            configCombo.removeItemAt(configCombo.getSelectedIndex());
            if(configCombo.getItemCount() == 0) {
                initEmptyConfig();
            }
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

        configCombo.removeAllItems();
        for(String s : configFiles)
            configCombo.addItem(s);

        if(configCombo.getItemCount() == 0) {
            initEmptyConfig();
        }
    }

    private void initEmptyConfig() {
        // nothing in the directory, create something
        String name = "Untitled Configuration";
        saveBean(name, new ConfigBean());
        configCombo.addItem(name);
    }


    public File getConfigFile(String name) {
        String s = name + ".json";
        return Paths.get(home.toString(), s).toFile();
    }

    private void saveBean(String name, ConfigBean bean) {
        try {
            mapper.writeValue(getConfigFile(name), bean);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void loadBean(String name) {
        try {
            ConfigBean bean = mapper.readValue(getConfigFile(name), ConfigBean.class);
            if(beanCallback != null)
                beanCallback.updateBean(bean);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    void setBeanCallback(BeanCallback beanCallback) {
        this.beanCallback = beanCallback;
        String name = (String)configCombo.getSelectedItem();
        if(name != null)
            loadBean(name);
    }

    public void updateBean(ConfigBean bean) {
        String name = (String)configCombo.getSelectedItem();
        saveBean(name, bean);
    }

}
