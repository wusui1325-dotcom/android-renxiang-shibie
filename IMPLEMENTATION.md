# 實作筆記

## 低功耗實時人型偵測建議
1. 使用 ML Kit Object Detection (person class) 或輕量 TFLite 模型
2. 降低偵測幀率（例如 5-10 FPS）
3. 僅在 Accessibility 事件或特定條件下觸發
4. Overlay 僅繪製 Bounding Box，不處理觸控
5. 使用 Foreground Service 保持運行

## 下一步
- 實作畫面擷取（MediaProjection 或 Accessibility 輔助）
- 整合偵測模型
- 在 OverlayView 上 Canvas 繪製矩形
- 優化功耗與延遲
