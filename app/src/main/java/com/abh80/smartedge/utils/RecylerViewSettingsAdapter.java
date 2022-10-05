package com.abh80.smartedge.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abh80.smartedge.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;

public class RecylerViewSettingsAdapter extends RecyclerView.Adapter<RecylerViewSettingsAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<SettingStruct> settings;

    public RecylerViewSettingsAdapter(Context context, ArrayList<SettingStruct> settings) {
        this.context = context;
        this.settings = settings;
        settings.add(0, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == SettingStruct.TYPE_TOGGLE) {
            itemView = LayoutInflater.from(context).inflate(R.layout.toggle_setting_layout, parent, false);
        } else if (viewType == SettingStruct.TYPE_CUSTOM) {
            itemView = LayoutInflater.from(context).inflate(R.layout.setting_custom_layout, parent, false);
        } else {
            itemView = LayoutInflater.from(context).inflate(R.layout.toggle_setting_null_layout, parent, false);
        }
        return new ViewHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(final int position) {
        if (settings.get(position) == null) return 0;
        return settings.get(position).type;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!holder.isItem) return;
        holder.textView.setText(settings.get(position).text);
        if (holder.ViewType == SettingStruct.TYPE_CUSTOM) {
            holder.itemView.setOnClickListener(l -> settings.get(position).onClick());
        }
        if (holder.ViewType == SettingStruct.TYPE_TOGGLE) {
            holder.switchBtn.setOnCheckedChangeListener((compoundButton, b) -> {
                settings.get(position).onCheckChanged(b);
            });
            holder.switchBtn.setChecked(settings.get(position).onAttach());
        }
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public MaterialSwitch switchBtn;
        public boolean isItem;
        public View itemView;
        public int ViewType;

        public ViewHolder(@NonNull View itemView, int itemViewType) {
            super(itemView);
            this.itemView = itemView;
            ViewType = itemViewType;
            isItem = itemViewType != 0;
            if (!isItem) {
                textView = itemView.findViewById(R.id.cat_text);
                return;
            }
            textView = itemView.findViewById(R.id.enable_text);
            if (itemViewType == SettingStruct.TYPE_TOGGLE) {
                switchBtn = itemView.findViewById(R.id.enable_switch2);
            }
        }
    }
}
