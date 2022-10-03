package com.abh80.smartedge.services;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


public class NotiService extends NotificationListenerService {
    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(receiver, new IntentFilter(getPackageName() + ".ACTION_OPEN_CLOSE"));
    }

    private ArrayList<StatusBarNotification> notifications = new ArrayList<>();

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Intent intent = new Intent(getPackageName() + ".NOTIFICATION_POSTED");
        Notification notification = sbn.getNotification();
        intent.putExtra("package_name", sbn.getPackageName());
        intent.putExtra("id", sbn.getId());
        try {
            intent.putExtra("title", notification.extras.getString("android.title"));
            intent.putExtra("body", notification.extras.getString("android.text"));
        } catch (Exception e) {
            //ignore
        }
        notifications.add(sbn);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Intent intent = new Intent(getPackageName() + ".NOTIFICATION_REMOVED");
        intent.putExtra("id", sbn.getId());
        notifications.remove(sbn);
        sendBroadcast(intent);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(context.getPackageName() + ".ACTION_OPEN_CLOSE")) {
                Optional<StatusBarNotification> n = notifications.stream().filter(x -> x.getId() == intent.getExtras().getInt("id")).findFirst();
                n.ifPresent(x -> {
                    try {
                        if (x.getNotification().deleteIntent != null) {
                            x.getNotification().deleteIntent.send();
                        }
                        if (x.getNotification().contentIntent != null) {
                            x.getNotification().contentIntent.send();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    };
}
