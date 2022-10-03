package com.abh80.smartedge.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.abh80.smartedge.BuildConfig;
import com.abh80.smartedge.R;
import com.abh80.smartedge.activities.PermissionActivity;
import com.abh80.smartedge.services.UpdaterService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        init();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName())
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, PermissionActivity.class));
        }
        MaterialCardView enable_btn = findViewById(R.id.enable_switch);
        enable_btn.setOnClickListener(l -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Installed Apps -> Smart Edge", Toast.LENGTH_SHORT).show();
        });
        MaterialSwitch enable_btn2 = findViewById(R.id.enable_switch2);
        enable_btn2.setOnClickListener(l -> {
            sharedPreferences.edit().putBoolean("hwd_enabled", enable_btn2.isChecked()).apply();
        });
        enable_btn2.setChecked(sharedPreferences.getBoolean("hwd_enabled", false));

        startService(new Intent(this, UpdaterService.class));

        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".UPDATE_AVAIL"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

    }

    private void init() {
        if (sharedPreferences == null) {

            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Intent intent = new Intent(getPackageName() + ".SETTINGS_CHANGED");
        sharedPreferences.getAll().forEach((key, value) -> {
            intent.putExtra(key, (boolean) value);
        });
        sendBroadcast(intent);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + ".UPDATE_AVAIL")) {
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle("New Update Available")
                        .setMessage("We would like to update this app from " + BuildConfig.VERSION_NAME + " --> " + intent.getExtras().getString("version") +
                                ".\n\nUpdating app generally means better and more stable experience.")
                        .setCancelable(false)
                        .setNegativeButton("Later", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setPositiveButton("Update Now", ((dialogInterface, i) -> {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                MainActivity.this.sendBroadcast(new Intent(getPackageName() + ".START_UPDATE"));
                                Toast.makeText(MainActivity.this, "Updating in background! Please don't kill the app", Toast.LENGTH_SHORT).show();
                            } else
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
                            dialogInterface.dismiss();
                            if (!getPackageManager().canRequestPackageInstalls()) {
                                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 103);
                                Toast.makeText(MainActivity.this, "Please provide install access to update the application.", Toast.LENGTH_SHORT).show();
                            }
                        }))
                        .show();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            MainActivity.this.sendBroadcast(new Intent(getPackageName() + ".START_UPDATE"));
            Toast.makeText(MainActivity.this, "Updating in background! Please don't kill the app", Toast.LENGTH_SHORT).show();
        }
    }
}
