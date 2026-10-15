package com.wusper.android.human.detection;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 低功耗偵測管理器。
 * 目前為 stub，實際可替換為 ML Kit Object Detection 或 TFLite person 模型。
 * 建議幀率 5-10 FPS 以控制功耗。
 */
public class DetectionManager {

    public interface DetectionListener {
        void onDetections(List<Rect> boxes);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private DetectionListener listener;
    private boolean running = false;
    private final long intervalMs = 150; // ~6-7 FPS

    public void setListener(DetectionListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        scheduleNext();
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scheduleNext() {
        if (!running) return;
        handler.postDelayed(() -> {
            if (!running) return;
            List<Rect> boxes = detectPersons();
            if (listener != null) {
                listener.onDetections(boxes);
            }
            scheduleNext();
        }, intervalMs);
    }

    /**
     * TODO: 替換為真實偵測。
     * 可使用 MediaProjection 擷取畫面後送入 ML Kit，
     * 或結合 AccessibilityService 取得視窗資訊輔助。
     */
    private List<Rect> detectPersons() {
        List<Rect> result = new ArrayList<>();
        // Stub: 隨機產生 0-2 個框，方便測試畫框流程
        int count = random.nextInt(3);
        for (int i = 0; i < count; i++) {
            int left = random.nextInt(800);
            int top = random.nextInt(1200);
            int size = 80 + random.nextInt(120);
            result.add(new Rect(left, top, left + size, top + size));
        }
        return result;
    }
}
