package com.abh80.smartedge.plugins.Notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
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
                handleNotificationUpdate(extras.getString("title"), extras.getString("body"), extras.getString("package_name"),
                        extras.getInt("id"));
            }
            if (intent.getAction().equals(context.getPackageName() + ".NOTIFICATION_REMOVED")) {
                int id = intent.getExtras().getInt("id");
                handleNotificationUpdate(id);
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
        context.registerReceiver(broadcastReceiver, filter);
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
        return null;
    }

    private void handleNotificationUpdate(int id) {
        Optional<NotificationMeta> to_remove = notificationArrayList.stream().filter(x -> x.getId() == id).findFirst();
        to_remove.ifPresent(notificationMeta -> notificationArrayList.remove(notificationMeta));
        if (notificationArrayList.size() > 0) meta = notificationArrayList.get(0);
        else meta = null;
        update();
    }

    private void handleNotificationUpdate(String title, String description, String packagename, int id) {
        if (title == null || description == null) return;
        Drawable icon_d = null;

        try {
            icon_d = context.getPackageManager().getApplicationIcon(packagename);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing lol
        }
        if (icon_d == null) {
            return;
        }
        meta = new NotificationMeta(title, description, id, icon_d);
        Optional<NotificationMeta> meta1 = notificationArrayList.stream().filter(x -> x.getId() == id).findFirst();
        meta1.ifPresent(notificationMeta -> notificationArrayList.remove(notificationMeta));
        notificationArrayList.add(0, meta);
        context.enqueue(this);
        update();
    }

    private void openOverlay() {
        overlayOpen = true;
        animateChild(false, context.dpToInt(25));
    }

    private void closeOverlay() {
        if (expanded) onCollapse();
        if (!overlayOpen) return;
        overlayOpen = false;
        animateChild(false, 0);
        mHandler.postDelayed(() -> context.dequeue(this), 301);
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
        ShapeableImageView imageView = mView.findViewById(R.id.cover);
        if (overlayOpen) {
            if (mView != null) {
                if (!expanded) {
                    closeOverlay(new CallBack() {
                        @Override
                        public void onFinish() {
                            super.onFinish();
                            imageView.setImageDrawable(meta.getIcon_drawable());
                            ((TextView) mView.findViewById(R.id.title)).setText(meta.getTitle());
                            ((TextView) mView.findViewById(R.id.text_description)).setText(meta.getDescription());
                            openOverlay();
                        }
                    });
                } else {
                    imageView.setImageDrawable(meta.getIcon_drawable());
                    ((TextView) mView.findViewById(R.id.title)).setText(meta.getTitle());
                    ((TextView) mView.findViewById(R.id.text_description)).setText(meta.getDescription());
                }
            }
        } else {
            if (mView != null) {
                imageView.setImageDrawable(meta.getIcon_drawable());
                ((TextView) mView.findViewById(R.id.title)).setText(meta.getTitle());
                ((TextView) mView.findViewById(R.id.text_description)).setText(meta.getDescription());
                openOverlay();
            }
        }
    }

    @Override
    public View onBind() {
        mView = LayoutInflater.from(context).inflate(R.layout.notification_layout, null);
        if (meta == null) {
            context.dequeue(this);
            return mView;
        }
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
        mView = null;
        overlayOpen = false;
    }

    @Override
    public void onDestroy() {
        if(context != null )context.unregisterReceiver(broadcastReceiver);
        meta = null;
    }

    private Handler mHandler;
    private final CallBack OverLayCallBackStart = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            RelativeLayout relativeLayout = mView.findViewById(R.id.relativeLayout);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) relativeLayout.getLayoutParams();
            if (expanded) {
                layoutParams.endToStart = ConstraintSet.UNSET;
                int pad = context.dpToInt(20);
                relativeLayout.setPadding(pad, pad, pad, pad);
            } else {
                layoutParams.endToStart = R.id.blank_space;
                relativeLayout.setPadding(0, 0, 0, 0);
                mView.findViewById(R.id.text_info).setVisibility(View.GONE);
            }
            relativeLayout.setLayoutParams(layoutParams);
        }
    };
    private final CallBack overLayCallBackEnd = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.findViewById(R.id.text_info).setVisibility(View.VISIBLE);

                mView.findViewById(R.id.title).setSelected(true);
                mView.findViewById(R.id.text_description).setSelected(true);

                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mView.setLayoutParams(layoutParams);
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
        DisplayMetrics metrics = new DisplayMetrics();
        context.mWindowManager.getDefaultDisplay().getMetrics(metrics);
        context.animateOverlay(300, metrics.widthPixels - 40, expanded, OverLayCallBackStart, overLayCallBackEnd);
        animateChild(true, (int) (500 / 4));
    }

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        context.animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT, expanded, OverLayCallBackStart, overLayCallBackEnd);
        animateChild(expanded, context.dpToInt(25));
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
