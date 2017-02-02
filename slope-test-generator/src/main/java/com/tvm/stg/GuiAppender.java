package com.tvm.stg;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import java.io.Serializable;

/**
 * Custom log4j plugin to log to JTextArea
 *
 * Created by horse on 16/01/2016.
 */
@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiAppender extends AbstractAppender {

    private static TabbedWindow tabbedWindow;

    private static final Object lock = new Object();

    protected GuiAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        System.out.println("GuiAppender ctor");
    }

    public static void setTabbedWindow(TabbedWindow tabbedWindow) {
        GuiAppender.tabbedWindow = tabbedWindow;
    }

    @Override
    public void append(LogEvent event) {
        //System.out.println("GuiAppender: append");
        synchronized (lock) {
            if (tabbedWindow != null && tabbedWindow.logText != null) {
                JTextArea area = tabbedWindow.logText;
                area.append(new String(getLayout().toByteArray(event)));
                area.setCaretPosition(area.getDocument().getLength());
            } else {
                System.out.println("Log: "+event.toString());
            }
        }
    }

    @PluginFactory
    public static GuiAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {

        if (name == null) {
            System.out.println("GuiAppender no name error");
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new GuiAppender(name, filter, layout, true);
    }
}
