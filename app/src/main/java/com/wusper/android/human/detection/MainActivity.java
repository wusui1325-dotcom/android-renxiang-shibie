package com.wusper.android.human.detection;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY = 1001;
    private static final int REQUEST_MEDIA_PROJECTION = 1002;

    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        Button overlayBtn = new Button(this);
        overlayBtn.setText("請求懸浮窗權限並啟動");
        overlayBtn.setOnClickListener(v -> requestOverlayPermission());

        Button accessBtn = new Button(this);
        accessBtn.setText("開啟無障礙服務");
        accessBtn.setOnClickListener(v -> openAccessibilitySettings());

        Button captureBtn = new Button(this);
        captureBtn.setText("請求畫面擷取權限");
        captureBtn.setOnClickListener(v -> requestScreenCapture());

        Button stopBtn = new Button(this);
        stopBtn.setText("停止服務");
        stopBtn.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));

        layout.addView(overlayBtn);
        layout.addView(accessBtn);
        layout.addView(captureBtn);
        layout.addView(stopBtn);

        setContentView(layout);
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY);
            } else {
                Toast.makeText(this, "懸浮窗權限已授予", Toast.LENGTH_SHORT).show();
                startOverlayService();
            }
        } else {
            startOverlayService();
        }
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void requestScreenCapture() {
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                startOverlayService();
            } else {
                Toast.makeText(this, "需要懸浮窗權限", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            // 將 MediaProjection 結果傳給 Service
            Intent serviceIntent = new Intent(this, OverlayService.class);
            serviceIntent.putExtra("media_projection_result_code", resultCode);
            serviceIntent.putExtra("media_projection_data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "畫面擷取已授權，服務將使用真實畫面", Toast.LENGTH_SHORT).show();
        }
    }
}
