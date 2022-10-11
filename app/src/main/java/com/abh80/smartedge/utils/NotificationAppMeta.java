package com.abh80.smartedge.utils;

import android.graphics.drawable.Drawable;

public class NotificationAppMeta {
    public String name;
    public String package_name;
    public Drawable app_icon;
    public boolean enabled;

    public NotificationAppMeta(String name, String package_name, Drawable app_icon) {
        this.name = name;
        this.package_name = package_name;
        this.app_icon = app_icon;
    }
}
