package com.vip.ttzz;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class AimService extends AccessibilityService {

    private WindowManager windowManager;
    private AimView aimView;
    private Button toggleButton;
    private boolean isAimVisible = true;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        showFloatingOverlay();
        Toast.makeText(this, "8Ball Helper Overlay Started!", Toast.LENGTH_SHORT).show();
    }

    private void showFloatingOverlay() {
        if (windowManager != null) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Full-screen Aim Line View
        aimView = new AimView(this);
        WindowManager.LayoutParams aimParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        // 2. Floating Toggle Button (On/Off karne ke liye)
        toggleButton = new Button(this);
        toggleButton.setText("HIDE");
        toggleButton.setTextSize(10f);
        toggleButton.setTextColor(Color.WHITE);
        toggleButton.setBackgroundColor(Color.argb(200, 220, 20, 60));

        WindowManager.LayoutParams btnParams = new WindowManager.LayoutParams(
                160,
                110,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = 50;
        btnParams.y = 200;

        toggleButton.setOnClickListener(v -> {
            if (isAimVisible) {
                aimView.setVisibility(View.GONE);
                toggleButton.setText("SHOW");
                toggleButton.setBackgroundColor(Color.argb(200, 34, 139, 34));
                isAimVisible = false;
            } else {
                aimView.setVisibility(View.VISIBLE);
                toggleButton.setText("HIDE");
                toggleButton.setBackgroundColor(Color.argb(200, 220, 20, 60));
                isAimVisible = true;
            }
        });

        try {
            windowManager.addView(aimView, aimParams);
            windowManager.addView(toggleButton, btnParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed for manual overlay
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null) {
            if (aimView != null) windowManager.removeView(aimView);
            if (toggleButton != null) windowManager.removeView(toggleButton);
        }
    }

    // Custom View jismein 2 draggable circles (Pocket & Cue Ball) aur guideline hain
    private static class AimView extends View {
        private final Paint linePaint = new Paint();
        private final Paint circlePaint = new Paint();
        private float startX = 400, startY = 400; // Circle 1 (Cue/Target)
        private float endX = 800, endY = 400;     // Circle 2 (Pocket)
        private int activePointer = 0; // 0 = none, 1 = start circle, 2 = end circle
        private static final float TOUCH_RADIUS = 70f;

        public AimView(android.content.Context context) {
            super(context);

            linePaint.setColor(Color.WHITE);
            linePaint.setStrokeWidth(5f);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setAntiAlias(true);
            linePaint.setPathEffect(new DashPathEffect(new float[]{20, 15}, 0));

            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(6f);
            circlePaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Draw main aim line
            canvas.drawLine(startX, startY, endX, endY, linePaint);

            // Draw extended bounce/bank helper lines from target circle
            float dx = endX - startX;
            float dy = endY - startY;
            canvas.drawLine(endX, endY, endX + dx * 0.5f, endY - dy * 0.5f, linePaint);

            // Draw Start Circle (Green - Drag this to ball)
            circlePaint.setColor(Color.GREEN);
            canvas.drawCircle(startX, startY, 45f, circlePaint);
            canvas.drawCircle(startX, startY, 6f, circlePaint);

            // Draw End Circle (Red - Drag this to pocket/target)
            circlePaint.setColor(Color.RED);
            canvas.drawCircle(endX, endY, 45f, circlePaint);
            canvas.drawCircle(endX, endY, 6f, circlePaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (Math.hypot(x - startX, y - startY) <= TOUCH_RADIUS) {
                        activePointer = 1;
                        return true;
                    } else if (Math.hypot(x - endX, y - endY) <= TOUCH_RADIUS) {
                        activePointer = 2;
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_MOVE:
                    if (activePointer == 1) {
                        startX = x;
                        startY = y;
                        invalidate();
                        return true;
                    } else if (activePointer == 2) {
                        endX = x;
                        endY = y;
                        invalidate();
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activePointer = 0;
                    return false;
            }
            return false;
        }
    }
}
