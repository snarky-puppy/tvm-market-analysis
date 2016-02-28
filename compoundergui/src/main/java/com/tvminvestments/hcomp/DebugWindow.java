package com.tvminvestments.hcomp;

import javax.swing.*;

/**
 * Created by horse on 16/01/2016.
 */
public class DebugWindow {
    private JTextArea debugText;
    public JPanel panel1;

    private static DebugWindow instance;

    private DebugWindow() {

    }

    public static DebugWindow getInstance() {
        if(instance == null) {
            synchronized (DebugWindow.class) {
                instance = new DebugWindow();
            }
        }
        return instance;
    }

    public void doLog(String msg) {
        debugText.append(msg);
    }

    public void reset() {
        debugText.setText("");
    }
}
