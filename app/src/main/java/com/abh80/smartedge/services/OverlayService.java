package com.abh80.smartedge.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;

import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.plugins.ExportedPlugins;
import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.google.android.material.color.DynamicColors;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class OverlayService extends AccessibilityService {
    private final ArrayList<BasePlugin> plugins = ExportedPlugins.getPlugins();
    public int minHeight;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                if (sharedPreferences.getBoolean("enable_on_lockscreen", false)) return;
                if (mView != null) {
                    mView.setVisibility(View.VISIBLE);
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (sharedPreferences.getBoolean("enable_on_lockscreen", false)) return;
                mView.setVisibility(View.INVISIBLE);
            } else if (intent.getAction().equals(getPackageName() + ".OVERLAY_LAYOUT_CHANGE")) {
                Bundle settings = intent.getExtras().getBundle("settings");
                for (String s : settings.keySet()) {
                    if (settings.get(s) instanceof Float) {
                        sharedPreferences.putFloat(s, settings.getFloat(s));
                    }
                }
                if (mView != null && mWindowManager != null) {
                    WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mView.getLayoutParams();

                    minWidth = dpToInt((int) sharedPreferences.getFloat("overlay_w", 83));
                    minHeight = dpToInt((int) sharedPreferences.getFloat("overlay_h", 40));
                    gap = dpToInt((int) sharedPreferences.getFloat("overlay_gap", 50));
                    y = (int) (sharedPreferences.getFloat("overlay_y", 0.67f) * 0.01 * metrics.heightPixels);
                    x = (int) (sharedPreferences.getFloat("overlay_x", 0) * 0.01 * metrics.widthPixels);
                    mParams.y = y;
                    mParams.x = x;
                    mParams.height = minHeight;
                    if (mView.findViewById(R.id.blank_space) != null) {
                        mView.findViewById(R.id.blank_space).setMinimumWidth(gap);
                    }
                    mWindowManager.updateViewLayout(mView, mParams);
                }
            } else if (intent.getAction().equals(getPackageName() + ".COLOR_CHANGED")) {
                color = intent.getExtras().getInt("color");
                textColor = isColorDark(color) ? getColor(R.color.white) : getColor(R.color.black);
                if (mView != null) {
                    mView.setBackgroundTintList(ColorStateList.valueOf(color));
                    if (binded_plugin != null) {
                        binded_plugin.onTextColorChange();
                    }
                }
            } else {
                Bundle settings = intent.getExtras().getBundle("settings");
                for (String s : settings.keySet()) {
                    if (settings.get(s) instanceof Boolean) {
                        sharedPreferences.putBoolean(s, settings.getBoolean(s));
                    }
                }
                plugins.forEach(BasePlugin::onDestroy);
                queued.clear();
                if (mView != null && mWindowManager != null) {
                    mWindowManager.removeViewImmediate(mView);
                }
                init();
            }
        }
    };
    public int x, y;
    private int color;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        plugins.forEach(x -> x.onEvent(accessibilityEvent));
    }

    @Override
    public void onInterrupt() {

    }


    private void expandOverlay() {
        if (binded_plugin != null) {
            if (sharedPreferences.getBoolean("invert_click", false)) {
                binded_plugin.onClick();
            } else
                binded_plugin.onExpand();
        } else {
            animateOverlay(minHeight + dpToInt(20), minWidth + dpToInt(20), false, new CallBack(), new CallBack() {
                @Override
                public void onFinish() {
                    animateOverlay(minHeight, minWidth, false, new CallBack(), new CallBack(), false);
                }
            }, false);
        }
    }

    private void shrinkOverlay() {
        if (binded_plugin != null) binded_plugin.onCollapse();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public Bundle sharedPreferences = new Bundle();

    private WindowManager.LayoutParams getParams(int width, int height, int extFlags) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                extFlags,
                PixelFormat.TRANSLUCENT);
    }

    private float y1, y2, x1, x2;
    static final int MIN_DISTANCE = 50;
    private final AtomicLong press_start = new AtomicLong();
    public DisplayMetrics metrics = new DisplayMetrics();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            if (sharedPreferences.getBoolean("clip_copy_enabled", true)) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("smart edge error log", throwable.getMessage() + " : " + Arrays.toString(throwable.getStackTrace()));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Smart Edge Crashed, logs copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            Runtime.getRuntime().exit(0);
        });
        IntentFilter filter = new IntentFilter(getPackageName() + ".SETTINGS_CHANGED");
        filter.addAction(getPackageName() + ".OVERLAY_LAYOUT_CHANGE");
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(getPackageName() + ".COLOR_CHANGED");
        registerReceiver(broadcastReceiver, filter);

        SharedPreferences sharedPreferences2 = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        sharedPreferences2.getAll().forEach((key, value) -> {
            if (value instanceof Boolean)
                sharedPreferences.putBoolean(key, (boolean) value);
            else if (value instanceof Float) {
                sharedPreferences.putFloat(key, (float) value);
            } else if (value instanceof String) {
                sharedPreferences.putString(key, (String) value);
            } else if (value instanceof Integer) {
                sharedPreferences.putInt(key, (int) value);
            }
        });
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        init();
    }

    public int gap;
    private Context ctx;

    public int getAttr(int attr) {
        final TypedValue value = new TypedValue();
        ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true);
        return value.data;
    }

    public int statusBarHeight = 0;

    private boolean isColorDark(int color) {
        // Source : https://stackoverflow.com/a/24261119
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        // It's a dark color
        return !(darkness < 0.5); // It's a light color
    }

    public int textColor;

    @SuppressLint("ClickableViewAccessibility")
    private void init() {

        binded_plugin = null;
        int flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;


        if (minWidth == 0) {
            minWidth = dpToInt((int) sharedPreferences.getFloat("overlay_w", 83));
        }
        if (minHeight == 0) {
            minHeight = dpToInt((int) sharedPreferences.getFloat("overlay_h", 40));
        }
        if (gap == 0) {
            gap = dpToInt((int) sharedPreferences.getFloat("overlay_gap", 50));
        }
        color = sharedPreferences.getInt("color", getColor(R.color.black));
        textColor = isColorDark(color) ? getColor(R.color.white) : getColor(R.color.black);
        last_min_size = minWidth;
        WindowManager.LayoutParams mParams = getParams(minWidth, minHeight, flags);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        getBaseContext().setTheme(R.style.Theme_SmartEdge);
        mView = layoutInflater.inflate(R.layout.overlay_layout, null);

        ctx = DynamicColors.wrapContextIfAvailable(getBaseContext(), com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight);
        mParams.gravity = Gravity.TOP | Gravity.CENTER;
        if (y == 0) {
            y = (int) (sharedPreferences.getFloat("overlay_y", 0.67f) * 0.01f * metrics.heightPixels);
        }
        mParams.y = y;
        if (x == 0) {
            x = (int) (sharedPreferences.getFloat("overlay_x", 0) * 0.01f * metrics.widthPixels);
        }
        mView.setBackgroundTintList(ColorStateList.valueOf(color));

        mParams.x = x;
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
                x1 = event.getX();
            }

            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                shrinkOverlay();
            }
            if ((event.getAction() == MotionEvent.ACTION_UP)) {
                mHandler.removeCallbacks(mLongPressed);
                y2 = event.getY();
                x2 = event.getX();
                float deltaY = y2 - y1;
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (binded_plugin != null) {
                        if (deltaX < 0) {
                            binded_plugin.onLeftSwipe();
                        } else binded_plugin.onRightSwipe();
                    }
                }
                if (-deltaY > MIN_DISTANCE) {
                    shrinkOverlay();
                    return false;
                }
                if (Math.abs(deltaX) < MIN_DISTANCE && -deltaY < MIN_DISTANCE) {
                    if (press_start.get() + ViewConfiguration.getLongPressTimeout() > Instant.now().toEpochMilli())
                        if (binded_plugin != null) {
                            if (sharedPreferences.getBoolean("invert_click", false))
                                binded_plugin.onExpand();
                            else binded_plugin.onClick();
                        }
                }

            }
            return false;
        });
        plugins.forEach(x -> {
            if (sharedPreferences.getBoolean(x.getID() + "_enabled", true)) x.onCreate(this);
        });
        binded_plugin = null;
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

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd, boolean expandedPrev) {
        int init_w = w;
        if (!expanded && w == ViewGroup.LayoutParams.WRAP_CONTENT) {
            w = last_min_size;
            if (w < minWidth) w = minWidth;
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
        if (expanded) {
            if (!expandedPrev) last_min_size = mView.getMeasuredWidth();
        }
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(800);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(800);
        width_anim.addUpdateListener(v2 -> {
            params.width = Math.abs((int) v2.getAnimatedValue());
            mWindowManager.updateViewLayout(mView, params);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                callBackStart.onFinish();
                if (expanded && params.x != 0) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, 0).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
                if (!expanded && x != params.x) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, x).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }

            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callBackEnd.onFinish();
                if (init_w == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                mWindowManager.updateViewLayout(mView, params);
            }
        });
        if (w != 0) {
            width_anim.setInterpolator(new OvershootInterpolator(1f));
            height_anim.setInterpolator(new OvershootInterpolator(1f));
        }

        width_anim.start();
        height_anim.start();
    }

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd, CallBack onChange, boolean expandedPrev) {
        int init_w = w;
        if (!expanded && w == ViewGroup.LayoutParams.WRAP_CONTENT) {
            w = last_min_size;
            if (w < minWidth) w = minWidth;
        }

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
        if (expanded) {
            if (!expandedPrev) last_min_size = mView.getMeasuredWidth();
        }
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(800);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });

        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(800);
        width_anim.addUpdateListener(v2 -> {
            onChange.onChange(v2.getAnimatedFraction());
            params.width = Math.abs((int) v2.getAnimatedValue());
            mWindowManager.updateViewLayout(mView, params);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                callBackStart.onFinish();
                if (expanded && params.x != 0) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, 0).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
                if (!expanded && x != params.x) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, x).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callBackEnd.onFinish();

                if (init_w == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                mWindowManager.updateViewLayout(mView, params);
            }
        });
        if (w != 0) {
            width_anim.setInterpolator(new OvershootInterpolator(1f));
            height_anim.setInterpolator(new OvershootInterpolator(1f));
        }

        width_anim.start();
        height_anim.start();
    }

    private int minWidth;

    private void closeOverlay() {
        animateOverlay(minHeight, minWidth, false, new CallBack(), new CallBack() {
            @Override
            public void onFinish() {
                super.onFinish();
                View replace = mView.findViewById(R.id.binded);
                if (replace == null) return;
                ((ViewGroup) mView).removeView(replace);
                mView.getLayoutParams().width = minWidth;
                mView.setLayoutParams(mView.getLayoutParams());
            }
        }, false);

    }

    private void bindPlugin() {
        if (queued.size() <= 0) {
            if (binded_plugin != null) binded_plugin.onUnbind();
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
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        if (replace == null) {
            ((ViewGroup) mView).addView(view);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) mView);
            constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
            constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo((ConstraintLayout) mView);
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            View vGap = mView.findViewById(R.id.blank_space);
            if (vGap != null) {
                ViewGroup.LayoutParams params1 = vGap.getLayoutParams();
                vGap.setMinimumWidth(gap);
                vGap.setLayoutParams(params1);
            }
            mWindowManager.updateViewLayout(mView, params);
            if (binded_plugin != null) binded_plugin.onBindComplete();
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
        View vGap = mView.findViewById(R.id.blank_space);
        if (vGap != null) {
            ViewGroup.LayoutParams params1 = vGap.getLayoutParams();
            vGap.setMinimumWidth(gap);
            vGap.setLayoutParams(params1);
        }
        mWindowManager.updateViewLayout(mView, params);
        if (binded_plugin != null) binded_plugin.onBindComplete();
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
