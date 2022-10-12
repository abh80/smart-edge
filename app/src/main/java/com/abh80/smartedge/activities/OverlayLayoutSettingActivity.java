package com.abh80.smartedge.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.abh80.smartedge.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class OverlayLayoutSettingActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;

    private int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    Slider h;
    TextView val_h;
    ShapeableImageView add_h;
    ShapeableImageView sub_h;

    Slider gap;
    TextView val_gap;
    ShapeableImageView add_gap;
    ShapeableImageView sub_gap;

    Slider x;
    TextView val_x;
    ShapeableImageView add_x;
    ShapeableImageView sub_x;

    Slider w;
    ShapeableImageView add_w;
    ShapeableImageView sub_w;

    Slider y;
    TextView val_y;
    ShapeableImageView add_y;
    ShapeableImageView sub_y;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overlay_layout_setting_activity);
        setSupportActionBar(findViewById(R.id.toolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        h = findViewById(R.id.seekbar_h);
        val_h = findViewById(R.id.val_h);
        add_h = findViewById(R.id.add_h);
        sub_h = findViewById(R.id.sub_h);

        gap = findViewById(R.id.seekbar_gap);
        val_gap = findViewById(R.id.val_gap);
        add_gap = findViewById(R.id.add_gap);
        sub_gap = findViewById(R.id.sub_gap);

        x = findViewById(R.id.seekbar_x);
        val_x = findViewById(R.id.val_x);
        add_x = findViewById(R.id.add_x);
        sub_x = findViewById(R.id.sub_x);

        w = findViewById(R.id.seekbar_w);
        add_w = findViewById(R.id.add_w);
        sub_w = findViewById(R.id.sub_w);
        val_w = findViewById(R.id.val_w);

        y = findViewById(R.id.seekbar_y);
        val_y = findViewById(R.id.val_y);

        add_w.setOnClickListener(l -> {
            int v = (int) w.getValue();
            v += 1;
            if (v >= w.getValueTo()) v = (int) w.getValueTo();
            if (v <= w.getValueFrom()) v = (int) w.getValueFrom();
            w.setValue(v);
            onChange();
        });
        sub_w.setOnClickListener(l -> {
            int v = (int) w.getValue();
            v -= 1;
            if (v >= w.getValueTo()) v = (int) w.getValueTo();
            if (v <= w.getValueFrom()) v = (int) w.getValueFrom();
            w.setValue(v);
            onChange();
        });
        add_h.setOnClickListener(l -> {
            int v = (int) h.getValue();
            v += 1;
            if (v >= h.getValueTo()) v = (int) h.getValueTo();
            if (v <= h.getValueFrom()) v = (int) h.getValueFrom();
            h.setValue(v);
            onChange();
        });
        sub_h.setOnClickListener(l -> {
            int v = (int) h.getValue();
            v -= 1;
            if (v >= h.getValueTo()) v = (int) h.getValueTo();
            if (v <= h.getValueFrom()) v = (int) h.getValueFrom();
            h.setValue(v);
            onChange();
        });
        add_gap.setOnClickListener(l -> {
            int v = (int) gap.getValue();
            v += 1;
            if (v >= gap.getValueTo()) v = (int) gap.getValueTo();
            if (v <= gap.getValueFrom()) v = (int) gap.getValueFrom();
            gap.setValue(v);
            onChange();
        });
        sub_gap.setOnClickListener(l -> {
            int v = (int) gap.getValue();
            v -= 1;
            if (v >= gap.getValueTo()) v = (int) gap.getValueTo();
            if (v <= gap.getValueFrom()) v = (int) gap.getValueFrom();
            gap.setValue(v);
            onChange();
        });
        add_y = findViewById(R.id.add_y);
        sub_y = findViewById(R.id.sub_y);

        add_y.setOnClickListener(l -> {
            float v = y.getValue();
            v += 0.1;
            if (v >= y.getValueTo()) v = y.getValueTo();
            if (v <= y.getValueFrom()) v = y.getValueFrom();
            y.setValue(v);
            onChange();
        });
        sub_y.setOnClickListener(l -> {
            float v = y.getValue();
            v -= 0.1;
            if (v >= y.getValueTo()) v = y.getValueTo();
            if (v <= y.getValueFrom()) v = y.getValueFrom();
            y.setValue(v);
            onChange();
        });
        add_x.setOnClickListener(l -> {
            float v = x.getValue();
            v += 0.1;
            if (Math.abs(v) < 0.1) v = 0;
            if (v >= x.getValueTo()) v = x.getValueTo();
            if (v <= x.getValueFrom()) v = x.getValueFrom();
            x.setValue(v);
            onChange();
        });
        sub_x.setOnClickListener(l -> {
            float v = x.getValue();
            v -= 0.1;
            if (v >= x.getValueTo()) v = x.getValueTo();
            if (v <= x.getValueFrom()) v = x.getValueFrom();
            x.setValue(v);
            onChange();
        });

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
        updateTexts();
        findViewById(R.id.reset_btn).setOnClickListener(l -> {
            gap.setValue(40);
            w.setValue(100);
            h.setValue(40);
            x.setValue(0);
            y.setValue(0.1f);
            onChange();
        });

    }

    TextView val_w;

    @SuppressLint("SetTextI18n")
    private void updateTexts() {
        DecimalFormat df = new DecimalFormat("##.##");
        df.setRoundingMode(RoundingMode.DOWN);
        val_gap.setText(df.format(gap.getValue()) + " dp");
        val_x.setText(df.format(x.getValue()) + " %");
        val_y.setText(df.format(y.getValue()) + " %");
        val_h.setText(df.format(h.getValue()) + " dp");
        val_w.setText(df.format(w.getValue()) + " dp");
    }

    private void onChange() {
        sharedPreferences.edit().putFloat("overlay_w", w.getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_h", h.getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_gap", gap.getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_x", x.getValue()).apply();
        sharedPreferences.edit().putFloat("overlay_y", y.getValue()).apply();
        updateTexts();
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

    private float pxToDp(int x) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return x / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
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
