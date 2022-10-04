package com.abh80.smartedge.plugins.MediaSession;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MediaSessionPlugin extends BasePlugin {

    private SeekBar seekBar;
    private TextView elapsedView;
    private TextView remainingView;
    public String current_package_name = "";
    public boolean expanded = false;
    public Map<String, MediaController.Callback> callbackMap = new HashMap<>();
    private boolean seekbar_dragging = false;
    public Instant last_played;
    OverlayService ctx;
    Handler mHandler;
    public MediaController mCurrent;

    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            if (!expanded) return;
            if (mCurrent == null) {
                closeOverlay();
                return;
            }
            long elapsed = mCurrent.getPlaybackState().getPosition();
            if (elapsed < 0) {
                closeOverlay();
                return;
            }
            if (mCurrent.getMetadata() == null) {
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


    private boolean overlayOpen = false;

    public boolean overlayOpen() {
        return overlayOpen;
    }

    public void closeOverlay() {
        animateChild(false, 0);
        overlayOpen = false;
        shouldRemoveOverlay();
    }

    public void closeOverlay(CallBack callBack) {
        animateChild(false, 0, callBack);
        overlayOpen = false;
    }

    private ImageView pause_play;

    public MediaController getActiveCurrent(List<MediaController> mediaControllers) {
        if (mediaControllers.size() == 0) return null;
        Optional<MediaController> controller = mediaControllers.stream().filter(x -> x.getPlaybackState().getState() == PlaybackState.STATE_PLAYING).findFirst();
        return controller.orElse(null);
    }

    private boolean isColorDark(int color) {
        // Source : https://stackoverflow.com/a/24261119
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        // It's a dark color
        return !(darkness < 0.5); // It's a light color
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayerResume(boolean b) {
        if (expanded && b) {
            pause_play.setImageDrawable(ctx.getDrawable(R.drawable.avd_play_to_pause));
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
        if (mCurrent == null) return;
        int index = -1;
        List<MediaController> controllerList = mediaSessionManager.getActiveSessions(new ComponentName(ctx.getBaseContext(), NotiService.class));
        for (int v = 0; v < controllerList.size(); v++) {
            if (Objects.equals(controllerList.get(v).getPackageName(), mCurrent.getPackageName())) {
                index = v;
                break;
            }

        }
        if (index == -1) return;
        visualizer.setPlayerId(index);
        if (mCurrent.getMetadata() == null) return;
        Bitmap bm = mCurrent.getMetadata().getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (bm == null) return;
        int dc = getDominantColor(bm);
        if (isColorDark(dc)) {
            dc = lightenColor(dc);
        }
        visualizer.setColor(dc);
    }

    private int lightenColor(int colorin) {
        Color color = Color.valueOf(colorin);
        double fraction = 0.3;
        float red = (float) (Math.min(255, color.red() * 255f + 255 * fraction) / 225f);
        float green = (float) (Math.min(255, color.green() * 255f + 255 * fraction) / 225f);
        float blue = (float) (Math.min(255, color.blue() * 255f + 255 * fraction) / 225f);
        float alpha = color.alpha();

        return Color.valueOf(red, green, blue, alpha).toArgb();

    }

    private SongVisualizer visualizer;
    private MediaSessionManager mediaSessionManager;

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayerPaused(boolean b) {
        if (expanded && b) {
            pause_play.setImageDrawable(ctx.getDrawable(R.drawable.avd_pause_to_play));
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
        last_played = Instant.now();
        mHandler.postDelayed(() -> {
            if (Math.abs(Instant.now().toEpochMilli() - last_played.toEpochMilli()) >= 60 * 1000) {
                if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(ctx, NotiService.class))) == null)
                    closeOverlay();
            }
        }, 60 * 1000);
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    @Override
    public String getID() {
        return "MediaSessionPlugin";
    }

    private MediaSessionManager.OnActiveSessionsChangedListener listnerForActiveSessions = list -> {
        list.forEach(x -> {
            if (callbackMap.get(x.getPackageName()) != null) return;
            MediaCallback c = new MediaCallback(x, this);
            callbackMap.put(x.getPackageName(), c);
            x.registerCallback(c);
        });
    };

    @Override
    public void onCreate(OverlayService context) {
        ctx = context;
        mHandler = new Handler(context.getMainLooper());
        mView = LayoutInflater.from(context).inflate(R.layout.media_session_layout, null);
        init();

        mediaSessionManager = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSessionManager.addOnActiveSessionsChangedListener(listnerForActiveSessions, new

                ComponentName(ctx, NotiService.class));
        mediaSessionManager.getActiveSessions(new

                        ComponentName(ctx, NotiService.class)).

                forEach(x ->

                {
                    if (callbackMap.get(x.getPackageName()) != null) return;
                    MediaCallback c = new MediaCallback(x, this);
                    callbackMap.put(x.getPackageName(), c);
                    x.registerCallback(c);
                });
    }

    public void shouldRemoveOverlay() {
        if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(ctx, NotiService.class))) == null) {
            ctx.dequeue(this);
        }
    }

    public void openOverlay(String pkg_name) {
        if (overlayOpen) return;
        overlayOpen = true;
        current_package_name = pkg_name;
        animateChild(false, ctx.dpToInt(25));
    }

    private View mView;

    private void init() {
        seekBar = mView.findViewById(R.id.progressBar);
        elapsedView = mView.findViewById(R.id.elapsed);
        remainingView = mView.findViewById(R.id.remaining);
        mView.findViewById(R.id.blank_space).setMinimumWidth(300);
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
        visualizer = mView.findViewById(R.id.visualizer);
    }

    @Override
    public View onBind() {
        return mView;
    }

    @Override
    public void onUnbind() {
        mHandler.removeCallbacks(r);
    }

    @Override
    public void onDestroy() {
        if (visualizer != null) visualizer.release();
        if (mediaSessionManager != null)
            mediaSessionManager.removeOnActiveSessionsChangedListener(listnerForActiveSessions);
        mediaSessionManager = null;
        mCurrent = null;
        mView = null;

    }

    @Override
    public void onExpand() {
        if (expanded) return;
        expanded = true;
        DisplayMetrics metrics = new DisplayMetrics();
        ctx.mWindowManager.getDefaultDisplay().getMetrics(metrics);
        ctx.animateOverlay(500, metrics.widthPixels - 40, expanded, OverLayCallBackStart, overLayCallBackEnd);
        animateChild(true, (int) (500 / 2.5));
    }

    CallBack OverLayCallBackStart = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            RelativeLayout relativeLayout = mView.findViewById(R.id.relativeLayout);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) relativeLayout.getLayoutParams();
            if (expanded) {
                layoutParams.endToStart = ConstraintSet.UNSET;
                layoutParams.bottomToTop = R.id.guideline_half;
                int pad = ctx.dpToInt(20);
                relativeLayout.setPadding(pad, pad, pad, pad);
            } else {
                layoutParams.endToStart = R.id.blank_space;
                layoutParams.bottomToTop = ConstraintSet.UNSET;
                relativeLayout.setPadding(0, 0, 0, 0);
                mHandler.removeCallbacks(r);
                mView.findViewById(R.id.text_info).setVisibility(View.GONE);
                mView.findViewById(R.id.controls_holder).setVisibility(View.GONE);
                mView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.elapsed).setVisibility(View.GONE);
                mView.findViewById(R.id.remaining).setVisibility(View.GONE);
            }
            relativeLayout.setLayoutParams(layoutParams);
        }
    };
    CallBack overLayCallBackEnd = new CallBack() {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onFinish() {
            super.onFinish();
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
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.pause));
                    } else {
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.play));
                    }
                }
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mView.setLayoutParams(layoutParams);
                mHandler.post(r);
            } else {
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mView.setLayoutParams(layoutParams);
            }
        }
    };

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        ctx.animateOverlay(100, ViewGroup.LayoutParams.WRAP_CONTENT, expanded, OverLayCallBackStart, overLayCallBackEnd);
        animateChild(false, ctx.dpToInt(25));
    }

    @Override
    public void onClick() {
        if (expanded) return;
        if (mCurrent != null && mCurrent.getSessionActivity() != null) {
            try {
                mCurrent.getSessionActivity().send(0);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    @Override
    public String getName() {
        return "Media Session";
    }

    @Override
    public ArrayList<SettingStruct> getSettings() {
        return null;
    }


    public void queueUpdate(UpdateQueueStruct queueStruct) {
        ctx.enqueue(this);
        TextView titleView = mView.findViewById(R.id.title);
        TextView artistView = mView.findViewById(R.id.artist_subtitle);
        ShapeableImageView imageView = mView.findViewById(R.id.cover);
        titleView.setText(queueStruct.getTitle());
        artistView.setText(queueStruct.getArtist());
        imageView.setImageBitmap(queueStruct.getCover());
    }


    private void animateChild(boolean expanding, int h) {
        View view1 = mView.findViewById(R.id.cover);
        View view2 = visualizer;
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
        View view2 = visualizer;
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
        height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();
    }

}
