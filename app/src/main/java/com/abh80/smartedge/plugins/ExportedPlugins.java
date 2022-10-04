package com.abh80.smartedge.plugins;

import com.abh80.smartedge.plugins.MediaSession.MediaSessionPlugin;
import com.abh80.smartedge.plugins.Notification.NotificationPlugin;

import java.util.ArrayList;

public class ExportedPlugins {
    public static ArrayList<BasePlugin> getPlugins() {
        ArrayList<BasePlugin> plugins = new ArrayList<>();
        plugins.add(new MediaSessionPlugin());
        plugins.add(new NotificationPlugin());
        return plugins;
    }
}
