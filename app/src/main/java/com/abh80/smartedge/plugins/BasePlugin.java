package com.abh80.smartedge.plugins;

import android.view.View;

import com.abh80.smartedge.services.OverlayService;

public abstract class BasePlugin {
    public abstract String getID();

    public abstract void onCreate(OverlayService context);

    public abstract View onBind();

    public abstract void onUnbind();

    public abstract void onDestroy();

    public abstract void onExpand();

    public abstract void onCollapse();

    public abstract void onClick();
}