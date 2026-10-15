package com.wusper.android.human.detection;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: setContentView with buttons for permissions

        Button overlayBtn = new Button(this);
        overlayBtn.setText("請求懸浮窗權限");
        overlayBtn.setOnClickListener(v -> requestOverlayPermission());

        Button accessBtn = new Button(this);
        accessBtn.setText("開啟無障礙服務");
        accessBtn.setOnClickListener(v -> openAccessibilitySettings());

        // Simple layout - replace with proper XML later
        setContentView(overlayBtn);
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
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
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
            if (Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else {
                Toast.makeText(this, "需要懸浮窗權限", Toast.LENGTH_LONG).show();
            }
        }
    }
}
