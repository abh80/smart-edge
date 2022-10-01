package com.abh80.smartedge.services;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.session.PlaybackState;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.MediaSession.MediaSessionPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class OverlayService extends AccessibilityService {

    private boolean is_hwd_enabled = false;
    private final ArrayList<BasePlugin> plugins = new ArrayList<>();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mView != null && mWindowManager != null)
                mWindowManager.removeView(mView);
            is_hwd_enabled = intent.getBooleanExtra("hwd_enabled", false);
            init();

        }
    };


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

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

    SharedPreferences sharedPreferences;

    private WindowManager.LayoutParams getParams(int width, int height, int extFlags) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                extFlags,
                PixelFormat.TRANSLUCENT);
    }

    private float y1, y2;
    static final int MIN_DISTANCE = 100;
    private final AtomicLong press_start = new AtomicLong();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Runtime.getRuntime().exit(0);
        });
        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".SETTINGS_CHANGED"));

        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        is_hwd_enabled = sharedPreferences.getBoolean("hwd_enabled", false);
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        plugins.add(new MediaSessionPlugin());
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (is_hwd_enabled) {
            flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        WindowManager.LayoutParams mParams = getParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100, flags);
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
                    if (binded_plugin != null) binded_plugin.onClick();
                }

            }
            return false;
        });
        plugins.forEach(x -> x.onCreate(this));
    }

    ArrayList<String> queued = new ArrayList<>();
    private BasePlugin binded_plugin;

    public void enqueue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) {
            queued.add(plugin.getID());
        }
        bindPlugin();
    }

    public void dequeue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) return;
        else queued.remove(plugin.getID());
        bindPlugin();
    }

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd) {
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(200);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(200);
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
            }
        });
        if (expanded) {
            width_anim.setInterpolator(new DecelerateInterpolator());
            height_anim.setInterpolator(new DecelerateInterpolator());
        } else {
            width_anim.setInterpolator(new AccelerateInterpolator());
            height_anim.setInterpolator(new AccelerateInterpolator());
        }
        width_anim.start();
        height_anim.start();
    }

    private void closeOverlay() {
        View replace = mView.findViewById(R.id.binded);
        if (replace == null) return;
        ViewGroup parent = (ViewGroup) replace.getParent();
        parent.removeView(replace);
    }

    private void bindPlugin() {
        if (queued.size() <= 0) {
            closeOverlay();
            return;
        }
        if (binded_plugin != null && Objects.equals(queued.get(0), binded_plugin.getID())) {
            return;
        }
        if (binded_plugin != null) binded_plugin.onUnbind();
        Optional<BasePlugin> optionalBasePlugin = plugins.stream().filter(x -> x.getID().equals(queued.get(0))).findFirst();
        if (!optionalBasePlugin.isPresent()) return;
        binded_plugin = optionalBasePlugin.get();
        View view = binded_plugin.onBind();
        View replace = mView.findViewById(R.id.binded);
        if (replace == null) {
            ((ViewGroup) mView).addView(view);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) mView);
            constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
            constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo((ConstraintLayout) mView);
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
