package com.abh80.smartedge.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abh80.smartedge.BuildConfig;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.ExportedPlugins;
import com.abh80.smartedge.services.UpdaterService;
import com.abh80.smartedge.utils.RecylerViewSettingsAdapter;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences sharedPreferences;
    private final ArrayList<SettingStruct> settings = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (sharedPreferences.getBoolean("clip_copy_enabled", true)) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("smart edge error log", throwable.getMessage());
                clipboard.setPrimaryClip(clip);
                sendCrashNotification();
            }
            Runtime.getRuntime().exit(0);
        });
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
        settings.add(new SettingStruct("Enable Hardware Acceleration", "App Settings") {
            @Override
            public boolean onAttach() {
                return sharedPreferences.getBoolean("hwd_enabled", false);
            }

            @Override
            public void onCheckChanged(boolean checked) {
                sharedPreferences.edit().putBoolean("hwd_enabled", checked).apply();
            }
        });
        settings.add(new SettingStruct("Manage Overlay Layout", "App Settings", SettingStruct.TYPE_CUSTOM) {
            @Override
            public void onClick() {
                startActivity(new Intent(MainActivity.this, OverlayLayoutSettingActivity.class));
            }
        });
        settings.add(new SettingStruct("Copy crash logs to clipboard", "App Settings") {
            @Override
            public boolean onAttach() {
                return sharedPreferences.getBoolean("clip_copy_enabled", true);
            }

            @Override
            public void onCheckChanged(boolean checked) {
                sharedPreferences.edit().putBoolean("clip_copy_enabled", checked).apply();
            }
        });
        settings.add(null);
        ExportedPlugins.getPlugins().forEach(x -> {
            settings.add(new SettingStruct("Enable " + x.getName() + " Plugin", x.getName() + " Plugin Settings") {
                             @Override
                             public boolean onAttach() {
                                 return sharedPreferences.getBoolean(x.getID() + "_enabled", true);
                             }

                             @Override
                             public void onCheckChanged(boolean checked) {
                                 sharedPreferences.edit().putBoolean(x.getID() + "_enabled", checked).apply();
                             }
                         }
            );
            if (x.getSettings() != null) {
                settings.addAll(x.getSettings());
            }
            settings.add(null);
        });
        RecylerViewSettingsAdapter adapter = new RecylerViewSettingsAdapter(this, settings);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ItemDecoration());
        recyclerView.invalidateItemDecorations();
        startService(new Intent(this, UpdaterService.class));
        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".UPDATE_AVAIL"));

    }

    private RecyclerView recyclerView;

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
        Bundle b = new Bundle();
        sharedPreferences.getAll().forEach((key, value) -> {
            b.putBoolean(key, (boolean) value);
        });
        intent.putExtra("settings", b);
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

    private int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    public class ItemDecoration extends RecyclerView.ItemDecoration {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int color = MaterialColors.getColor(MainActivity.this, com.google.android.material.R.attr.colorOnSecondary, getColor(R.color.md_theme_dark_secondary));
            super.onDraw(c, parent, state);
            int childCount = recyclerView.getChildCount();
            int width = recyclerView.getWidth();
            Rect cornerBounds = new Rect();
            c.getClipBounds(cornerBounds);
            for (int i = 0; i < childCount; i++) {
                View childAt = recyclerView.getChildAt(i);
                RecylerViewSettingsAdapter.ViewHolder viewHolder = (RecylerViewSettingsAdapter.ViewHolder) recyclerView.getChildViewHolder(childAt);
                int y = ((int) childAt.getY()) + childAt.getHeight();
                boolean shallDrawDivider;
                if (recyclerView.getChildAt(i + 1) != null)
                    shallDrawDivider = ((RecylerViewSettingsAdapter.ViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i + 1))).textView == null;
                else
                    shallDrawDivider = false;

                Drawable mDivider = getBaseContext().getDrawable(com.google.android.material.R.drawable.abc_list_divider_material);
                int mDividerHeight = mDivider.getIntrinsicHeight();

                if (viewHolder.isItem && shallDrawDivider) {
                    mDivider.setBounds(dpToInt(20), y, width - dpToInt(20), mDividerHeight + y);
                    mDivider.draw(c);
                }
                if (!viewHolder.isItem) {
                    if (settings.size() >= i + 2) {
                        c.drawText(settings.get(i + 1).category, cornerBounds.left + dpToInt(10), childAt.getBottom() - dpToInt(30), new Paint() {
                            {
                                setColor(MainActivity.this.getColor(R.color.quite_white));
                                setTextSize(dpToInt(16));
                            }
                        });
                    }
                    Drawable cornerBottom = getDrawable(R.drawable.rounded_corner_setting_bottom);
                    Drawable cornerTop = getDrawable(R.drawable.rounded_corner_setting_top);
                    if (recyclerView.getChildAt(i + 1) != null) {
                        View v = recyclerView.getChildAt(i + 1);
                        cornerTop.setBounds(cornerBounds.left, (int) v.getY() - dpToInt(20), cornerBounds.right, (int) v.getY());
                        cornerTop.draw(c);
                    }
                    if (recyclerView.getChildAt(i - 1) != null) {
                        View v = recyclerView.getChildAt(i - 1);
                        cornerBottom.setBounds(cornerBounds.left, v.getBottom(), cornerBounds.right, v.getBottom() + dpToInt(20));
                        cornerBottom.draw(c);
                    }

                } else {
                    Rect bounds = new Rect(cornerBounds.left, (int) childAt.getY(), cornerBounds.right, childAt.getBottom());
                    c.drawRect(bounds, new Paint() {
                        {
                            setColor(color);
                        }
                    });
                }

            }
            if (recyclerView.getChildCount() < 1) return;
            if (((RecylerViewSettingsAdapter.ViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(0))).isItem) {
                Drawable roundedCornerTop = getDrawable(R.drawable.rounded_corner_setting_top);
                roundedCornerTop.setBounds(cornerBounds.left, cornerBounds.top, cornerBounds.right, (int) recyclerView.getChildAt(0).getY());
                roundedCornerTop.draw(c);
            }
            if (((RecylerViewSettingsAdapter.ViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(recyclerView.getChildCount() - 1))).isItem) {
                Drawable roundedCornerBottom = getDrawable(R.drawable.rounded_corner_setting_bottom);
                roundedCornerBottom.setBounds(cornerBounds.left,
                        recyclerView.getChildAt(recyclerView.getChildCount() - 1).getBottom(),
                        cornerBounds.right,
                        recyclerView.getChildAt(recyclerView.getChildCount() - 1).getBottom() + dpToInt(20));
                roundedCornerBottom.draw(c);
            }
        }
    }

    private void sendCrashNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final String NOTIFICATION_CHANNEL_ID = getPackageName() + ".updater_channel";
        String channelName = "Updater Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(false)
                .setContentTitle("Smart Edge Crashed")
                .setContentText("Crash Log copied to clipboard")
                .setSmallIcon(R.drawable.launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_ERROR)
                .build();
        manager.notify(100, notification);
    }
}
