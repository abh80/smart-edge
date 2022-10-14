package com.abh80.smartedge.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.abh80.smartedge.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppearanceActivity extends AppCompatActivity {
    private String intToHex(int i) {
        return Integer.toHexString(i);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appearence_layout);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        TextInputLayout t = findViewById(R.id.textField);
        Objects.requireNonNull(t.getEditText()).setText(intToHex(sharedPreferences.getInt("color", getColor(R.color.black))));
        findViewById(R.id.apply_btn).setOnClickListener(l -> {
            String value = null;
            if (Objects.requireNonNull(t.getEditText()).getText() != null)
                value = "#" + t.getEditText().getText().toString();
            if (value != null) {
                if (isValidColor(value)) {
                    t.setError(null);
                    t.setErrorEnabled(false);
                    try {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(findViewById(R.id.textField).getWindowToken(), 0);
                    } catch (Exception ignored) {
                    }
                    try {
                        int color = Color.parseColor(value);
                        sharedPreferences.edit().putInt("color", color).apply();
                        Snackbar.make(this, findViewById(R.id.textField), "Successfully updated color", Snackbar.LENGTH_SHORT).show();
                        Intent intent = new Intent(getPackageName() + ".COLOR_CHANGED");
                        intent.putExtra("color", color);
                        sendBroadcast(intent);
                    } catch (Exception e) {
                        t.setErrorEnabled(true);
                        t.setError("Invalid hexadecimal value");
                    }
                } else {
                    t.setErrorEnabled(true);
                    t.setError("Please provide a valid hexadecimal value");
                }

            } else {
                t.setErrorEnabled(true);
                t.setError("Please provide a hexadecimal value");
            }
        });
    }

    private boolean isValidColor(String value) {
        // Source : https://stackoverflow.com/a/23155867/14200419
        Pattern colorPattern = Pattern.compile("#([0-9a-f]{6}|[0-9a-f]{8})");
        Matcher m = colorPattern.matcher(value);
        return m.matches();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }
}
