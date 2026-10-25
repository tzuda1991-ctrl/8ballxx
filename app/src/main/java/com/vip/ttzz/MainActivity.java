package com.vip.ttzz;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEnable = findViewById(R.id.btnEnable);
        tvStatus = findViewById(R.id.tvStatus);

        btnEnable.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Turn ON '8Ball Helper' in Accessibility!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                Toast.makeText(this, "Auto-Aim Active! Open 8Ball Pool.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAccessibilityEnabled()) {
            tvStatus.setText("STATUS: AUTO-AIM ACTIVE!\nAb 8Ball Pool open karein.");
            btnEnable.setText("Accessibility Settings (Active)");
        } else {
            tvStatus.setText("STATUS: INACTIVE\nButton daba kar Accessibility ON karein.");
            btnEnable.setText("Enable Auto-Aim Service");
        }
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        return enabledServices.contains(getPackageName());
    }
}
