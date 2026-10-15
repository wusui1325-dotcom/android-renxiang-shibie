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
- OverlayService 已整合兩者
- MainActivity 支援懸浮窗、無障礙、畫面擷取（MediaProjection）權限請求
- OverlayService 可接收 MediaProjection 並建立 VirtualDisplay + ImageReader
- ImageReader 回調已就緒，可在此接入真實偵測模型

## 下一步
- 在 ImageReader 回調中將 Image 轉為 Bitmap 並送入 ML Kit / TFLite
- 將模型輸出的座標映射到 OverlayView（注意密度與旋轉）
- 加入開關控制偵測幀率與功耗
- 處理螢幕旋轉、多視窗、權限回收情況
- 優化 Image 處理避免記憶體洩漏
