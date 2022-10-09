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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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
        animateChild(0, new CallBack());
        overlayOpen = false;
        shouldRemoveOverlay();
    }

    public void closeOverlay(CallBack callBack) {
        animateChild(0, callBack);
        overlayOpen = false;
    }

    private ImageView pause_play;

    public MediaController getActiveCurrent(List<MediaController> mediaControllers) {
        if (mediaControllers.size() == 0) return null;
        try {
            Optional<MediaController> controller = mediaControllers.stream().filter(x -> x.getPlaybackState().getState() == PlaybackState.STATE_PLAYING).findFirst();
            return controller.orElse(null);
        } catch (Exception e) {
            return null;
        }
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
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
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

    private void layoutHandle(View v, int width) {
        int width1 = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(width1, height);
        v.getLayoutParams().width = v.getMeasuredWidth();
        v.getLayoutParams().height = v.getMeasuredHeight();
        v.setLayoutParams(v.getLayoutParams());
    }

    public void openOverlay(String pkg_name) {
        if (overlayOpen) return;
        overlayOpen = true;
        current_package_name = pkg_name;
        animateChild(ctx.dpToInt(ctx.minHeight / 4), new CallBack());
    }

    private View mView;
    private ShapeableImageView cover;

    private void init() {
        seekBar = mView.findViewById(R.id.progressBar);
        elapsedView = mView.findViewById(R.id.elapsed);
        remainingView = mView.findViewById(R.id.remaining);
        pause_play = mView.findViewById(R.id.pause_play);
        ImageView next = mView.findViewById(R.id.next_play);
        ImageView back = mView.findViewById(R.id.back_play);
        cover = mView.findViewById(R.id.cover);
        coverHolder = mView.findViewById(R.id.relativeLayout);
        text_info = mView.findViewById(R.id.text_info);
        controls_holder = mView.findViewById(R.id.controls_holder);

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

    RelativeLayout coverHolder;
    private final CallBack onChange = new CallBack() {
        @Override
        public void onChange(float p) {
            float f;
            int h = (ctx.minHeight - (ctx.minHeight / 4)) / 2;
            if (expanded) {
                f = p;
            } else {
                f = 1 - p;
            }
            mView.setPadding(0, (int) (f * ctx.statusBarHeight), 0, 0);
            ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = (int) (f * ctx.dpToInt(20));
        }
    };

    @Override
    public void onExpand() {
        if (expanded) return;
        expanded = true;
        DisplayMetrics metrics = ctx.metrics;
        ctx.animateOverlay(ctx.dpToInt(210), metrics.widthPixels - ctx.dpToInt(15), expanded, OverLayCallBackStart, overLayCallBackEnd, onChange);
        animateChild(true, ctx.dpToInt(76));

    }

    LinearLayout text_info;
    LinearLayout controls_holder;
    CallBack OverLayCallBackStart = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.findViewById(R.id.blank_space).setVisibility(View.GONE);
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                text_info.setVisibility(View.VISIBLE);
                controls_holder.setVisibility(View.VISIBLE);
                seekBar.setVisibility(View.VISIBLE);
                mView.findViewById(R.id.title).setSelected(true);
                mView.findViewById(R.id.artist_subtitle).setSelected(true);
                elapsedView.setVisibility(View.VISIBLE);
                remainingView.setVisibility(View.VISIBLE);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).removeRule(RelativeLayout.CENTER_VERTICAL);
                coverHolder.setLayoutParams(coverHolder.getLayoutParams());
            } else {
                mHandler.removeCallbacks(r);
                text_info.setVisibility(View.GONE);
                controls_holder.setVisibility(View.GONE);
                seekBar.setVisibility(View.GONE);
                mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
                elapsedView.setVisibility(View.GONE);
                remainingView.setVisibility(View.GONE);

                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).addRule(RelativeLayout.CENTER_VERTICAL);
                coverHolder.setLayoutParams(coverHolder.getLayoutParams());

            }
        }
    };
    CallBack overLayCallBackEnd = new CallBack() {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.setPadding(0, ctx.statusBarHeight, 0, 0);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = ctx.dpToInt(20);
                if (mCurrent != null && mCurrent.getPlaybackState() != null) {
                    if (mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.pause));
                    } else {
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.play));
                    }
                }
                mHandler.post(r);
                text_info.setAlpha(1);
                controls_holder.setAlpha(1);
                seekBar.setAlpha(1);
                elapsedView.setAlpha(1);
                remainingView.setAlpha(1);
            } else {
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mView.setLayoutParams(layoutParams);
                mView.setPadding(0, 0, 0, 0);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = 0;
            }
        }
    };

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        ctx.animateOverlay(ctx.minHeight, ViewGroup.LayoutParams.WRAP_CONTENT, expanded, OverLayCallBackStart, overLayCallBackEnd, onChange);
        animateChild(false, ctx.dpToInt(ctx.minHeight / 4));
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
        ShapeableImageView imageView = cover;
        titleView.setText(queueStruct.getTitle());
        artistView.setText(queueStruct.getArtist());
        imageView.setImageBitmap(queueStruct.getCover());
    }


    private void animateChild(boolean expanding, int h) {
        View view1 = cover;
        View view2 = visualizer;

        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(500);
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
                if (!expanding) {
                    view2.setVisibility(View.VISIBLE);
                    visualizer.paused = false;
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (expanding) {
                    view2.setVisibility(View.GONE);
                    visualizer.paused = true;
                }
            }
        });
        height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();


    }

    private void animateChild(int h, CallBack callback) {
        View view1 = cover;
        View view2 = visualizer;
        ViewGroup.LayoutParams params1 = view1.getLayoutParams();
        ViewGroup.LayoutParams params2 = view2.getLayoutParams();
        params1.height = h;
        params2.height = h;
        params1.width = h;
        params2.width = h;
        view1.setScaleY(0);
        view1.setScaleX(0);
        view2.setScaleX(0);
        view2.setScaleY(0);
        view1.setLayoutParams(params1);
        view2.setLayoutParams(params2);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(h != 0 ? 0 : 1, h != 0 ? 1 : 0);
        valueAnimator.addUpdateListener(l -> {
            float f = (float) l.getAnimatedValue();
            view1.setScaleX(f);
            view1.setScaleY(f);
            view2.setScaleX(f);
            view2.setScaleY(f);
        });
        valueAnimator.setDuration(300);
        valueAnimator.start();
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callback.onFinish();
            }
        });


    }

}
