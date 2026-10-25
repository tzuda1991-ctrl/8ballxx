package com.vip.ttzz;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class AimService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Logic to detect balls can be added here
        // For now, it just stays active
    }

    @Override
    public void onInterrupt() {}
}
