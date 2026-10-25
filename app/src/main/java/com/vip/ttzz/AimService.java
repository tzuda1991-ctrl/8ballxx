package com.vip.ttzz;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AimService extends AccessibilityService {

    private WindowManager windowManager;
    private ProAimView aimView;
    private Button toggleBtn;
    private Button autoBtn;

    private View cueHandle;
    private View targetHandle;
    private WindowManager.LayoutParams cueParams;
    private WindowManager.LayoutParams targetParams;

    private boolean isVisible = true;
    private boolean isAutoScan = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService scannerPool = Executors.newSingleThreadExecutor();
    private static final int HANDLE_SIZE = 100;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        setupOverlay();
    }

    private void setupOverlay() {
        if (windowManager != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int flagOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Drawing Layer (Untouchable - Game touch chalta rahega)
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

        // 2. Target Ball Circle Handle (Red)
        targetHandle = new View(this);
        targetParams = new WindowManager.LayoutParams(
                HANDLE_SIZE, HANDLE_SIZE, flagOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.LEFT;
        targetParams.x = (int) aimView.ballX - HANDLE_SIZE / 2;
        targetParams.y = (int) aimView.ballY - HANDLE_SIZE / 2;

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
                        aimView.ballX = targetParams.x + HANDLE_SIZE / 2f;
                        aimView.ballY = targetParams.y + HANDLE_SIZE / 2f;
                        aimView.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(targetHandle, targetParams);

        // 3. Cue Ball Circle Handle (Green)
        cueHandle = new View(this);
        cueParams = new WindowManager.LayoutParams(
                HANDLE_SIZE, HANDLE_SIZE, flagOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        cueParams.gravity = Gravity.TOP | Gravity.LEFT;
        cueParams.x = (int) aimView.cueX - HANDLE_SIZE / 2;
        cueParams.y = (int) aimView.cueY - HANDLE_SIZE / 2;

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
                        aimView.cueX = cueParams.x + HANDLE_SIZE / 2f;
                        aimView.cueY = cueParams.y + HANDLE_SIZE / 2f;
                        aimView.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(cueHandle, cueParams);

        // 4. ON/OFF Button
        toggleBtn = new Button(this);
        toggleBtn.setText("ON");
        toggleBtn.setTextSize(10f);
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setBackgroundColor(Color.argb(210, 0, 180, 0));

        WindowManager.LayoutParams btn1Params = new WindowManager.LayoutParams(
                130, 95, flagOverlay, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        btn1Params.gravity = Gravity.TOP | Gravity.LEFT;
        btn1Params.x = 20;
        btn1Params.y = 80;

        toggleBtn.setOnClickListener(v -> {
            isVisible = !isVisible;
            int vis = isVisible ? View.VISIBLE : View.GONE;
            aimView.setVisibility(vis);
            targetHandle.setVisibility(vis);
            cueHandle.setVisibility(vis);
            autoBtn.setVisibility(vis);
            toggleBtn.setText(isVisible ? "ON" : "OFF");
            toggleBtn.setBackgroundColor(isVisible ? Color.argb(210, 0, 180, 0) : Color.argb(210, 200, 0, 0));
        });
        windowManager.addView(toggleBtn, btn1Params);

        // 5. AUTO DETECT Button (Scans Screen for White Aim Line & Cue Ball)
        autoBtn = new Button(this);
        autoBtn.setText("AUTO: OFF");
        autoBtn.setTextSize(9f);
        autoBtn.setTextColor(Color.BLACK);
        autoBtn.setBackgroundColor(Color.argb(220, 255, 215, 0));

        WindowManager.LayoutParams btn2Params = new WindowManager.LayoutParams(
                180, 95, flagOverlay, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        btn2Params.gravity = Gravity.TOP | Gravity.LEFT;
        btn2Params.x = 160;
        btn2Params.y = 80;

        autoBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Toast.makeText(this, "Auto-Detect requires Android 11+", Toast.LENGTH_SHORT).show();
                return;
            }
            isAutoScan = !isAutoScan;
            autoBtn.setText(isAutoScan ? "AUTO: ON" : "AUTO: OFF");
            autoBtn.setBackgroundColor(isAutoScan ? Color.argb(220, 0, 255, 255) : Color.argb(220, 255, 215, 0));
            if (isAutoScan) {
                runAutoScanLoop();
            }
        });
        windowManager.addView(autoBtn, btn2Params);
    }

    // Auto-Scan loop using Android 11+ Accessibility takeScreenshot
    private void runAutoScanLoop() {
        if (!isAutoScan || !isVisible || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;

        takeScreenshot(Display.DEFAULT_DISPLAY, scannerPool, new TakeScreenshotCallback() {
            @Override
            public void onSuccess(ScreenshotResult screenshot) {
                try {
                    Bitmap hwBitmap = Bitmap.wrapHardwareBuffer(screenshot.getHardwareBuffer(), screenshot.getColorSpace());
                    if (hwBitmap != null) {
                        Bitmap bmp = hwBitmap.copy(Bitmap.Config.ARGB_8888, false);
                        hwBitmap.recycle();
                        screenshot.getHardwareBuffer().close();
                        detectWhiteLineEndpoints(bmp);
                    }
                } catch (Exception ignored) {}
                handler.postDelayed(() -> runAutoScanLoop(), 450);
            }

            @Override
            public void onFailure(int errorCode) {
                handler.postDelayed(() -> runAutoScanLoop(), 600);
            }
        });
    }

    // Finds the pure white game aim line (R>245, G>245, B>245) inside the green pool table
    private void detectWhiteLineEndpoints(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int left = (int) (w * 0.175f);
        int right = (int) (w * 0.825f);
        int top = (int) (h * 0.22f);
        int bottom = (int) (h * 0.82f);

        // Hough-style line voting along directions from current cue ball or full table
        float bestScore = 0;
        float foundCueX = -1, foundCueY = -1, foundTargetX = -1, foundTargetY = -1;

        // Collect bright white pixels inside table
        int maxPts = 600;
        float[] px = new float[maxPts];
        float[] py = new float[maxPts];
        int count = 0;

        for (int y = top; y < bottom && count < maxPts; y += 4) {
            for (int x = left; x < right && count < maxPts; x += 4) {
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                // Pure bright white pixel of 8Ball Pool aim line
                if (r > 246 && g > 246 && b > 246) {
                    px[count] = x;
                    py[count] = y;
                    count++;
                }
            }
        }
        bmp.recycle();

        // Find longest colinear chain of white pixels (the game's white aim line)
        if (count > 12) {
            for (int i = 0; i < count; i += 2) {
                for (int j = i + 1; j < count; j += 3) {
                    float dx = px[j] - px[i];
                    float dy = py[j] - py[i];
                    float len = (float) Math.hypot(dx, dy);
                    if (len < 120f) continue;

                    float ux = dx / len;
                    float uy = dy / len;
                    int inliers = 0;
                    float minProj = 0, maxProj = len;
                    float startX = px[i], startY = py[i], endX = px[j], endY = py[j];

                    for (int k = 0; k < count; k++) {
                        float vx = px[k] - px[i];
                        float vy = py[k] - py[i];
                        float dist = Math.abs(vx * uy - vy * ux);
                        if (dist < 4.5f) {
                            inliers++;
                            float proj = vx * ux + vy * uy;
                            if (proj < minProj) { minProj = proj; startX = px[k]; startY = py[k]; }
                            if (proj > maxProj) { maxProj = proj; endX = px[k]; endY = py[k]; }
                        }
                    }

                    if (inliers > bestScore) {
                        bestScore = inliers;
                        foundCueX = startX;
                        foundCueY = startY;
                        foundTargetX = endX;
                        foundTargetY = endY;
                    }
                }
            }
        }

        if (bestScore > 18 && foundCueX > 0) {
            // Keep cue handle closest to previous cue position so direction doesn't flip
            float d1 = (float) Math.hypot(foundCueX - aimView.cueX, foundCueY - aimView.cueY);
            float d2 = (float) Math.hypot(foundTargetX - aimView.cueX, foundTargetY - aimView.cueY);
            final float cx = (d1 <= d2) ? foundCueX : foundTargetX;
            final float cy = (d1 <= d2) ? foundCueY : foundTargetY;
            final float tx = (d1 <= d2) ? foundTargetX : foundCueX;
            final float ty = (d1 <= d2) ? foundTargetY : foundCueY;

            handler.post(() -> {
                aimView.cueX = cx;
                aimView.cueY = cy;
                aimView.ballX = tx;
                aimView.ballY = ty;
                cueParams.x = (int) cx - HANDLE_SIZE / 2;
                cueParams.y = (int) cy - HANDLE_SIZE / 2;
                targetParams.x = (int) tx - HANDLE_SIZE / 2;
                targetParams.y = (int) ty - HANDLE_SIZE / 2;
                windowManager.updateViewLayout(cueHandle, cueParams);
                windowManager.updateViewLayout(targetHandle, targetParams);
                aimView.invalidate();
            });
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // Calibrated View matching exact screenshot proportions
    private static class ProAimView extends View {
        private final Paint pocketLinePaint = new Paint();
        private final Paint mainLinePaint = new Paint();
        private final Paint deflectPaint = new Paint();
        private final Paint tableBorderPaint = new Paint();
        private final Paint handlePaint = new Paint();

        float cueX = 1000, cueY = 650;
        float ballX = 600, ballY = 300;
        private final RectF tableRect = new RectF();

        public ProAimView(Context context) {
            super(context);

            // 6-Pocket Guidelines (Thin Yellow Dotted)
            pocketLinePaint.setColor(Color.argb(180, 255, 255, 0));
            pocketLinePaint.setStrokeWidth(3f);
            pocketLinePaint.setStyle(Paint.Style.STROKE);
            pocketLinePaint.setAntiAlias(true);
            pocketLinePaint.setPathEffect(new DashPathEffect(new float[]{12, 10}, 0));

            // Main Extended Aim + Cushion Bounce Line (Bright White/Cyan)
            mainLinePaint.setColor(Color.CYAN);
            mainLinePaint.setStrokeWidth(5f);
            mainLinePaint.setStyle(Paint.Style.STROKE);
            mainLinePaint.setAntiAlias(true);

            // 90-Degree Cue Ball Deflection Line (Green Dotted)
            deflectPaint.setColor(Color.GREEN);
            deflectPaint.setStrokeWidth(4f);
            deflectPaint.setStyle(Paint.Style.STROKE);
            deflectPaint.setAntiAlias(true);
            deflectPaint.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));

            // Calibrated Table Cushion Border
            tableBorderPaint.setColor(Color.argb(120, 0, 255, 100));
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

            // Exact Inner Cushion Bounds calibrated from your screenshot!
            float left = w * 0.176f;
            float right = w * 0.824f;
            float top = h * 0.228f;
            float bottom = h * 0.818f;
            tableRect.set(left, top, right, bottom);

            // 1. Draw Inner Cushion Box
            canvas.drawRect(tableRect, tableBorderPaint);

            // 2. Draw 6-Pocket Auto Lines from Red Target Circle to all 6 Pockets!
            float midX = (left + right) / 2f;
            canvas.drawLine(ballX, ballY, left, top, pocketLinePaint);
            canvas.drawLine(ballX, ballY, midX, top, pocketLinePaint);
            canvas.drawLine(ballX, ballY, right, top, pocketLinePaint);
            canvas.drawLine(ballX, ballY, left, bottom, pocketLinePaint);
            canvas.drawLine(ballX, ballY, midX, bottom, pocketLinePaint);
            canvas.drawLine(ballX, ballY, right, bottom, pocketLinePaint);

            // 3. Draw Extended Shot Line + 2 Cushion Bounces
            float dx = ballX - cueX;
            float dy = ballY - cueY;
            canvas.drawLine(cueX, cueY, ballX, ballY, mainLinePaint);
            drawRayWithBounces(canvas, ballX, ballY, dx, dy, 2);

            // 4. Draw 90-Degree Tangent Deflection Line (White ball takrane ke baad kahan jayegi)
            float len = (float) Math.hypot(dx, dy);
            if (len > 1f) {
                float nx = -dy / len;
                float ny = dx / len;
                canvas.drawLine(ballX - nx * 220f, ballY - ny * 220f, ballX + nx * 220f, ballY + ny * 220f, deflectPaint);
            }

            // 5. Draw Handles
            handlePaint.setColor(Color.RED);
            canvas.drawCircle(ballX, ballY, 26f, handlePaint);
            canvas.drawCircle(ballX, ballY, 4f, handlePaint);

            handlePaint.setColor(Color.GREEN);
            canvas.drawCircle(cueX, cueY, 26f, handlePaint);
            canvas.drawCircle(cueX, cueY, 4f, handlePaint);
        }

        private void drawRayWithBounces(Canvas canvas, float startX, float startY, float dirX, float dirY, int bouncesLeft) {
            if (bouncesLeft <= 0 || (Math.abs(dirX) < 0.001f && Math.abs(dirY) < 0.001f)) return;

            float tMin = Float.MAX_VALUE;
            boolean hitVertical = false;

            if (dirX < 0) {
                float t = (tableRect.left - startX) / dirX;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = true; }
            }
            if (dirX > 0) {
                float t = (tableRect.right - startX) / dirX;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = true; }
            }
            if (dirY < 0) {
                float t = (tableRect.top - startY) / dirY;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = false; }
            }
            if (dirY > 0) {
                float t = (tableRect.bottom - startY) / dirY;
                if (t > 0.01f && t < tMin) { tMin = t; hitVertical = false; }
            }

            if (tMin < Float.MAX_VALUE && tMin < 5000f) {
                float hitX = startX + dirX * tMin;
                float hitY = startY + dirY * tMin;

                canvas.drawLine(startX, startY, hitX, hitY, mainLinePaint);
                canvas.drawCircle(hitX, hitY, 8f, mainLinePaint);

                float nextDirX = hitVertical ? -dirX : dirX;
                float nextDirY = hitVertical ? dirY : -dirY;

                drawRayWithBounces(canvas, hitX, hitY, nextDirX, nextDirY, bouncesLeft - 1);
            }
        }
    }
}
