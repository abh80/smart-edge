package com.abh80.smartedge.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class NotiService extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(getPackageName() + ".ACTION_OPEN_CLOSE");
        filter.addAction(getPackageName() + ".ACTION_CLOSE");
        registerReceiver(receiver, filter);
    }

    private final ArrayList<StatusBarNotification> notifications = new ArrayList<>();

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
        intent.putExtra("time", sbn.getPostTime());
        intent.putExtra("icon_large", sbn.getNotification().getLargeIcon());
        intent.putExtra("icon_small", sbn.getNotification().getSmallIcon());
        intent.putExtra("category", sbn.getNotification().category);
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
                        } else {
                            cancelNotification(x.getKey());
                        }
                        if (x.getNotification().contentIntent != null) {
                            x.getNotification().contentIntent.send();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
            if (intent.getAction().equals(context.getPackageName() + ".ACTION_CLOSE")) {
                Optional<StatusBarNotification> n = notifications.stream().filter(x -> x.getId() == intent.getExtras().getInt("id")).findFirst();
                n.ifPresent(x -> {
                    try {
                        if (x.getNotification().deleteIntent != null) {
                            x.getNotification().deleteIntent.send();
                        } else {
                            cancelNotification(x.getKey());
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    };
}
