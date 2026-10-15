package com.wusper.android.human.detection;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class HumanDetectionAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO: 可在此取得視窗內容，輔助人型定位
        // 低功耗路徑可考慮結合此事件觸發偵測
    }

    @Override
    public void onInterrupt() {
    }
}
