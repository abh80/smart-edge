package com.abh80.smartedge.plugins.BatteryPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.abh80.smartedge.views.BatteryImageView;

import java.util.ArrayList;

public class BatteryPlugin extends BasePlugin {
    @Override
    public String getID() {
        return "BatteryPlugin";
    }

    @Override
    public String getName() {
        return "Battery Plugin";
    }

    private OverlayService ctx;

    @Override
    public void onCreate(OverlayService context) {
        ctx = context;
        ctx.registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private View mView;

    @Override
    public View onBind() {
        mView = LayoutInflater.from(ctx).inflate(R.layout.battery_layout, null);
        init();
        return mView;
    }

    private void init() {
        tv = mView.findViewById(R.id.text_percent);
        batteryImageView = mView.findViewById(R.id.cover);
        updateView();
    }

    float batteryPercent;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getExtras().getInt(BatteryManager.EXTRA_STATUS);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
            if (isCharging) {
                ctx.enqueue(BatteryPlugin.this);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPercent = level * 100 / (float) scale;
                updateView();
            } else {
                ctx.dequeue(BatteryPlugin.this);
                if (tv != null && batteryImageView != null) {
                    ValueAnimator valueAnimator = ValueAnimator.ofInt(0, ctx.dpToInt(0));
                    valueAnimator.setDuration(300);
                    valueAnimator.addUpdateListener(valueAnimator1 -> {
                        ViewGroup.LayoutParams p = batteryImageView.getLayoutParams();
                        p.width = (int) valueAnimator1.getAnimatedValue();
                        p.height = (int) valueAnimator1.getAnimatedValue();
                        batteryImageView.setLayoutParams(p);
                    });
                    valueAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationEnd(animation);
                            tv.setVisibility(View.INVISIBLE);
                        }
                    });
                    valueAnimator.start();
                }
            }
        }
    };
    private TextView tv;
    private BatteryImageView batteryImageView;

    private void updateView() {
        if (mView != null) {
            tv.setText((int) batteryPercent + "%");
            batteryImageView.updateBatteryPercent(batteryPercent);
            if (batteryPercent > 80) {
                batteryImageView.setStrokeColor(Color.GREEN);
                tv.setTextColor(Color.GREEN);
            } else if (batteryPercent < 80 && batteryPercent > 20) {
                batteryImageView.setStrokeColor(Color.YELLOW);
                tv.setTextColor(Color.YELLOW);
            } else {
                batteryImageView.setStrokeColor(Color.RED);
                tv.setTextColor(Color.RED);
            }
        }
    }

    @Override
    public void onUnbind() {

        mView = null;
    }

    @Override
    public void onBindComplete() {
        if (mView == null) return;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, ctx.dpToInt(ctx.minHeight / 4));
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(valueAnimator1 -> {
            ViewGroup.LayoutParams p = batteryImageView.getLayoutParams();
            p.width = (int) valueAnimator1.getAnimatedValue();
            p.height = (int) valueAnimator1.getAnimatedValue();
            batteryImageView.setLayoutParams(p);
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tv.setVisibility(View.VISIBLE);
                batteryImageView.requestLayout();
                batteryImageView.updateBatteryPercent(batteryPercent);
            }
        });
        valueAnimator.start();
    }

    @Override
    public void onDestroy() {
        ctx.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onExpand() {

    }

    @Override
    public void onCollapse() {

    }

    @Override
    public void onClick() {

    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    @Override
    public ArrayList<SettingStruct> getSettings() {
        return null;
    }
}
