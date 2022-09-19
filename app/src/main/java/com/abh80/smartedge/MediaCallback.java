package com.abh80.smartedge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
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

import com.google.android.material.imageview.ShapeableImageView;

public class MediaCallback extends MediaController.Callback {
    public MediaCallback(MediaController mCurrent, View view, OverlayService context) {
        this.mCurrent = mCurrent;
        this.mView = view;
        this.ctx = context;
        mediaMetadata = mCurrent.getMetadata();
        isPlaying = mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
        updateView();
    }

    private final OverlayService ctx;
    private final View mView;
    private final MediaController mCurrent;
    private MediaMetadata mediaMetadata;
    private boolean isPlaying = true;

    @Override
    public void onMetadataChanged(@Nullable MediaMetadata metadata) {
        super.onMetadataChanged(metadata);
        if (metadata == null) return;
        mediaMetadata = metadata;
    }

    private void updateView() {
        if (!isPlaying) return;
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
    }

    @Override
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
        super.onPlaybackStateChanged(state);
        isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        if (!isPlaying) ctx.onPlayerPaused();
        if (!ctx.current_package_name.equals(mCurrent.getPackageName())) {
            if (!isPlaying) return;
            ctx.closeOverlay(new CallBack() {
                @Override
                public void onFinish() {
                    super.onFinish();
                    updateView();
                }
            });
        } else {
            if (!isPlaying) return;
            ctx.closeOverlay(new CallBack() {
                @Override
                public void onFinish() {
                    super.onFinish();
                    updateView();
                }
            });
        }
    }


    @Override
    public void onSessionDestroyed() {
        super.onSessionDestroyed();
        if (mCurrent != null) {
            mCurrent.unregisterCallback(this);
            ctx.callbackMap.remove(mCurrent.getPackageName());
        }
        ctx.shouldRemoveOverlay();

    }
}
