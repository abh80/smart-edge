package com.abh80.smartedge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

public class BootReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Settings.canDrawOverlays(context) || !Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners").contains(context.getApplicationContext().getPackageName())
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent1 = new Intent(context, OverlayService.class);
        context.startForegroundService(intent1);
    }
}
