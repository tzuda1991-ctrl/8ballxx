package com.vip.ttzz;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AimOverlayView overlayView;
    private boolean isOverlayActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnEnable = findViewById(R.id.btnEnable);
        TextView tvStatus = findViewById(R.id.tvStatus);

        btnEnable.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                startOverlay();
                tvStatus.setText("Service Active. Open 8Ball Pool.");
            }
        });
    }

    private void startOverlay() {
        if (overlayView == null) {
            overlayView = new AimOverlayView(this);
        }
        
        android.view.WindowManager.LayoutParams params = 
            new android.view.WindowManager.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                    android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                    android.view.WindowManager.LayoutParams.TYPE_PHONE,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                -3);
                
        params.alpha = 0.7f;
        
        overlayView.setVisibility(View.VISIBLE);
        getWindowManager().addView(overlayView, params);
        isOverlayActive = true;
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(
            getContentResolver(), 
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        if (enabledServices == null) return false;
        
        String myServiceName = getPackageName() + "/com.vip.ttzz.AimService";
        return enabledServices.contains(myServiceName);
    }

    // Custom View for drawing lines
    private static class AimOverlayView extends View {
        Paint paint = new Paint();

        public AimOverlayView(android.content.Context context) {
            super(context);
            initPaint();
        }

        private void initPaint() {
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Example: Draw a crosshair in the center as a placeholder for aim
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            paint.setColor(Color.argb(150, 255, 0, 0)); // Semi-transparent red
            canvas.drawLine(centerX - 100, centerY, centerX + 100, centerY, paint);
            canvas.drawLine(centerX, centerY - 100, centerX, centerY + 100, paint);
            
            paint.setColor(Color.GREEN);
            canvas.drawCircle(centerX, centerY, 5, paint);
        }
    }
}
