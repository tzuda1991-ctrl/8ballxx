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
    private AutoAimOverlay overlay;
    private Button statusBtn;
    private View manualTargetHandle;
    private View manualCueHandle;
    private WindowManager.LayoutParams targetParams, cueParams;

    private boolean isRunning = true;
    private boolean isScanning = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private static final int HANDLE_SIZE = 90;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        initOverlay();
        startAutoScanner();
        Toast.makeText(this, "Auto-Aim Engine Started!", Toast.LENGTH_SHORT).show();
    }

    private void initOverlay() {
        if (windowManager != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Full Screen Transparent Laser Canvas (100% Pass-through touch)
        overlay = new AutoAimOverlay(this);
        WindowManager.LayoutParams canvasParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(overlay, canvasParams);

        // 2. Fine-Tune Target Handle (Agar kabhi hath se bhi set karna ho)
        manualTargetHandle = new View(this);
        targetParams = new WindowManager.LayoutParams(
                HANDLE_SIZE, HANDLE_SIZE, overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.LEFT;
        targetParams.x = (int) overlay.targetX - HANDLE_SIZE / 2;
        targetParams.y = (int) overlay.targetY - HANDLE_SIZE / 2;

        manualTargetHandle.setOnTouchListener(new View.OnTouchListener() {
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
                        windowManager.updateViewLayout(manualTargetHandle, targetParams);
                        overlay.targetX = targetParams.x + HANDLE_SIZE / 2f;
                        overlay.targetY = targetParams.y + HANDLE_SIZE / 2f;
                        overlay.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(manualTargetHandle, targetParams);

        // 3. Fine-Tune Cue Handle
        manualCueHandle = new View(this);
        cueParams = new WindowManager.LayoutParams(
                HANDLE_SIZE, HANDLE_SIZE, overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        cueParams.gravity = Gravity.TOP | Gravity.LEFT;
        cueParams.x = (int) overlay.cueX - HANDLE_SIZE / 2;
        cueParams.y = (int) overlay.cueY - HANDLE_SIZE / 2;

        manualCueHandle.setOnTouchListener(new View.OnTouchListener() {
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
                        windowManager.updateViewLayout(manualCueHandle, cueParams);
                        overlay.cueX = cueParams.x + HANDLE_SIZE / 2f;
                        overlay.cueY = cueParams.y + HANDLE_SIZE / 2f;
                        overlay.invalidate();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(manualCueHandle, cueParams);

        // 4. Small Top-Left ON/OFF Toggle Button
        statusBtn = new Button(this);
        statusBtn.setText("AIM: ON");
        statusBtn.setTextSize(9f);
        statusBtn.setTextColor(Color.WHITE);
        statusBtn.setBackgroundColor(Color.argb(200, 0, 180, 70));

        WindowManager.LayoutParams btnParams = new WindowManager.LayoutParams(
                150, 85, overlayType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        btnParams.gravity = Gravity.TOP | Gravity.LEFT;
        btnParams.x = 25;
        btnParams.y = 95;

        statusBtn.setOnClickListener(v -> {
            isRunning = !isRunning;
            int vis = isRunning ? View.VISIBLE : View.GONE;
            overlay.setVisibility(vis);
            manualTargetHandle.setVisibility(vis);
            manualCueHandle.setVisibility(vis);
            statusBtn.setText(isRunning ? "AIM: ON" : "AIM: OFF");
            statusBtn.setBackgroundColor(isRunning ? Color.argb(200, 0, 180, 70) : Color.argb(200, 200, 30, 30));
            if (isRunning) startAutoScanner();
        });
        windowManager.addView(statusBtn, btnParams);
    }

    private void startAutoScanner() {
        if (!isRunning || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        if (isScanning) return;
        isScanning = true;

        // Temporarily hide overlay lines for 15ms so screenshot only sees pure game lines
        overlay.setAlpha(0f);
        mainHandler.postDelayed(() -> {
            takeScreenshot(Display.DEFAULT_DISPLAY, scanExecutor, new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult screenshot) {
                    mainHandler.post(() -> overlay.setAlpha(1f));
                    try {
                        Bitmap hw = Bitmap.wrapHardwareBuffer(screenshot.getHardwareBuffer(), screenshot.getColorSpace());
                        if (hw != null) {
                            Bitmap bmp = hw.copy(Bitmap.Config.ARGB_8888, false);
                            hw.recycle();
                            screenshot.getHardwareBuffer().close();
                            scanTableForAimLine(bmp);
                        }
                    } catch (Exception ignored) {}
                    isScanning = false;
                    if (isRunning) {
                        mainHandler.postDelayed(() -> startAutoScanner(), 320);
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    mainHandler.post(() -> overlay.setAlpha(1f));
                    isScanning = false;
                    if (isRunning) {
                        mainHandler.postDelayed(() -> startAutoScanner(), 500);
                    }
                }
            });
        }, 25);
    }

    // High-Precision Scanner: Detects the game's White Aim Line + Target Ball Impact Stub
    private void scanTableForAimLine(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        // Only scan when phone is in Landscape (or game table bounds)
        int left = (int) (w * 0.176f);
        int right = (int) (w * 0.824f);
        int top = (int) (h * 0.228f);
        int bottom = (int) (h * 0.818f);

        int maxPts = 900;
        float[] wx = new float[maxPts];
        float[] wy = new float[maxPts];
        int count = 0;

        // Step 1: Collect pure white pixels surrounded by dark/black outline (8Ball Pool Aim Line signature)
        for (int y = top + 6; y < bottom - 6 && count < maxPts; y += 3) {
            for (int x = left + 6; x < right - 6 && count < maxPts; x += 3) {
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r > 242 && g > 242 && b > 242) {
                    wx[count] = x;
                    wy[count] = y;
                    count++;
                }
            }
        }

        if (count < 15) {
            bmp.recycle();
            return;
        }

        // Step 2: RANSAC Line Fit for Main Cue-to-Target Line
        int bestInliers = 0;
        float bestX1 = 0, bestY1 = 0, bestX2 = 0, bestY2 = 0;
        float bestUx = 0, bestUy = 0;

        for (int i = 0; i < count; i += 2) {
            for (int j = i + 1; j < count; j += 3) {
                float dx = wx[j] - wx[i];
                float dy = wy[j] - wy[i];
                float dist = (float) Math.hypot(dx, dy);
                if (dist < 90f) continue;

                float ux = dx / dist;
                float uy = dy / dist;
                int inliers = 0;
                float minP = 0, maxP = dist;
                float sx = wx[i], sy = wy[i], ex = wx[j], ey = wy[j];

                for (int k = 0; k < count; k++) {
                    float vx = wx[k] - wx[i];
                    float vy = wy[k] - wy[i];
                    float perp = Math.abs(vx * uy - vy * ux);
                    if (perp < 3.5f) {
                        inliers++;
                        float proj = vx * ux + vy * uy;
                        if (proj < minP) { minP = proj; sx = wx[k]; sy = wy[k]; }
                        if (proj > maxP) { maxP = proj; ex = wx[k]; ey = wy[k]; }
                    }
                }

                if (inliers > bestInliers) {
                    bestInliers = inliers;
                    bestX1 = sx; bestY1 = sy;
                    bestX2 = ex; bestY2 = ey;
                    bestUx = ux; bestUy = uy;
                }
            }
        }

        if (bestInliers < 16) {
            bmp.recycle();
            return;
        }

        // Step 3: Determine which end is the Target Circle (Aim Ring has secondary stub lines branching off it)
        int branch1 = countNearbyNonColinearWhite( wx, wy, count, bestX1, bestY1, bestUx, bestUy);
        int branch2 = countNearbyNonColinearWhite( wx, wy, count, bestX2, bestY2, bestUx, bestUy);

        final float cueX, cueY, targetX, targetY;
        if (branch1 > branch2) {
            targetX = bestX1; targetY = bestY1;
            cueX = bestX2; cueY = bestY2;
        } else {
            targetX = bestX2; targetY = bestY2;
            cueX = bestX1; cueY = bestY1;
        }

        // Step 4: Detect the short Target Ball guideline angle branching from the Target Circle
        float subDx = 0, subDy = 0;
        float bestSubScore = 0;
        for (int angleDeg = 0; angleDeg < 360; angleDeg += 3) {
            double rad = Math.toRadians(angleDeg);
            float dirX = (float) Math.cos(rad);
            float dirY = (float) Math.sin(rad);

            // Ignore the incoming cue line direction
            float mainDx = cueX - targetX;
            float mainDy = cueY - targetY;
            float mainLen = (float) Math.hypot(mainDx, mainDy);
            if (mainLen > 0) {
                float dot = (dirX * mainDx + dirY * mainDy) / mainLen;
                if (dot > 0.75f) continue;
            }

            float score = 0;
            for (int r = 18; r <= 68; r += 3) {
                int px = (int) (targetX + dirX * r);
                int py = (int) (targetY + dirY * r);
                if (px > left && px < right && py > top && py < bottom) {
                    int c = bmp.getPixel(px, py);
                    int red = (c >> 16) & 0xFF;
                    int grn = (c >> 8) & 0xFF;
                    int blu = c & 0xFF;
                    if (red > 230 && grn > 230 && blu > 230) score += 1f;
                }
            }
            if (score > bestSubScore) {
                bestSubScore = score;
                subDx = dirX;
                subDy = dirY;
            }
        }
        bmp.recycle();

        final float finalSubDx = (bestSubScore >= 5) ? subDx : (targetX - cueX);
        final float finalSubDy = (bestSubScore >= 5) ? subDy : (targetY - cueY);

        mainHandler.post(() -> {
            overlay.cueX = cueX;
            overlay.cueY = cueY;
            overlay.targetX = targetX;
            overlay.targetY = targetY;
            overlay.targetDirX = finalSubDx;
            overlay.targetDirY = finalSubDy;

            cueParams.x = (int) cueX - HANDLE_SIZE / 2;
            cueParams.y = (int) cueY - HANDLE_SIZE / 2;
            targetParams.x = (int) targetX - HANDLE_SIZE / 2;
            targetParams.y = (int) targetY - HANDLE_SIZE / 2;
            windowManager.updateViewLayout(manualCueHandle, cueParams);
            windowManager.updateViewLayout(manualTargetHandle, targetParams);
            overlay.invalidate();
        });
    }

    private int countNearbyNonColinearWhite(float[] wx, float[] wy, int count, float cx, float cy, float ux, float uy) {
        int pts = 0;
        for (int i = 0; i < count; i++) {
            float dx = wx[i] - cx;
            float dy = wy[i] - cy;
            float dist = (float) Math.hypot(dx, dy);
            if (dist > 10f && dist < 65f) {
                float perp = Math.abs(dx * uy - dy * ux);
                if (perp > 6f) pts++;
            }
        }
        return pts;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // Pro Laser View
    private static class AutoAimOverlay extends View {
        private final Paint targetRayPaint = new Paint();
        private final Paint pocketLaserPaint = new Paint();
        private final Paint cueDeflectPaint = new Paint();
        private final Paint borderPaint = new Paint();
        private final Paint ringPaint = new Paint();

        float cueX = 950, cueY = 600;
        float targetX = 650, targetY = 500;
        float targetDirX = -1f, targetDirY = -0.3f;
        private final RectF tableRect = new RectF();

        public AutoAimOverlay(Context context) {
            super(context);

            // 1. Extended Target Ball Path (Bright Neon Green/Cyan Solid Line till last pocket/cushion!)
            targetRayPaint.setColor(Color.rgb(0, 255, 200));
            targetRayPaint.setStrokeWidth(5.5f);
            targetRayPaint.setStyle(Paint.Style.STROKE);
            targetRayPaint.setAntiAlias(true);

            // 2. 6-Pocket Helper Dotted Lines (Yellow)
            pocketLaserPaint.setColor(Color.argb(170, 255, 255, 0));
            pocketLaserPaint.setStrokeWidth(3f);
            pocketLaserPaint.setStyle(Paint.Style.STROKE);
            pocketLaserPaint.setAntiAlias(true);
            pocketLaserPaint.setPathEffect(new DashPathEffect(new float[]{14, 10}, 0));

            // 3. White Ball Deflection Line (White Dotted)
            cueDeflectPaint.setColor(Color.WHITE);
            cueDeflectPaint.setStrokeWidth(4f);
            cueDeflectPaint.setStyle(Paint.Style.STROKE);
            cueDeflectPaint.setAntiAlias(true);
            cueDeflectPaint.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));

            // 4. Table Inner Cushion Border
            borderPaint.setColor(Color.argb(100, 0, 255, 100));
            borderPaint.setStrokeWidth(2.5f);
            borderPaint.setStyle(Paint.Style.STROKE);

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(4f);
            ringPaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();

            float left = w * 0.176f;
            float right = w * 0.824f;
            float top = h * 0.228f;
            float bottom = h * 0.818f;
            tableRect.set(left, top, right, bottom);

            canvas.drawRect(tableRect, borderPaint);

            // A. Draw 6 Pocket Guidelines from Target Circle so you always see all pockets
            float midX = (left + right) / 2f;
            canvas.drawLine(targetX, targetY, left, top, pocketLaserPaint);
            canvas.drawLine(targetX, targetY, midX, top, pocketLaserPaint);
            canvas.drawLine(targetX, targetY, right, top, pocketLaserPaint);
            canvas.drawLine(targetX, targetY, left, bottom, pocketLaserPaint);
            canvas.drawLine(targetX, targetY, midX, bottom, pocketLaserPaint);
            canvas.drawLine(targetX, targetY, right, bottom, pocketLaserPaint);

            // B. Draw Extended Target Ball Line (Last tak kahan ja rahi hai with 2 cushion bounces!)
            canvas.drawLine(cueX, cueY, targetX, targetY, targetRayPaint);
            drawExtendedRay(canvas, targetX, targetY, targetDirX, targetDirY, 2);

            // C. Draw 90-Degree White Ball Deflection Line
            float len = (float) Math.hypot(targetDirX, targetDirY);
            if (len > 0.001f) {
                float nx = -targetDirY / len;
                float ny = targetDirX / len;
                canvas.drawLine(targetX - nx * 280f, targetY - ny * 280f, targetX + nx * 280f, targetY + ny * 280f, cueDeflectPaint);
            }

            // D. Draw Lock-On Rings
            ringPaint.setColor(Color.RED);
            canvas.drawCircle(targetX, targetY, 24f, ringPaint);
            ringPaint.setColor(Color.GREEN);
            canvas.drawCircle(cueX, cueY, 24f, ringPaint);
        }

        private void drawExtendedRay(Canvas canvas, float sx, float sy, float dx, float dy, int bounces) {
            if (bounces <= 0 || (Math.abs(dx) < 0.0001f && Math.abs(dy) < 0.0001f)) return;

            float tMin = Float.MAX_VALUE;
            boolean verticalHit = false;

            if (dx < 0) {
                float t = (tableRect.left - sx) / dx;
                if (t > 0.01f && t < tMin) { tMin = t; verticalHit = true; }
            }
            if (dx > 0) {
                float t = (tableRect.right - sx) / dx;
                if (t > 0.01f && t < tMin) { tMin = t; verticalHit = true; }
            }
            if (dy < 0) {
                float t = (tableRect.top - sy) / dy;
                if (t > 0.01f && t < tMin) { tMin = t; verticalHit = false; }
            }
            if (dy > 0) {
                float t = (tableRect.bottom - sy) / dy;
                if (t > 0.01f && t < tMin) { tMin = t; verticalHit = false; }
            }

            if (tMin < Float.MAX_VALUE && tMin < 5000f) {
                float hitX = sx + dx * tMin;
                float hitY = sy + dy * tMin;
                canvas.drawLine(sx, sy, hitX, hitY, targetRayPaint);
                canvas.drawCircle(hitX, hitY, 9f, targetRayPaint);

                drawExtendedRay(canvas, hitX, hitY, verticalHit ? -dx : dx, verticalHit ? dy : -dy, bounces - 1);
            }
        }
    }
}
