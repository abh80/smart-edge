package com.abh80.smartedge.plugins;

import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;

import java.util.ArrayList;

public abstract class BasePlugin {
    public abstract String getID();
    public abstract String getName();

    public abstract void onCreate(OverlayService context);

    public abstract View onBind();

    public abstract void onUnbind();

    public abstract void onDestroy();

    public abstract void onExpand();

    public abstract void onCollapse();

    public abstract void onClick();

    public abstract String[] permissionsRequired();

    public void onEvent(AccessibilityEvent event) {
    }

    public abstract ArrayList<SettingStruct> getSettings();

    public void onBindComplete() {
    }

    ;
}
