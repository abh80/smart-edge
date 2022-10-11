package com.abh80.smartedge.utils.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.utils.NotificationAppMeta;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationManageAppsAdapter extends RecyclerView.Adapter<NotificationManageAppsAdapter.ViewHolder> {
    public ArrayList<NotificationAppMeta> apps;
    private SharedPreferences sharedPreferences;

    public NotificationManageAppsAdapter(SharedPreferences sharedPreferences, ArrayList<NotificationAppMeta> apps) {
        this.apps = apps;
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_notification_layout_app, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageDrawable(apps.get(position).app_icon);
        holder.name.setText(apps.get(position).name);
        holder.switchBtn.setChecked(apps.get(position).enabled);
        holder.switchBtn.setOnClickListener(l -> {
            NotificationAppMeta app = apps.get(position);
            app.enabled = holder.switchBtn.isChecked();
            holder.switchBtn.setChecked(holder.switchBtn.isChecked());
            ArrayList<String> p = parseEnabledApps(sharedPreferences.getString("notifications_apps", ""));
            if (app.enabled) {
                p.add(app.package_name);
            } else {
                p.remove(app.package_name);
            }
            sharedPreferences.edit().putString("notifications_apps", StringUtils.join(p, ",")).apply();
        });
    }

    public static ArrayList<String> parseEnabledApps(String s) {
        return new ArrayList<>(Arrays.asList(s.split(",")));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public MaterialSwitch switchBtn;
        public ShapeableImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.app_title);
            switchBtn = itemView.findViewById(R.id.app_switch);
            imageView = itemView.findViewById(R.id.app_image);
        }
    }
}
