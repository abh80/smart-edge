package com.abh80.smartedge;

import android.content.Context;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SongVisualizer extends View {
    Visualizer visualizer;

    public SongVisualizer(Context context) {
        super(context);
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPlayerId(int sessionID) {
        visualizer = new Visualizer(sessionID);
    }
}
