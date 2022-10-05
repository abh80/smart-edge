package com.abh80.smartedge.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.plugins.ExportedPlugins;
import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.MediaSession.MediaSessionPlugin;
import com.abh80.smartedge.plugins.Notification.NotificationPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class OverlayService extends AccessibilityService {

    private boolean is_hwd_enabled = false;
    private final ArrayList<BasePlugin> plugins = ExportedPlugins.getPlugins();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            sharedPreferences = intent.getExtras().getBundle("settings");
            plugins.forEach(BasePlugin::onDestroy);
            queued.clear();
            if (mView != null && mWindowManager != null) {
                mWindowManager.removeViewImmediate(mView);
            }
            init();
        }
    };


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        plugins.forEach(x -> x.onEvent(accessibilityEvent));
    }

    @Override
    public void onInterrupt() {

    }


    private void expandOverlay() {
        if (binded_plugin != null) binded_plugin.onExpand();
    }

    private void shrinkOverlay() {
        if (binded_plugin != null) binded_plugin.onCollapse();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    Bundle sharedPreferences = new Bundle();

    private WindowManager.LayoutParams getParams(int width, int height, int extFlags) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                extFlags,
                PixelFormat.TRANSLUCENT);
    }

    private float y1, y2;
    static final int MIN_DISTANCE = 50;
    private final AtomicLong press_start = new AtomicLong();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            if (sharedPreferences.getBoolean("clip_copy_enabled", true)) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("smart edge error log", throwable.getMessage());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Smart Edge Crashed, logs copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            Runtime.getRuntime().exit(0);
        });
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.notificationTimeout = 100;
        info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
        setServiceInfo(info);
        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".SETTINGS_CHANGED"));

        SharedPreferences sharedPreferences2 = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        sharedPreferences2.getAll().forEach((key, value) -> {
            sharedPreferences.putBoolean(key, (boolean) value);
        });
        is_hwd_enabled = sharedPreferences.getBoolean("hwd_enabled", false);
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        binded_plugin = null;
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (is_hwd_enabled) {
            flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        WindowManager.LayoutParams mParams = getParams(minWidth, 100, flags);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.overlay_layout, null);
        mParams.gravity = Gravity.TOP | Gravity.CENTER;
        mParams.verticalMargin = 0.005f;

        Runnable mLongPressed = this::expandOverlay;
        try {

            if (mView.getWindowToken() == null) {
                if (mView.getParent() == null) {
                    mWindowManager.addView(mView, mParams);
                }
            }
        } catch (Exception e) {
            Log.d("Error1", e.toString());
        }
        mView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mHandler.postDelayed(mLongPressed, ViewConfiguration.getLongPressTimeout());
                press_start.set(Instant.now().toEpochMilli());
                y1 = event.getY();
            }

            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                shrinkOverlay();
            }
            if ((event.getAction() == MotionEvent.ACTION_UP)) {
                mHandler.removeCallbacks(mLongPressed);
                y2 = event.getY();
                float deltaY = y2 - y1;
                if (-deltaY > MIN_DISTANCE) {
                    shrinkOverlay();
                    return false;
                } else {
                    if (press_start.get() + ViewConfiguration.getLongPressTimeout() > Instant.now().toEpochMilli())
                        if (binded_plugin != null) binded_plugin.onClick();
                }

            }
            return false;
        });
        plugins.forEach(x -> {
            if (sharedPreferences.getBoolean(x.getID() + "_enabled", true)) x.onCreate(this);
        });
        bindPlugin();
    }

    ArrayList<String> queued = new ArrayList<>();
    private BasePlugin binded_plugin;

    public void enqueue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) {
            if (binded_plugin != null && plugins.indexOf(plugin) < plugins.indexOf(binded_plugin)) {
                queued.add(0, plugin.getID());
            } else queued.add(plugin.getID());
        }
        bindPlugin();
    }

    public void dequeue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) return;
        else queued.remove(plugin.getID());
        if (binded_plugin != null && binded_plugin.getID().equals(plugin.getID()))
            binded_plugin = null;
        bindPlugin();
    }

    private int last_min_size = 200;

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd) {
        int init_w = w;
        if (!expanded && w == ViewGroup.LayoutParams.WRAP_CONTENT) {
            w = last_min_size;
            if (w < minWidth) w = minWidth;
        }
        if (expanded) {
            last_min_size = mView.getMeasuredWidth();
        }
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(500);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(500);
        width_anim.addUpdateListener(v2 -> {
            params.width = (int) v2.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                callBackStart.onFinish();
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callBackEnd.onFinish();
                if (init_w == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    mWindowManager.updateViewLayout(mView, params);
                }
            }
        });
        width_anim.setInterpolator(new OvershootInterpolator(1f));
        height_anim.setInterpolator(new OvershootInterpolator(1f));

        width_anim.start();
        height_anim.start();
    }

    private int minWidth = 200;

    private void closeOverlay() {
        animateOverlay(100, minWidth, false, new CallBack(), new CallBack() {
            @Override
            public void onFinish() {
                super.onFinish();
                View replace = mView.findViewById(R.id.binded);
                if (replace == null) return;
                ((ViewGroup) mView).removeView(replace);
            }
        });

    }

    private void bindPlugin() {
        if (queued.size() <= 0) {
            closeOverlay();
            return;
        }
        Log.d("binding", queued.get(0));

        if (binded_plugin != null && Objects.equals(queued.get(0), binded_plugin.getID())) {
            return;
        }
        if (binded_plugin != null) binded_plugin.onUnbind();
        Optional<BasePlugin> optionalBasePlugin = plugins.stream().filter(x -> x.getID().equals(queued.get(0))).findFirst();
        if (!optionalBasePlugin.isPresent()) return;
        binded_plugin = optionalBasePlugin.get();
        View view = binded_plugin.onBind();
        View replace = mView.findViewById(R.id.binded);
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        if (replace == null) {
            ((ViewGroup) mView).addView(view);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) mView);
            constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
            constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo((ConstraintLayout) mView);
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mWindowManager.updateViewLayout(mView, params);
            binded_plugin.onBindComplete();
            return;
        }
        ViewGroup parent = (ViewGroup) replace.getParent();
        parent.removeView(replace);
        parent.addView(view);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) mView);
        constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo((ConstraintLayout) mView);
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mWindowManager.updateViewLayout(mView, params);
        binded_plugin.onBindComplete();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mView.setVisibility(View.INVISIBLE);
        } else mView.setVisibility(View.VISIBLE);
    }

    public int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        mWindowManager.removeView(mView);
        plugins.forEach(BasePlugin::onDestroy);
        Runtime.getRuntime().exit(0);
    }

    public final Handler mHandler = new Handler();


    private View mView;
    public WindowManager mWindowManager;


}
