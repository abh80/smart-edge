package com.abh80.smartedge;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.NotificationCompat;

import com.google.android.material.imageview.ShapeableImageView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class OverlayService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Map<String, MediaCallback> callbackMap = new HashMap<>();

    private void animateOverlay(int h, int w) {
        ValueAnimator height_anim = ValueAnimator.ofInt(mParams.height, h);
        height_anim.setDuration(200);
        height_anim.addUpdateListener(valueAnimator -> {
            mParams.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, mParams);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(200);
        width_anim.addUpdateListener(v2 -> {
            mParams.width = (int) v2.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, mParams);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                RelativeLayout relativeLayout = mView.findViewById(R.id.relativeLayout);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) relativeLayout.getLayoutParams();
                if (expanded) {
                    layoutParams.endToStart = ConstraintSet.UNSET;
                    int pad = dpToInt(20);
                    relativeLayout.setPadding(pad, pad, pad, pad);
                } else {
                    layoutParams.endToStart = R.id.blank_space;
                    relativeLayout.setPadding(0, 0, 0, 0);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (expanded) {
                    mView.findViewById(R.id.text_info).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.title).setSelected(true);
                } else {
                    mView.findViewById(R.id.text_info).setVisibility(View.GONE);
                    mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
                }
            }
        });
        width_anim.start();
        height_anim.start();
    }

    private void animateChild(View view, int w, int h) {
        ValueAnimator height_anim = ValueAnimator.ofInt(view.getHeight(), h);
        height_anim.setDuration(200);
        height_anim.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = (int) valueAnimator.getAnimatedValue();
            view.setLayoutParams(params);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(view.getWidth(), w);
        width_anim.setDuration(200);
        width_anim.addUpdateListener(v2 -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (int) v2.getAnimatedValue();
            view.setLayoutParams(params);
        });
        width_anim.start();
        height_anim.start();
    }

    private boolean expanded = false;


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCreate() {
        super.onCreate();
        startOnForeground();
        mParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 100,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,

                PixelFormat.TRANSLUCENT);
        layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.overlay_layout, null);
        mView.findViewById(R.id.blank_space).setMinimumWidth(200);
        mParams.gravity = Gravity.TOP;
        mView.setOnClickListener(l -> {
            if (!expanded) {
                expanded = true;
                animateOverlay(500, mWindowManager.getCurrentWindowMetrics().getBounds().width() - 40);
                animateChild(mView.findViewById(R.id.cover), 250, 250);
            } else {
                expanded = false;
                animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                animateChild(mView.findViewById(R.id.cover), dpToInt(25), dpToInt(25));
            }
        });
        mParams.verticalMargin = -0.035f;
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSessionManager.addOnActiveSessionsChangedListener(list -> {
            list.forEach(x -> {
                MediaCallback c = new MediaCallback(x, mView, this);
                callbackMap.put(x.getPackageName(), c);
                x.registerCallback(c);
            });
        }, new ComponentName(this, NotiService.class));
        mediaSessionManager.getActiveSessions(new ComponentName(this, NotiService.class)).forEach(x -> {
            MediaCallback c = new MediaCallback(x, mView, this);
            callbackMap.put(x.getPackageName(), c);
            x.registerCallback(c);
        });
        try {

            if (mView.getWindowToken() == null) {
                if (mView.getParent() == null) {
                    mWindowManager.addView(mView, mParams);
                }
            }
        } catch (Exception e) {
            Log.d("Error1", e.toString());
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            mView.setVisibility(View.INVISIBLE);
        } else mView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Instant last_played;

    private MediaController getActiveCurrent(List<MediaController> mediaControllers) {
        if (mediaControllers.size() == 0) return null;
        Optional<MediaController> controller = mediaControllers.stream().filter(x -> x.getPlaybackState().getState() == PlaybackState.STATE_PLAYING).findFirst();
        return controller.orElse(null);
    }

    private int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    public void shouldRemoveOverlay() {
        if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(this, NotiService.class))) == null) {
            closeOverlay();
        }
    }

    public void closeOverlay() {
        animateChild(mView.findViewById(R.id.cover), 0, 0);
    }

    private final Handler mHandler = new Handler();

    public void onPlayerPaused() {
        last_played = Instant.now();
        mHandler.postDelayed(() -> {
            if (Math.abs(Instant.now().toEpochMilli() - last_played.toEpochMilli()) >= 60 * 1000) {
                if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(this, NotiService.class))) == null)
                    closeOverlay();
            }
        }, 60 * 1000);
    }

    private View mView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    private LayoutInflater layoutInflater;
    private MediaSessionManager mediaSessionManager;
    DisplayMetrics metrics;

    private void startOnForeground() {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Service running")
                .setContentText("Displaying over other apps")
                .setSmallIcon(R.drawable.ic_launcher_foreground)

                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}
