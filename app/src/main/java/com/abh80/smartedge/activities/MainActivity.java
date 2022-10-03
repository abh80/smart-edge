package com.abh80.smartedge.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.abh80.smartedge.R;
import com.abh80.smartedge.activities.PermissionActivity;
import com.google.android.material.card.MaterialCardView;
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
            Log.d("kok", String.valueOf(enable_btn2.isChecked()));
        });
        enable_btn2.setChecked(sharedPreferences.getBoolean("hwd_enabled", false));
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
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Intent intent = new Intent(getPackageName() + ".SETTINGS_CHANGED");
        sharedPreferences.getAll().forEach((key, value) -> {
            intent.putExtra(key, (boolean) value);
        });
        sendBroadcast(intent);
    }
}
