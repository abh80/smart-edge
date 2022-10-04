package com.abh80.smartedge.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.utils.ToggleSetting;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;

public class RecylerViewSettingsAdapter extends RecyclerView.Adapter<RecylerViewSettingsAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<ToggleSetting> settings;

    public RecylerViewSettingsAdapter(Context context, ArrayList<ToggleSetting> settings) {
        this.context = context;
        this.settings = settings;
        settings.add(0, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == 0) {
            itemView = LayoutInflater.from(context).inflate(R.layout.toggle_setting_layout, parent, false);
        } else {
            itemView = LayoutInflater.from(context).inflate(R.layout.toggle_setting_null_layout, parent, false);
        }
        return new ViewHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(final int position) {
        if (settings.get(position) == null) return 1;
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!holder.isItem) return;
        holder.textView.setText(settings.get(position).text);

        holder.switchBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            settings.get(position).onCheckChanged(b);
        });
        holder.switchBtn.setChecked(settings.get(position).onAttach());
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public MaterialSwitch switchBtn;
        public boolean isItem;

        public ViewHolder(@NonNull View itemView, int itemViewType) {
            super(itemView);
            isItem = itemViewType == 0;
            if (!isItem) {
                textView = itemView.findViewById(R.id.cat_text);
                return;
            }
            switchBtn = itemView.findViewById(R.id.enable_switch2);
            textView = itemView.findViewById(R.id.enable_text);
        }
    }
}
