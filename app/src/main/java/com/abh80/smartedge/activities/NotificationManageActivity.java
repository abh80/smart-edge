package com.abh80.smartedge.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.utils.NotificationAppMeta;
import com.abh80.smartedge.utils.adapters.NotificationManageAppsAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationManageActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private NotificationManageAppsAdapter adapter;
    private SharedPreferences sharedPreferences;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Intent intent = new Intent(getPackageName() + ".NOTIFICATION_APPS_UPDATE");
        intent.putExtra("apps", sharedPreferences.getString("notifications_apps", ""));
        sendBroadcast(intent);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_notification_layout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<NotificationAppMeta> apps = new ArrayList<>();
        ArrayList<String> apps_enabled = NotificationManageAppsAdapter.parseEnabledApps(sharedPreferences.getString("notifications_apps", ""));
        // avoid blocking ui
        new Thread(() -> {
            List<ApplicationInfo> infos = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : infos) {
                if (((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) || ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)) {
                    NotificationAppMeta meta = new NotificationAppMeta(getPackageManager().getApplicationLabel(info).toString(), info.packageName, info.loadIcon(getPackageManager()));
                    meta.enabled = apps_enabled.contains("all") || apps_enabled.contains(info.packageName);
                    apps.add(meta);
                }
            }
            runOnUiThread(() -> {
                adapter = new NotificationManageAppsAdapter(sharedPreferences, apps);
                recyclerView.setAdapter(adapter);
            });
        }).start();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter = null;
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.notifications_top_bar_menu, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            if (adapter.apps != null) {
                if (adapter.apps.stream().anyMatch(x -> x.enabled)) {
                    for (NotificationAppMeta app : adapter.apps) {
                        app.enabled = false;
                    }
                    sharedPreferences.edit().putString("notifications_apps", "").apply();
                } else {
                    for (NotificationAppMeta app : adapter.apps) {
                        app.enabled = true;
                    }
                    sharedPreferences.edit().putString("notifications_apps", StringUtils.join(adapter.apps.stream().map(x -> x.package_name).toArray(), ",")).apply();
                }
                adapter.notifyDataSetChanged();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
