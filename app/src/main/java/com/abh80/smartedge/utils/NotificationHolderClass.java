package com.abh80.smartedge.utils;

import android.service.notification.StatusBarNotification;

import java.io.Serializable;

public class NotificationHolderClass implements Serializable {
    public StatusBarNotification notification;

    public NotificationHolderClass(StatusBarNotification notification) {
        this.notification = notification;
    }
}
