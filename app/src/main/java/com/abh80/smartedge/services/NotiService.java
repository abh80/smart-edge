package com.abh80.smartedge.services;

import android.app.NotificationManager;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.abh80.smartedge.utils.NotificationHolderClass;

public class NotiService extends NotificationListenerService {
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Intent intent = new Intent(getPackageName() + ".NOTIFICATION_POSTED");
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }
}
