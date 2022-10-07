package com.abh80.smartedge.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.abh80.smartedge.R;
import com.google.android.material.slider.Slider;

public class OverlayLayoutSettingActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overlay_layout_setting_activity);
        setSupportActionBar(findViewById(R.id.toolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        Slider w = findViewById(R.id.seekbar_w);
        Slider h = findViewById(R.id.seekbar_h);
        Slider gap = findViewById(R.id.seekbar_gap);
        Slider x = findViewById(R.id.seekbar_x);
        Slider y = findViewById(R.id.seekbar_y);

        y.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) onChange();
        });
        x.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) onChange();
        });
        h.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) onChange();
        });
        w.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) onChange();
        });
        gap.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) onChange();
        });
        gap.setValue(sharedPreferences.getFloat("overlay_gap", 50));
        w.setValue(sharedPreferences.getFloat("overlay_w", 83));
        h.setValue(sharedPreferences.getFloat("overlay_h", 40));
        x.setValue(sharedPreferences.getFloat("overlay_x", 0));
        y.setValue(sharedPreferences.getFloat("overlay_y", 0.67f));

        findViewById(R.id.reset_btn).setOnClickListener(l -> {
            gap.setValue(40);
            w.setValue(100);
            h.setValue(40);
            x.setValue(0);
            y.setValue(0.1f);
            onChange();
        });

    }

    private void onChange() {
        sharedPreferences.edit().putFloat("overlay_w", ((Slider) findViewById(R.id.seekbar_w)).getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_h", ((Slider) findViewById(R.id.seekbar_h)).getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_gap", ((Slider) findViewById(R.id.seekbar_gap)).getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_x", ((Slider) findViewById(R.id.seekbar_x)).getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_y", ((Slider) findViewById(R.id.seekbar_y)).getValue()).apply();
        Intent intent = new Intent(getPackageName() + ".OVERLAY_LAYOUT_CHANGE");
        Bundle b = new Bundle();
        sharedPreferences.getAll().forEach((key, val) -> {
            if (val instanceof Float) {
                b.putFloat(key, (float) val);
            }
        });
        intent.putExtra("settings", b);
        sendBroadcast(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
