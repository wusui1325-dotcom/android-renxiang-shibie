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
        createNotificationChannel();
        startForeground(1, buildNotification());

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
        } catch (Exception e) {
            yoloDetector = null;
        }

        // 僅在沒有 YOLO 時才用 stub 偵測
        if (yoloDetector == null) {
            detectionManager = new DetectionManager();
            detectionManager.setListener(overlayView::updateBoxes);
            detectionManager.start();
        }

        processThread = new android.os.HandlerThread("YoloProcess");
        processThread.start();
        processHandler = new android.os.Handler(processThread.getLooper());

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("media_projection_data")) {
            int resultCode = intent.getIntExtra("media_projection_result_code", -1);
            Intent data = intent.getParcelableExtra("media_projection_data");
            if (resultCode == Activity.RESULT_OK && data != null) {
                startScreenCapture(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private void startScreenCapture(int resultCode, Intent data) {
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, data);

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image == null) return;

            processHandler.post(() -> {
                try {
                    if (yoloDetector != null) {
                        Bitmap bitmap = ImageUtils.imageToBitmap(image);
                        if (bitmap != null) {
                            List<android.graphics.Rect> boxes = yoloDetector.detect(bitmap, screenWidth, screenHeight);
                            overlayView.post(() -> overlayView.updateBoxes(boxes));
                            bitmap.recycle();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    image.close();
                }
            });
        }, processHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "HumanDetection",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "overlay", "Overlay Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, "overlay")
                .setContentTitle("人像識別運行中")
                .setContentText("實時畫框")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
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
