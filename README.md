# Android 人像識別 (Real-time Human Detection Overlay)

低功耗、快速、實時的 Android 懸浮窗人型識別與畫框應用。

## 功能
- 請求懸浮窗權限 (SYSTEM_ALERT_WINDOW)
- 請求無障礙服務權限 (Accessibility Service)
- 實時在螢幕上偵測人型並以浮窗畫框
- 優先使用輕量模型以降低功耗

## 技術方向
- Overlay Window 繪製 Bounding Boxes
- **YOLOv8n TFLite**（最快實用方案）進行 person 偵測
- MediaProjection 畫面擷取
- Accessibility Service 輔助
- 背景執行緒處理，避免阻塞 UI

## 使用 YOLO
將 `yolov8n.tflite` 放入 `app/src/main/assets/` 後即可啟用真實偵測。
詳見 `IMPLEMENTATION.md`。

## 注意事項
- 需要使用者手動授權懸浮窗與無障礙權限
- 實時畫面擷取可能影響功耗，需優化幀率與模型
- 僅供研究與個人使用，注意隱私

## 建置
使用 Android Studio 開啟此專案。

目標 SDK：34+
最低 SDK：26+
