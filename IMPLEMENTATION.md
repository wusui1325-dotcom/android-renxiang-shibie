# 實作筆記

## 低功耗實時人型偵測建議
1. 使用 ML Kit Object Detection (person class) 或輕量 TFLite 模型
2. 降低偵測幀率（例如 5-10 FPS）
3. 僅在 Accessibility 事件或特定條件下觸發
4. Overlay 僅繪製 Bounding Box，不處理觸控
5. 使用 Foreground Service 保持運行

## 目前進度
- OverlayView 已支援動態更新 Bounding Boxes
- DetectionManager 提供可替換的偵測迴圈（目前 stub，約 6-7 FPS）
- OverlayService 已整合兩者，啟動後即可看到測試框

## 下一步
- 實作畫面擷取（MediaProjection API，需使用者授權）
- 整合 ML Kit 或 TFLite person detection
- 將偵測結果座標映射到 OverlayView
- 加入開關與功耗監控
- 處理螢幕旋轉與多視窗情況
