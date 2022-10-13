package com.abh80.smartedge.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class BatteryImageView extends View {
    public BatteryImageView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint() {
            {
                setAntiAlias(true);
                setColor(Color.GREEN);
                setStrokeWidth(8);
                setStyle(Style.STROKE);
                setStrokeJoin(Join.MITER);
                setDither(true);
            }
        };
    }

    public BatteryImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    Paint paint;
    private float batterPercent;

    public void updateBatteryPercent(float p) {
        batterPercent = p;
        invalidate();
    }

    public void setStrokeColor(int color) {
        if (paint != null) paint.setColor(color);
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (paint == null) {
            init();
        }
        float margin = 5;
        RectF rectangle = new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
        canvas.drawArc(rectangle, 270f, (batterPercent / 100f) * 360f, false, paint);
    }
}
