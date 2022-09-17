package com.abh80.smartedge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        updateView();
    }

    private void updateView() {
        if (!isPlaying) return;
        Bitmap b = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) {
            return;
        }
        ShapeableImageView imageView = mView.findViewById(R.id.cover);
        imageView.setImageBitmap(b);
        if (mView.findViewById(R.id.cover).getWidth() == 0) {
            ValueAnimator animator = ValueAnimator.ofInt(0, dpToInt(25));
            animator.addUpdateListener(v -> {
                ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                layoutParams.width = (int) v.getAnimatedValue();
                layoutParams.height = (int) v.getAnimatedValue();
                imageView.setLayoutParams(layoutParams);
            });
            animator.setDuration(200);
            animator.start();
        }
        String title = mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
        TextView titleView = mView.findViewById(R.id.title);
        titleView.setText(title);
    }

    @Override
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
        super.onPlaybackStateChanged(state);
        isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        if (!isPlaying) ctx.onPlayerPaused();
        updateView();
    }

    private int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }

    @Override
    public void onSessionDestroyed() {
        super.onSessionDestroyed();
        if (mCurrent != null) mCurrent.unregisterCallback(this);
        ctx.shouldRemoveOverlay();
    }
}
