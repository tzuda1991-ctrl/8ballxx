package com.vip.ttzz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AimOverlayView overlayView;
    private WindowManager windowManager;
    private boolean isOverlayActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnEnable = findViewById(R.id.btnEnable);
        TextView tvStatus = findViewById(R.id.tvStatus);

        btnEnable.setOnClickListener(v -> {
            // Step 1: Pehle Overlay ("Display over other apps") Permission check karein
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            // Step 2: Phir Accessibility Service check karein (Optional, agar zaroorat ho)
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Please enable 8Ball Helper in Accessibility", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }

            // Step 3: Dono permissions milne par Overlay start/stop karein
            if (!isOverlayActive) {
                startOverlay();
                tvStatus.setText("Service Active! Ab aap app minimize kar sakte hain.");
                btnEnable.setText("Stop Overlay");
            } else {
                stopOverlay();
                tvStatus.setText("Overlay Stopped.");
                btnEnable.setText("Enable Accessibility & Start Overlay");
            }
        });
    }

    private void startOverlay() {
        if (isOverlayActive) return;

        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (overlayView == null) {
            overlayView = new AimOverlayView(getApplicationContext());
        }

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // FLAG_NOT_TOUCHABLE zaroori hai taaki overlay ke aar-paar screen touch kaam kare
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        try {
            windowManager.addView(overlayView, params);
            isOverlayActive = true;
            Toast.makeText(this, "Overlay Started!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error starting overlay: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopOverlay() {
        if (isOverlayActive && windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
            isOverlayActive = false;
        }
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServices == null) return false;

        String fullServiceName = getPackageName() + "/" + getPackageName() + ".AimService";
        String shortServiceName = getPackageName() + "/.AimService";
        return enabledServices.contains(fullServiceName) || enabledServices.contains(shortServiceName);
    }

    // Custom View for drawing lines on screen
    private static class AimOverlayView extends View {
        private final Paint paint = new Paint();

        public AimOverlayView(Context context) {
            super(context);
            paint.setStrokeWidth(6f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            // Red guidelines + crosshair
            paint.setColor(Color.argb(180, 255, 0, 0));
            canvas.drawLine(centerX - 150, centerY, centerX + 150, centerY, paint);
            canvas.drawLine(centerX, centerY - 150, centerX, centerY + 150, paint);

            // Outer circle
            canvas.drawCircle(centerX, centerY, 80, paint);

            // Green center dot
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.GREEN);
            canvas.drawCircle(centerX, centerY, 8, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
    }
}
