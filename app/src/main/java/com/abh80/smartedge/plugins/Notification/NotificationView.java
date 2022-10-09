package com.abh80.smartedge.plugins.Notification;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class NotificationView extends View {
    public NotificationView(Context context) {
        super(context);
    }

    public NotificationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NotificationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }
}
