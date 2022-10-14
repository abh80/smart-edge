package com.abh80.smartedge.plugins.Notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.viewpager.widget.ViewPager;

import com.abh80.smartedge.activities.NotificationManageActivity;
import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.adapters.NotificationManageAppsAdapter;
import com.abh80.smartedge.utils.adapters.NotificationViewPagerAdapter;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;


public class NotificationPlugin extends BasePlugin {
    private View mView;
    private OverlayService context;

    @Override
    public String getID() {
        return "NotificationPlugin";
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(context.getPackageName() + ".NOTIFICATION_POSTED")) {
                Bundle extras = intent.getExtras();
                if (!enabled_apps.contains(extras.getString("package_name"))) return;
                if (Notification.CATEGORY_SYSTEM.equals(extras.getString("category")) && Notification.CATEGORY_SERVICE
                        .equals(extras.getString("category"))) return;
                handleNotificationUpdate(extras.getString("title"), extras.getString("body"), extras.getString("package_name"),
                        extras.getInt("id"), extras);
            }
            if (intent.getAction().equals(context.getPackageName() + ".NOTIFICATION_REMOVED")) {
                int id = intent.getExtras().getInt("id");
                handleNotificationUpdate(id);
            }
            if (intent.getAction().equals(context.getPackageName() + ".NOTIFICATION_APPS_UPDATE")) {
                NotificationPlugin.this.context.sharedPreferences.putString("notifications_apps", intent.getExtras().getString("apps"));
                enabled_apps = NotificationManageAppsAdapter.parseEnabledApps(intent.getExtras().getString("apps"));
            }
        }
    };
    private ArrayList<NotificationMeta> notificationArrayList = new ArrayList<>();

    @Override
    public void onCreate(OverlayService context) {
        this.context = context;
        mHandler = new Handler(context.getMainLooper());
        IntentFilter filter = new IntentFilter();
        filter.addAction(context.getPackageName() + ".NOTIFICATION_POSTED");
        filter.addAction(context.getPackageName() + ".NOTIFICATION_REMOVED");
        filter.addAction(context.getPackageName() + ".NOTIFICATION_APPS_UPDATE");
        context.registerReceiver(broadcastReceiver, filter);
        enabled_apps = NotificationManageAppsAdapter.parseEnabledApps(context.sharedPreferences.getString("notifications_apps", ""));
    }

    NotificationMeta meta;

    @Override
    public String getName() {
        return "Notification";
    }

    @Override
    public void onEvent(AccessibilityEvent event) {
        super.onEvent(event);
    }

    @Override
    public ArrayList<SettingStruct> getSettings() {
        ArrayList<SettingStruct> settingStructs = new ArrayList<>();
        settingStructs.add(new SettingStruct("Manage notifications", "Notification Plugin", SettingStruct.TYPE_CUSTOM) {
            @Override
            public void onClick(Context c) {
                c.startActivity(new Intent(c, NotificationManageActivity.class));
            }
        });
        return settingStructs;
    }

    private void handleNotificationUpdate(int id) {
        Optional<NotificationMeta> to_remove = notificationArrayList.stream().filter(x -> x.getId() == id).findFirst();
        to_remove.ifPresent(notificationMeta -> notificationArrayList.remove(notificationMeta));
        if (notificationArrayList.size() > 0) meta = notificationArrayList.get(0);
        else meta = null;
        if (adapter != null) adapter.notifyDataSetChanged();
        update();
    }

    private ArrayList<String> enabled_apps = new ArrayList<>();

    private void handleNotificationUpdate(String title, String description, String packagename, int id, Bundle all) {
        if (title == null || description == null) return;
        Drawable icon_d = null;

        try {
            icon_d = context.getPackageManager().getApplicationIcon(packagename);
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packagename, 0);
            all.putString("name", (String) context.getPackageManager().getApplicationLabel(ai));
        } catch (Exception e) {
            // do nothing lol
        }
        if (icon_d == null) {
            return;
        }
        meta = new NotificationMeta(title, description, id, icon_d, all);
        Optional<NotificationMeta> meta1 = notificationArrayList.stream().filter(x -> x.getId() == id).findFirst();
        meta1.ifPresent(notificationMeta -> notificationArrayList.remove(notificationMeta));
        notificationArrayList.add(0, meta);
        if (adapter != null) adapter.notifyDataSetChanged();
        context.enqueue(this);
        update();
    }

    private void openOverlay() {
        overlayOpen = true;
        animateChild(false, context.dpToInt(context.minHeight / 4));
    }

    private void closeOverlay() {
        if (expanded) onCollapse();
        if (!overlayOpen) return;
        overlayOpen = false;
        animateChild(false, 0);
        mHandler.postDelayed(() -> context.dequeue(this), 301);
    }

    @Override
    public void onTextColorChange() {
        ((ImageView) mView.findViewById(R.id.cover2)).setImageTintList(ColorStateList.valueOf(context.textColor));
    }

    private void closeOverlay(CallBack callBack) {
        overlayOpen = false;
        if (expanded) onCollapse();
        if (!overlayOpen) {
            callBack.onFinish();
            return;
        }
        animateChild(false, 0, callBack);
    }

    private boolean overlayOpen = false;

    private void update() {
        if (mView == null) return;
        if (meta == null) {
            closeOverlay();
            return;
        }

        if (overlayOpen) {

            if (!expanded) {
                closeOverlay(new CallBack() {
                    @Override
                    public void onFinish() {
                        super.onFinish();
                        updateLayout();
                        openOverlay();
                    }
                });
            } else {
                updateLayout();
                int h = getRequiredHeight();
                if (h == 0) h = 300;
                else h = h + 150 + context.statusBarHeight;
                h += context.dpToInt(20);
                mView.findViewById(R.id.text_info).getLayoutParams().height = h;
                mView.findViewById(R.id.text_info).setLayoutParams(mView.findViewById(R.id.text_info).getLayoutParams());
                context.animateOverlay(h, context.metrics.widthPixels - context.dpToInt(15), true, new CallBack(), new CallBack(), true);
                int imgH = h / 2;
                if (imgH > context.dpToInt(50)) imgH = context.dpToInt(50);
                animateChild(true, imgH);
            }
        } else {
            updateLayout();
            openOverlay();

        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateLayout() {
        if (mView != null && meta != null) {
            ShapeableImageView imageView = mView.findViewById(R.id.cover);
            ShapeableImageView imageView1 = mView.findViewById(R.id.cover2);
            imageView1.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_message_24));
            Drawable small_i = null, large_i = null;
            try {
                Icon small_icon = meta.getAll().getParcelable("icon_small");
                small_i = small_icon.loadDrawable(context);
                Icon large_icon = meta.getAll().getParcelable("icon_large");
                large_i = large_icon.loadDrawable(context);
            } catch (Exception ignored) {
            }
            if (small_i != null) {
                imageView1.setImageDrawable(small_i);
            }
            if (large_i != null) imageView.setImageDrawable(large_i);
            else imageView.setImageDrawable(meta.getIcon_drawable());
            ((ImageView) mView.findViewById(R.id.cover2)).setImageTintList(ColorStateList.valueOf(context.textColor));
        }
    }

    NotificationViewPagerAdapter adapter;
    private final ViewPager.SimpleOnPageChangeListener listener = new ViewPager.SimpleOnPageChangeListener() {
        public void onPageSelected(int position) {
            meta = notificationArrayList.get(position);
            ViewPager pager = mView.findViewById(R.id.text_info);
            View v = (View) pager.findViewWithTag("mv_" + pager.getCurrentItem());
            if (v != null) {
                ((TextView) v.findViewById(R.id.title)).setTextColor(context.textColor);
                ((TextView) v.findViewById(R.id.text_description)).setTextColor(context.textColor);
                StringBuilder stringBuilder = new StringBuilder();
                String name = meta.getAll().getString("name");
                if (name != null) stringBuilder.append(name).append(" • ");
                long since = meta.getAll().getLong("time");
                PrettyTime p = new PrettyTime();
                stringBuilder.append(p.format(new Date(since)));
                ((TextView) mView.findViewById(R.id.author)).setText(stringBuilder.toString());
            }
            update();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onBind() {
        mView = LayoutInflater.from(context).inflate(R.layout.notification_layout, null);
        if (meta == null) {
            context.dequeue(this);
            return mView;
        }
        if (adapter == null)
            adapter = new NotificationViewPagerAdapter(notificationArrayList, context);
        ViewPager pager = mView.findViewById(R.id.text_info);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(listener);
        pager.setClickable(false);
        TabLayout tabLayout = (TabLayout) mView.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(pager, true);
        update();
        return mView;
    }

    @Override
    public void onBindComplete() {
        super.onBindComplete();
        openOverlay();
    }

    @Override
    public void onUnbind() {
        closeOverlay();
        if (mView != null)
            ((ViewPager) mView.findViewById(R.id.text_info)).removeOnPageChangeListener(listener);
        mView = null;
        overlayOpen = false;
    }

    @Override
    public void onDestroy() {
        if (context != null) context.unregisterReceiver(broadcastReceiver);
        meta = null;
    }

    private Handler mHandler;
    private final CallBack OverLayCallBackStart = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                View v = mView.findViewById(R.id.text_info);
                v.setVisibility(View.VISIBLE);
                int width = View.MeasureSpec.makeMeasureSpec(context.metrics.widthPixels - context.dpToInt(15) - context.dpToInt(50) - context.dpToInt(10), View.MeasureSpec.EXACTLY);
                int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                v.measure(width, height);
                v.getLayoutParams().width = v.getMeasuredWidth();
                v.setLayoutParams(v.getLayoutParams());
                v.setAlpha(0);
                ObjectAnimator.ofFloat(v, "alpha", 0, 1f).setDuration(400).start();
            } else {
                View v = mView.findViewById(R.id.text_info);
                ObjectAnimator.ofFloat(v, "alpha", 1f, 0).setDuration(200).start();
                mView.findViewById(R.id.text_info).setVisibility(View.GONE);
                mView.findViewById(R.id.tab_layout).setVisibility(View.GONE);
            }
        }
    };
    private boolean shouldRedraw;

    @Override
    public void onRightSwipe() {
        if (meta != null && !expanded) {
            Intent i = new Intent(context.getPackageName() + ".ACTION_CLOSE");
            i.putExtra("id", meta.getId());
            context.sendBroadcast(i);
            handleNotificationUpdate(meta.getId());
        }
    }

    private final CallBack overLayCallBackEnd = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.findViewById(R.id.tab_layout).setVisibility(View.VISIBLE);
                if (shouldRedraw) {
                    int h = getRequiredHeight();
                    if (h == 0) h = 300;
                    else h = h + 150 + context.statusBarHeight;
                    h += context.dpToInt(20);
                    shouldRedraw = false;
                    context.animateOverlay(h, context.metrics.widthPixels - context.dpToInt(15), true, new CallBack(), new CallBack(), true);
                }
            } else {
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mView.setLayoutParams(layoutParams);
            }
        }
    };
    private boolean expanded = false;

    @Override
    public void onExpand() {
        if (expanded) return;
        expanded = true;
        DisplayMetrics metrics = context.metrics;
        int h = getRequiredHeight();
        if (h == 0) h = 300;
        else h = h + 150 + context.statusBarHeight;
        h += context.dpToInt(20);
        context.animateOverlay(h, metrics.widthPixels - context.dpToInt(15), expanded, OverLayCallBackStart, overLayCallBackEnd, onChange, false);
        int imgH = context.dpToInt(50);
        animateChild(true, imgH);
    }

    private int getRequiredHeight() {
        int h = 0;
        if (mView != null && meta != null) {
            ViewPager pager = mView.findViewById(R.id.text_info);
            View v = (View) pager.findViewWithTag("mv_" + pager.getCurrentItem());
            if (v == null) {
                shouldRedraw = true;
                return 0;
            }
            ;
            int width = View.MeasureSpec.makeMeasureSpec(context.metrics.widthPixels - context.dpToInt(15) - context.dpToInt(50) - context.dpToInt(10), View.MeasureSpec.AT_MOST);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            v.measure(width, height);
            h = v.getMeasuredHeight();
        }
        return h;
    }

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        context.animateOverlay(context.minHeight, ViewGroup.LayoutParams.WRAP_CONTENT, expanded, OverLayCallBackStart, overLayCallBackEnd, onChange, false);
        animateChild(expanded, context.dpToInt(context.minHeight / 4));
    }

    @Override
    public void onClick() {
        if (meta != null) {
            Intent intent = new Intent(context.getPackageName() + ".ACTION_OPEN_CLOSE");
            intent.putExtra("id", meta.getId());
            context.sendBroadcast(intent);
            handleNotificationUpdate(meta.getId());
        }
    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    private final CallBack onChange = new CallBack() {
        @Override
        public void onChange(float p) {
            RelativeLayout relativeLayout = mView.findViewById(R.id.relativeLayout);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) relativeLayout.getLayoutParams();
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) relativeLayout.getParent());
            if (!expanded && p >= 0.2) {
                layoutParams.endToStart = R.id.blank_space;
                constraintSet.connect(R.id.relativeLayout, ConstraintSet.BOTTOM, ((ConstraintLayout) relativeLayout.getParent()).getId(), ConstraintSet.BOTTOM, 0);
                relativeLayout.setPadding(0, 0, 0, 0);
                mView.setPadding(0, 0, 0, 0);
                constraintSet.applyTo((ConstraintLayout) relativeLayout.getParent());
            } else if (expanded && p >= 0.2) {
                layoutParams.endToStart = ConstraintSet.UNSET;
                int pad = context.dpToInt(20);
                relativeLayout.setPadding(context.dpToInt(10), pad, pad, pad);
                layoutParams.bottomToBottom = ConstraintSet.UNSET;
                mView.setPadding(0, context.statusBarHeight, 0, 0);
            }
            relativeLayout.setLayoutParams(layoutParams);
        }
    };

    private void animateChild(boolean expanding, int h) {
        View view1 = mView.findViewById(R.id.cover);
        View view2 = mView.findViewById(R.id.cover2);
        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(expanding ? 500 : 300);
        height_anim.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params1 = view1.getLayoutParams();
            ViewGroup.LayoutParams params2 = view2.getLayoutParams();
            params1.height = (int) valueAnimator.getAnimatedValue();
            params1.width = (int) valueAnimator.getAnimatedValue();
            params2.height = (int) valueAnimator.getAnimatedValue();
            params2.width = (int) valueAnimator.getAnimatedValue();
            view2.setLayoutParams(params2);

            view1.setLayoutParams(params1);
        });
        height_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (expanding) view2.setVisibility(View.INVISIBLE);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!expanding) view2.setVisibility(View.VISIBLE);

            }
        });
        if (expanding) height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();
    }

    private void animateChild(boolean expanding, int h, CallBack callback) {
        View view1 = mView.findViewById(R.id.cover);
        View view2 = mView.findViewById(R.id.cover2);
        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(expanding ? 500 : 300);

        height_anim.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params1 = view1.getLayoutParams();
            ViewGroup.LayoutParams params2 = view2.getLayoutParams();
            params1.height = (int) valueAnimator.getAnimatedValue();
            params2.height = (int) valueAnimator.getAnimatedValue();
            params1.width = (int) valueAnimator.getAnimatedValue();
            params2.width = (int) valueAnimator.getAnimatedValue();
            view1.setLayoutParams(params1);
            view2.setLayoutParams(params2);
        });
        height_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!expanding) view2.setVisibility(View.VISIBLE);
                callback.onFinish();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (expanding) view2.setVisibility(View.INVISIBLE);
            }
        });
        if (expanding) height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();
    }
}
