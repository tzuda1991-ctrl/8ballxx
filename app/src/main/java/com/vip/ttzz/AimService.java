package com.vip.ttzz;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.Toast;

public class AimService extends AccessibilityService {

    private WindowManager windowManager;
    private ProAimView aimView;
    private Button menuBtn;
    private Button modeBtn;

    // 2 Floating transparent touch handles taaki baaki poori screen par game touch kaam kare!
    private View cueHandle;
    private View targetHandle;
    private WindowManager.LayoutParams cueParams;
    private WindowManager.LayoutParams targetParams;

    private boolean isVisible = true;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        setupProOverlay();
        Toast.makeText(this, "Pro Aim Line Ready! Rotate phone to Landscape for 8Ball.", Toast.LENGTH_LONG).show();
    }

    private void setupProOverlay() {
        if (windowManager != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int flagOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Drawing Layer (100% Untouchable - Game touch bilkul block nahi karega!)
        aimView = new ProAimView(this);
        WindowManager.LayoutParams drawParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                flagOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(aimView, drawParams);

        // 2. Target Ball Handle (Sirf is chhote circle ko pakad kar ball par rakhna hai)
        int handleSize = 110;
        targetHandle = new View(this);
        targetParams = new WindowManager.LayoutParams(
                handleSize, handleSize, flagOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.LEFT;
        targetParams.x = (int) aimView.ballX - handleSize / 2;
        targetParams.y = (int) aimView.ballY - handleSize / 2;

        targetHandle.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = targetParams.x - event.getRawX();
                        dY = targetParams.y - event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        targetParams.x = (int) (event.getRawX() + dX);
                        targetParams.y = (int) (event.getRawY() + dY);
                        windowManager.updateViewLayout(targetHandle, targetParams);
                        aimView.ballX = targetParams.x + handleSize / 2f;
                        aimView.ballY = targetParams.y + handleSize / 2f;
                        aimView.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(targetHandle, targetParams);

        // 3. Cue Ball Handle (Bank/Bounce shot ke angle ke liye)
        cueHandle = new View(this);
        cueParams = new WindowManager.LayoutParams(
                handleSize, handleSize, flagOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        cueParams.gravity = Gravity.TOP | Gravity.LEFT;
        cueParams.x = (int) aimView.cueX - handleSize / 2;
        cueParams.y = (int) aimView.cueY - handleSize / 2;

        cueHandle.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = cueParams.x - event.getRawX();
                        dY = cueParams.y - event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        cueParams.x = (int) (event.getRawX() + dX);
                        cueParams.y = (int) (event.getRawY() + dY);
                        windowManager.updateViewLayout(cueHandle, cueParams);
                        aimView.cueX = cueParams.x + handleSize / 2f;
                        aimView.cueY = cueParams.y + handleSize / 2f;
                        aimView.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(cueHandle, cueParams);

        // 4. Control Buttons (Top Corner)
        menuBtn = new Button(this);
        menuBtn.setText("ON");
        menuBtn.setTextSize(10f);
        menuBtn.setTextColor(Color.WHITE);
        menuBtn.setBackgroundColor(Color.argb(210, 0, 180, 0));

        WindowManager.LayoutParams btn1Params = new WindowManager.LayoutParams(
                130, 95, flagOverlay, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        btn1Params.gravity = Gravity.TOP | Gravity.LEFT;
        btn1Params.x = 20;
        btn1Params.y = 80;

        menuBtn.setOnClickListener(v -> {
            isVisible = !isVisible;
            int vis = isVisible ? View.VISIBLE : View.GONE;
            aimView.setVisibility(vis);
            targetHandle.setVisibility(vis);
            cueHandle.setVisibility(vis);
            modeBtn.setVisibility(vis);
            menuBtn.setText(isVisible ? "ON" : "OFF");
            menuBtn.setBackgroundColor(isVisible ? Color.argb(210, 0, 180, 0) : Color.argb(210, 200, 0, 0));
        });
        windowManager.addView(menuBtn, btn1Params);

        // Mode Button (6-Pocket Lines + Bounce toggle)
        modeBtn = new Button(this);
        modeBtn.setText("3-LINE");
        modeBtn.setTextSize(9f);
        modeBtn.setTextColor(Color.BLACK);
        modeBtn.setBackgroundColor(Color.argb(220, 255, 215, 0));

        WindowManager.LayoutParams btn2Params = new WindowManager.LayoutParams(
                150, 95, flagOverlay, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        btn2Params.gravity = Gravity.TOP | Gravity.LEFT;
        btn2Params.x = 160;
        btn2Params.y = 80;

        modeBtn.setOnClickListener(v -> {
            aimView.showPocketLines = !aimView.showPocketLines;
            modeBtn.setText(aimView.showPocketLines ? "ALL" : "BOUNCE");
            aimView.invalidate();
        });
        windowManager.addView(modeBtn, btn2Params);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // Full Physics & Pocket Guideline View
    private static class ProAimView extends View {
        private final Paint pocketLinePaint = new Paint();
        private final Paint bounceLinePaint = new Paint();
        private final Paint tableBorderPaint = new Paint();
        private final Paint handlePaint = new Paint();

        float cueX = 450, cueY = 500;
        float ballX = 750, ballY = 500;
        boolean showPocketLines = true;

        private final RectF tableRect = new RectF();

        public ProAimView(Context context) {
            super(context);

            // 6-Pocket Laser Lines (Yellow Dotted)
            pocketLinePaint.setColor(Color.argb(190, 255, 255, 0));
            pocketLinePaint.setStrokeWidth(3.5f);
            pocketLinePaint.setStyle(Paint.Style.STROKE);
            pocketLinePaint.setAntiAlias(true);
            pocketLinePaint.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));

            // Cushion Bounce Line (Bright Cyan/White Solid Line)
            bounceLinePaint.setColor(Color.CYAN);
            bounceLinePaint.setStrokeWidth(5f);
            bounceLinePaint.setStyle(Paint.Style.STROKE);
            bounceLinePaint.setAntiAlias(true);

            // Pool Table Inner Cushion Boundary
            tableBorderPaint.setColor(Color.argb(130, 0, 255, 100));
            tableBorderPaint.setStrokeWidth(3f);
            tableBorderPaint.setStyle(Paint.Style.STROKE);

            handlePaint.setStyle(Paint.Style.STROKE);
            handlePaint.setStrokeWidth(5f);
            handlePaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();

            // Standard 8-Ball Pool Table Cushion Area (Auto-adjusts to Landscape/Portrait)
            float left = w * 0.135f;
            float right = w * 0.865f;
            float top = h * 0.19f;
            float bottom = h * 0.85f;
            tableRect.set(left, top, right, bottom);

            // Draw Table Frame
            canvas.drawRect(tableRect, tableBorderPaint);

            // 1. Draw 6-Pocket Auto-Lines from Target Ball (Ball se charo kono aur beech ke pockets tak seedhi line)
            if (showPocketLines) {
                float midX = (left + right) / 2f;
                // Top 3 pockets
                canvas.drawLine(ballX, ballY, left, top, pocketLinePaint);
                canvas.drawLine(ballX, ballY, midX, top, pocketLinePaint);
                canvas.drawLine(ballX, ballY, right, top, pocketLinePaint);
                // Bottom 3 pockets
                canvas.drawLine(ballX, ballY, left, bottom, pocketLinePaint);
                canvas.drawLine(ballX, ballY, midX, bottom, pocketLinePaint);
                canvas.drawLine(ballX, ballY, right, bottom, pocketLinePaint);
            }

            // 2. Draw Cue-to-Ball & 3-Cushion Bounce Ray (Ball last tak kaha jayegi!)
            canvas.drawLine(cueX, cueY, ballX, ballY, bounceLinePaint);
            drawRayWithBounces(canvas, ballX, ballY, ballX - cueX, ballY - cueY, 3);

            // 3. Draw Target Circle (Red) & Cue Circle (Green)
            handlePaint.setColor(Color.RED);
            canvas.drawCircle(ballX, ballY, 38f, handlePaint);
            canvas.drawCircle(ballX, ballY, 5f, handlePaint);

            handlePaint.setColor(Color.GREEN);
            canvas.drawCircle(cueX, cueY, 38f, handlePaint);
            canvas.drawCircle(cueX, cueY, 5f, handlePaint);
        }

        // Exact Physics Ray-Tracing for Cushion Reflections (Bank Shots)
        private void drawRayWithBounces(Canvas canvas, float startX, float startY, float dirX, float dirY, int bouncesLeft) {
            if (bouncesLeft <= 0 || (Math.abs(dirX) < 0.001f && Math.abs(dirY) < 0.001f)) return;

            float tMin = Float.MAX_VALUE;
            boolean hitVertical = false;

            // Left Cushion
            if (dirX < 0) {
                float t = (tableRect.left - startX) / dirX;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = true; }
            }
            // Right Cushion
            if (dirX > 0) {
                float t = (tableRect.right - startX) / dirX;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = true; }
            }
            // Top Cushion
            if (dirY < 0) {
                float t = (tableRect.top - startY) / dirY;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = false; }
            }
            // Bottom Cushion
            if (dirY > 0) {
                float t = (tableRect.bottom - startY) / dirY;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = false; }
            }

            if (tMin < Float.MAX_VALUE && tMin < 5000f) {
                float hitX = startX + dirX * tMin;
                float hitY = startY + dirY * tMin;

                canvas.drawLine(startX, startY, hitX, hitY, bounceLinePaint);
                canvas.drawCircle(hitX, hitY, 10f, bounceLinePaint); // Cushion impact point

                // Reflect direction for next cushion bounce
                float nextDirX = hitVertical ? -dirX : dirX;
                float nextDirY = hitVertical ? dirY : -dirY;

                drawRayWithBounces(canvas, hitX, hitY, nextDirX, nextDirY, bouncesLeft - 1);
            }
        }
    }
}
