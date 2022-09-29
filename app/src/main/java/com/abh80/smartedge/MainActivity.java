package com.abh80.smartedge;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        init();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!Settings.canDrawOverlays(this) || !Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName())
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, PermissionActivity.class));
        }
        MaterialSwitch enable_btn = findViewById(R.id.enable_switch);
        enable_btn.setOnClickListener(l -> {
            sharedPreferences.edit().putBoolean("enabled", enable_btn.isChecked()).apply();
        });
        enable_btn.setChecked(sharedPreferences.getBoolean("enabled", false));
        MaterialSwitch enable_btn2 = findViewById(R.id.enable_switch2);
        enable_btn2.setOnClickListener(l -> {
            sharedPreferences.edit().putBoolean("hwd_enabled", enable_btn2.isChecked()).apply();
        });
        enable_btn2.setChecked(sharedPreferences.getBoolean("hwd_enabled", false));
    }


    private void init() {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
    }

}
