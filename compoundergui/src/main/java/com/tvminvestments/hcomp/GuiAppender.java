package com.tvminvestments.hcomp;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.table.AbstractTableModel;
import java.io.Serializable;

/**
 * Created by horse on 16/01/2016.
 */
@Plugin(name = "GuiAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiAppender extends AbstractAppender {

    protected GuiAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        System.out.println("GuiAppender ctor");
    }

    @Override
    public void append(LogEvent event) {
        //System.out.println("GuiAppender: append");
        DebugWindow.getInstance().doLog(new String(getLayout().toByteArray(event)));
    }

    @PluginFactory
    public static GuiAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {

        System.out.println("GuiAppender factory");
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
