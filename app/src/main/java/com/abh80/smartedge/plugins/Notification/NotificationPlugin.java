package com.abh80.smartedge.plugins.Notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.google.android.material.imageview.ShapeableImageView;


public class NotificationPlugin extends BasePlugin {
    private final String TAG = getClass().getSimpleName();
    private View mView;
    private OverlayService context;

    @Override
    public String getID() {
        return "NotificationPlugin";
    }

    @Override
    public void onCreate(OverlayService context) {
        this.context = context;
        mHandler = new Handler(context.getMainLooper());
    }

    NotificationMeta meta;
    private Notification notification;

    @Override
    public void onEvent(AccessibilityEvent event) {
        super.onEvent(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Notification n = (Notification) event.getParcelableData();
            String title = n.extras.getString("android.title");
            String description = n.extras.getString("android.text");
            if (title == null || description == null) return;
            Drawable icon_d = null;

            try {
                icon_d = context.getPackageManager().getApplicationIcon(event.getPackageName().toString());
            } catch (PackageManager.NameNotFoundException e) {
                // do nothing lol
            }
            if (icon_d == null) {
                return;
            }
            meta = new NotificationMeta(title, description, icon_d);
            context.enqueue(this);
            update();
            notification = n;
        }
    }

    private void openOverlay() {
        overlayOpen = true;
        animateChild(false, context.dpToInt(25));
    }

    private void closeOverlay() {
        overlayOpen = false;
        animateChild(false, 0);
        notification = null;
        mHandler.postDelayed(() -> context.dequeue(this), 301);
    }

    private void closeOverlay(CallBack callBack) {
        overlayOpen = false;
        animateChild(false, 0, callBack);
        notification = null;
    }

    private boolean overlayOpen = false;

    private void update() {
        ShapeableImageView imageView = mView.findViewById(R.id.cover);
        if (overlayOpen && mView != null) {
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
            if (mView != null) {
                imageView.setImageDrawable(meta.getIcon_drawable());
                ((TextView) mView.findViewById(R.id.title)).setText(meta.getTitle());
                ((TextView) mView.findViewById(R.id.text_description)).setText(meta.getDescription());
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
        meta = null;
        overlayOpen = false;
    }

    @Override
    public void onDestroy() {
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
                mView.findViewById(R.id.text_info).setVisibility(View.GONE);
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
        if (notification != null) {
            try {
                if (notification.contentIntent != null) notification.contentIntent.send();
                if (notification.deleteIntent != null) notification.deleteIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
        closeOverlay();
    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    private void animateChild(boolean expanding, int h) {
        View view1 = mView.findViewById(R.id.cover);
        View view2 = mView.findViewById(R.id.cover2);
        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(300);
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
        height_anim.start();
    }

    private void animateChild(boolean expanding, int h, CallBack callback) {
        View view1 = mView.findViewById(R.id.cover);
        View view2 = mView.findViewById(R.id.cover2);
        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(300);
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
        height_anim.start();
    }
}
