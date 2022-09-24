package com.abh80.smartedge;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Objects;

public class MediaCallback extends MediaController.Callback {
    public MediaCallback(MediaController mCurrent, View view, OverlayService context) {
        this.mCurrent = mCurrent;
        this.mView = view;
        this.ctx = context;
        try {
            isPlaying = mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
            updateView();
        } catch (Exception e) {
            // do nothing lol
        }
    }

    private final OverlayService ctx;
    private final View mView;
    private final MediaController mCurrent;
    private MediaMetadata mediaMetadata;
    private boolean isPlaying = true;

    private void updateView() {
        if (!isPlaying) return;
        if (mCurrent.getMetadata() == null) return;
        mediaMetadata = mCurrent.getMetadata();
        Bitmap b = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) {
            return;
        }
        ShapeableImageView imageView = mView.findViewById(R.id.cover);
        imageView.setImageBitmap(b);
        String title = mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        TextView titleView = mView.findViewById(R.id.title);
        TextView artistView = mView.findViewById(R.id.artist_subtitle);
        titleView.setText(title);
        artistView.setText(artist);
        ctx.openOverLay(mCurrent.getPackageName());
        ctx.mCurrent = mCurrent;
        int index = -1;
        List<MediaController> controllerList = ctx.mediaSessionManager.getActiveSessions(new ComponentName(ctx, NotiService.class));
        for (int v = 0; v < controllerList.size(); v++) {
            if (Objects.equals(controllerList.get(v).getPackageName(), mCurrent.getPackageName())) {
                index = v;
                break;
            }

        }
        if (index == -1) return;
        SongVisualizer visualizer = mView.findViewById(R.id.visualizer);
        visualizer.paint.setColor(Color.BLUE);
        visualizer.paint.setStyle(Paint.Style.FILL);
        int color = getDominantColor(b);
        visualizer.paint.setColor(color);
        visualizer.paint.setStrokeCap(Paint.Cap.ROUND);
        visualizer.setPlayerId(index);
    }
    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }
    @Override
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
        super.onPlaybackStateChanged(state);
        if (state == null || mCurrent.getMetadata() == null) return;
        boolean isPlaying2 = state.getState() == PlaybackState.STATE_PLAYING;
        if (mediaMetadata != null && mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART).sameAs(mCurrent.getMetadata().getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)) && isPlaying2 != isPlaying) {
            if (!isPlaying2) ctx.onPlayerPaused(true);
            else ctx.onPlayerResume(true);
            isPlaying = isPlaying2;
            return;
        }
        isPlaying = isPlaying2;

        if (!isPlaying) ctx.onPlayerPaused(false);
        if (!isPlaying) return;
        if (ctx.expanded) {
            updateView();
            return;
        }
        ctx.closeOverlay(new CallBack() {
            @Override
            public void onFinish() {
                super.onFinish();
                updateView();
            }
        });
    }

    @Override
    public void onSessionDestroyed() {
        super.onSessionDestroyed();
        if (mCurrent != null) {
            mCurrent.unregisterCallback(this);
            ctx.callbackMap.remove(mCurrent.getPackageName());
            ctx.mCurrent = null;
        }
        ctx.shouldRemoveOverlay();

    }
}
