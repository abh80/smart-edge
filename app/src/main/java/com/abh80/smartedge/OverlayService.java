package com.abh80.smartedge;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimatedVectorDrawable;
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
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.NotificationCompat;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OverlayService extends Service {
    private SeekBar seekBar;
    private TextView elapsedView;
    private TextView remainingView;
    private Runnable r = new Runnable() {
        @Override
        public void run() {
            if (!expanded) return;
            if (mCurrent == null) {
                expanded = false;
                animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                closeOverlay();
                return;
            }
            long elapsed = mCurrent.getPlaybackState().getPosition();
            if (elapsed < 0) {
                expanded = false;
                animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                closeOverlay();
                return;
            }
            if (mCurrent.getMetadata() == null) {
                expanded = false;
                animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                closeOverlay();
                return;
            }
            long total = mCurrent.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);
            elapsedView.setText(DurationFormatUtils.formatDuration(elapsed, "mm:ss", true));
            remainingView.setText("-" + DurationFormatUtils.formatDuration(Math.abs(total - elapsed), "mm:ss", true));
            if (!seekbar_dragging) seekBar.setProgress((int) ((((float) elapsed / total) * 100)));
            mHandler.post(r);
        }
    };
    private ImageView pause_play;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String current_package_name = "";

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
                    layoutParams.bottomToTop = R.id.guideline_half;
                    int pad = dpToInt(20);
                    relativeLayout.setPadding(pad, pad, pad, pad);
                    mHandler.post(r);
                } else {
                    layoutParams.endToStart = R.id.blank_space;
                    layoutParams.bottomToTop = ConstraintSet.UNSET;
                    relativeLayout.setPadding(0, 0, 0, 0);
                }
                relativeLayout.setLayoutParams(layoutParams);
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (expanded) {
                    mView.findViewById(R.id.text_info).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.controls_holder).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.title).setSelected(true);
                    mView.findViewById(R.id.artist_subtitle).setSelected(true);
                    mView.findViewById(R.id.elapsed).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.remaining).setVisibility(View.VISIBLE);
                    if (mCurrent != null && mCurrent.getPlaybackState() != null) {
                        if (mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                            pause_play.setImageDrawable(getDrawable(R.drawable.pause));
                        } else {
                            pause_play.setImageDrawable(getDrawable(R.drawable.play));
                        }
                    }
                } else {
                    mView.findViewById(R.id.text_info).setVisibility(View.GONE);
                    mView.findViewById(R.id.controls_holder).setVisibility(View.GONE);
                    mView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                    mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.elapsed).setVisibility(View.GONE);
                    mView.findViewById(R.id.remaining).setVisibility(View.GONE);
                }
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

    public MediaController mCurrent;

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

    private void animateChild(View view, int w, int h, CallBack callback) {
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
        height_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callback.onFinish();
            }
        });
        width_anim.start();
        height_anim.start();
    }

    public boolean expanded = false;
    public Map<String, MediaController.Callback> callbackMap = new HashMap<>();
    private boolean seekbar_dragging = false;

    @SuppressLint("UseCompatLoadingForDrawables")
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
        seekBar = mView.findViewById(R.id.progressBar);

        elapsedView = mView.findViewById(R.id.elapsed);
        remainingView = mView.findViewById(R.id.remaining);
        mView.findViewById(R.id.blank_space).setMinimumWidth(200);
        mParams.gravity = Gravity.TOP;
        pause_play = mView.findViewById(R.id.pause_play);
        ImageView next = mView.findViewById(R.id.next_play);
        ImageView back = mView.findViewById(R.id.back_play);
        pause_play.setOnClickListener(l -> {
            if (mCurrent == null) return;
            if (mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PAUSED) {
                mCurrent.getTransportControls().play();

            } else {
                mCurrent.getTransportControls().pause();

            }
        });
        next.setOnClickListener(l -> {
            if (mCurrent == null) return;
            mCurrent.getTransportControls().skipToNext();
        });
        back.setOnClickListener(l -> {
            if (mCurrent == null) return;
            mCurrent.getTransportControls().skipToPrevious();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbar_dragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekbar_dragging = false;
                mCurrent.getTransportControls().seekTo((long) ((float) seekBar.getProgress() / 100 * mCurrent.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION)));
            }
        });
        mView.setOnClickListener(l -> {
            if (!overlayOpen) return;

            DisplayMetrics metrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            if (!expanded) {
                expanded = true;
                animateOverlay(500, metrics.widthPixels - 40);
                animateChild(mView.findViewById(R.id.cover), (int) (500 / 2.5), (int) (500 / 2.5));
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
                if (callbackMap.get(x.getPackageName()) != null) return;
                MediaCallback c = new MediaCallback(x, mView, this);
                callbackMap.put(x.getPackageName(), c);
                x.registerCallback(c);
            });
        }, new ComponentName(this, NotiService.class));
        mediaSessionManager.getActiveSessions(new ComponentName(this, NotiService.class)).forEach(x -> {
            if (callbackMap.get(x.getPackageName()) != null) return;
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
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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

    private boolean overlayOpen = false;

    public void openOverLay(String packagename) {
        if (overlayOpen) return;
        current_package_name = packagename;
        animateChild(mView.findViewById(R.id.cover), dpToInt(25), dpToInt(25));
        overlayOpen = true;
    }

    public void shouldRemoveOverlay() {
        if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(this, NotiService.class))) == null) {
            closeOverlay();
            mCurrent = null;
        }
    }

    public void closeOverlay() {
        if (!overlayOpen) return;
        animateChild(mView.findViewById(R.id.cover), 0, 0);
        overlayOpen = false;
    }

    public void closeOverlay(CallBack callback) {
        if (!overlayOpen) {
            callback.onFinish();
            return;
        }
        ;
        animateChild(mView.findViewById(R.id.cover), 0, 0, callback);
        overlayOpen = false;
    }

    public final Handler mHandler = new Handler();

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayerResume(boolean b) {
        if (expanded && b) {
            pause_play.setImageDrawable(getBaseContext().getDrawable(R.drawable.avd_play_to_pause));
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayerPaused(boolean b) {
        if (expanded && b) {
            pause_play.setImageDrawable(getBaseContext().getDrawable(R.drawable.avd_pause_to_play));
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
        if (b) return;
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
