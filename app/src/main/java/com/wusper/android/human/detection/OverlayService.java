package com.wusper.android.human.detection;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.graphics.Bitmap;
import java.util.List;
import android.view.Gravity;
import android.view.WindowManager;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private OverlayView overlayView;
    private DetectionManager detectionManager;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private YoloDetector yoloDetector;
    private android.os.HandlerThread processThread;
    private android.os.Handler processHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        // 先確保通知一定會出現
        createNotificationChannel();
        try {
            startForeground(1, buildNotification());
        } catch (Throwable t) {
            // 即使失敗也繼續，避免完全沒通知
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlayView = new OverlayView(this);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            windowManager.addView(overlayView, params);

            try {
                yoloDetector = new YoloDetector(this);
            } catch (Throwable t) {
                yoloDetector = null;
            }

            // 更新通知顯示模型狀態
            try {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.notify(1, buildNotification());
            } catch (Throwable ignored) {}

            processThread = new android.os.HandlerThread("YoloProcess");
            processThread.start();
            processHandler = new android.os.Handler(processThread.getLooper());

            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            screenDensity = metrics.densityDpi;
        } catch (Throwable t) {
            // 不在這裡 stopSelf，讓通知至少留著
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && intent.hasExtra("media_projection_data")) {
            int resultCode = intent.getIntExtra("media_projection_result_code", -1);
            Intent data = intent.getParcelableExtra("media_projection_data");
            if (resultCode == Activity.RESULT_OK && data != null) {
                startScreenCapture(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private long lastProcessTime = 0;
    private static final long MIN_INTERVAL_MS = 250; // 最多約 4 FPS，降低卡頓

    private void startScreenCapture(int resultCode, Intent data) {
        try {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpm.getMediaProjection(resultCode, data);

            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopSelf();
                }
            }, processHandler);

            // 降低擷取解析度以減少負載
            int captureW = Math.max(320, screenWidth / 2);
            int captureH = Math.max(240, screenHeight / 2);

            imageReader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                long now = System.currentTimeMillis();
                if (now - lastProcessTime < MIN_INTERVAL_MS) {
                    Image skip = reader.acquireLatestImage();
                    if (skip != null) skip.close();
                    return;
                }
                lastProcessTime = now;

                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) return;

                    Image finalImage = image;
                    processHandler.post(() -> {
                        try {
                            if (yoloDetector != null) {
                                Bitmap bitmap = ImageUtils.imageToBitmap(finalImage);
                                if (bitmap != null) {
                                    List<android.graphics.Rect> boxes = yoloDetector.detect(bitmap, screenWidth, screenHeight);
                                    overlayView.post(() -> overlayView.updateBoxes(boxes));
                                    bitmap.recycle();
                                }
                            }
                        } catch (Throwable ignored) {
                        } finally {
                            try {
                                finalImage.close();
                            } catch (Exception ignored) {
                            }
                        }
                    });
                } catch (Throwable ignored) {
                    if (image != null) {
                        try { image.close(); } catch (Exception ignored2) {}
                    }
                }
            }, processHandler);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "HumanDetection",
                    captureW, captureH, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, null);
        } catch (Throwable t) {
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "overlay", "Overlay Service", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        String status = (yoloDetector != null) ? "YOLO 已載入（點停止結束）" : "未載入模型";
        Intent stopIntent = new Intent(this, OverlayService.class);
        stopIntent.setAction("STOP");
        android.app.PendingIntent stopPending = android.app.PendingIntent.getService(
                this, 1, stopIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, "overlay")
                .setContentTitle("人像識別")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPending)
                .setContentIntent(stopPending)
                .setOngoing(false)
                .setAutoCancel(false)
                .build();
    }

    @Override
    public void onDestroy() {
        if (detectionManager != null) {
            detectionManager.stop();
        }
        if (processThread != null) {
            processThread.quitSafely();
        }
        if (yoloDetector != null) {
            yoloDetector.close();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (imageReader != null) {
            imageReader.close();
        }
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
