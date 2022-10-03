package com.abh80.smartedge.plugins.MediaSession;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SongVisualizer extends View {
    Visualizer visualizer;

    public SongVisualizer(Context context) {
        super(context);
        init();
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private byte[] bytes;

    public void setPlayerId(int sessionID) {
        try {
            if (visualizer != null) {
                release();
                visualizer = null;
            }
            visualizer = new Visualizer(sessionID);
            visualizer.setEnabled(false);
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                                  int samplingRate) {
                    SongVisualizer.this.bytes = bytes;
                    invalidate();
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                                             int samplingRate) {
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);


            visualizer.setEnabled(true);
        } catch (Exception e) {
            // do nothing lol
        }
    }

    public void release() {
        //will be null if setPlayer hasn't yet been called
        if (visualizer == null)
            return;

        visualizer.release();
        bytes = null;
        invalidate();
    }

    private void init() {
        setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setColor(int Color) {
        paint.setColor(Color);
    }

    private final Paint paint = new Paint();

    @Override
    protected void onDraw(Canvas canvas) {
        float density = 3;

        if (bytes != null) {
            float barWidth = getWidth() / density;
            float div = bytes.length / density;
            paint.setStrokeWidth(barWidth - 4);

            for (int i = 0; i < density; i++) {
                int bytePosition = (int) Math.ceil(i * div);
                float barX = (i * barWidth) + (barWidth / 2);
                if (bytes[bytePosition] == 0 || bytes[bytePosition] + 128 == 0) {
                    canvas.drawLine(barX, (getHeight() / 2f), barX, (getHeight() / 2f), paint);
                } else {
                    int top = (getHeight() - 20) +
                            ((byte) (Math.abs(bytes[bytePosition]) + 128)) * (getHeight() - 20) / 128;
                    canvas.drawLine(barX, ((getHeight() + 20) - top) / 2f, barX, top, paint);
                }
            }
            super.onDraw(canvas);
        }
    }
}
