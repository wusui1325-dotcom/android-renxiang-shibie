## 高幀率優化（本次更新）
- 畫面處理移到專用 HandlerThread，ImageReader 回調立即返回
- 只處理最新幀（acquireLatestImage）
- YOLO 推論啟用 GPU Delegate（可用時）
- 有 YOLO 模型時停用 stub 偵測迴圈，避免競爭
- 目標：盡可能高的實時幀率（實際取決於裝置 GPU/CPU 與模型）

## 目前進度
- OverlayView 動態畫框
- MediaProjection + ImageReader 完整流程
- **YoloDetector**：TFLite YOLO 推理（GPU 優先）
- ImageUtils：Image → Bitmap
- OverlayService 高幀率非同步處理

## 如何使用 YOLO
1. 將 `yolov8n.tflite` 放到 `app/src/main/assets/yolov8n.tflite`
2. 重新編譯。授權懸浮窗 + 畫面擷取後，以高幀率實時畫框。

## 注意
- 高幀率會明顯增加功耗與發熱，可依需求再加幀率上限。
- output shape / post-processing 需依實際模型調整。
- 建議使用量化版（int8）模型以進一步加速。