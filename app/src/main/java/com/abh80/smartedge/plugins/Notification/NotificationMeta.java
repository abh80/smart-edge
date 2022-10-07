package com.abh80.smartedge.plugins.Notification;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;

public class NotificationMeta {
    private String title;
    private String description;
    private Icon icon;
    private Drawable icon_drawable;
    private Bundle all;

    public NotificationMeta(String title, String description, int id, Drawable icon , Bundle all) {
        this.title = title;
        this.description = description;
        this.icon_drawable = icon;
        this.id = id;
        this.all = all;
    }

    public Bundle getAll() {
        return all;
    }

    public void setAll(Bundle all) {
        this.all = all;
    }

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Drawable getIcon_drawable() {
        return icon_drawable;
    }

    public void setIcon_drawable(Drawable icon_drawable) {
        this.icon_drawable = icon_drawable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }
}
