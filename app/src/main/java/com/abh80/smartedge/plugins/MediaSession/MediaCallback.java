package com.abh80.smartedge.plugins.MediaSession;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;

import androidx.annotation.Nullable;

import com.abh80.smartedge.utils.CallBack;

public class MediaCallback extends MediaController.Callback {
    public MediaCallback(MediaController mCurrent, MediaSessionPlugin context) {
        this.mCurrent = mCurrent;
        this.ctx = context;
        try {
            isPlaying = mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
            mediaMetadata = mCurrent.getMetadata();
            updateView();
        } catch (Exception e) {
            // do nothing lol
        }
    }

    private final MediaSessionPlugin ctx;
    private final MediaController mCurrent;
    private MediaMetadata mediaMetadata;
    private boolean isPlaying = true;

    private void updateView() {
        if (!isPlaying) return;
        if (mCurrent.getMetadata() == null) return;
        Bitmap b = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) {
            return;
        }
        String title = mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
        String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        ctx.queueUpdate(new UpdateQueueStruct(artist, title, b));
        ctx.openOverlay(mCurrent.getPackageName());
        ctx.mCurrent = mCurrent;
        ctx.onPlayerResume(false);
    }


    @Override
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
        super.onPlaybackStateChanged(state);
        try {
            if (state == null || mCurrent.getMetadata() == null) return;
            MediaMetadata targetMetada = mCurrent.getMetadata();
            boolean isPlaying2 = state.getState() == PlaybackState.STATE_PLAYING;
            if (mediaMetadata != null && mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) != null
                    && mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE).equals(targetMetada.getString(MediaMetadata.METADATA_KEY_TITLE)) && ctx.overlayOpen()) {
                if (ctx.mCurrent != null && ctx.mCurrent.getPackageName().equals(mCurrent.getPackageName())) {
                    if (!isPlaying2) ctx.onPlayerPaused(true);
                    else ctx.onPlayerResume(true);
                }
                isPlaying = isPlaying2;
                return;
            }

            isPlaying = isPlaying2;
            if (ctx.mCurrent != null && ctx.mCurrent.getPackageName().equals(mCurrent.getPackageName())) {
                if (!isPlaying) ctx.onPlayerPaused(false);
                else ctx.onPlayerResume(false);
            }
            if (!isPlaying) return;
            mediaMetadata = targetMetada;
            ctx.mCurrent = mCurrent;
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
        } catch (Exception e) {
            e.printStackTrace();
            ctx.closeOverlay();
        }
    }

    @Override
    public void onSessionDestroyed() {
        super.onSessionDestroyed();
        if (mCurrent != null) {
            mCurrent.unregisterCallback(this);
            ctx.callbackMap.remove(mCurrent.getPackageName());
            ctx.mCurrent = null;
        }
        ctx.closeOverlay();

    }
}
