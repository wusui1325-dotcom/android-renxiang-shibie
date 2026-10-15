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
- **已移除隨機測試框**（避免亂框）。沒有模型時不會畫任何框。

## 如何使用 YOLO
1. 將 `yolov8n.tflite` 放到 `app/src/main/assets/yolov8n.tflite`
2. 重新編譯。授權懸浮窗 + 畫面擷取後，才會用 YOLO 實時畫框。
3. 若模型輸出格式與程式假設不同，框可能不準，需調整 `YoloDetector.parseYoloOutput`。

## 穩定性更新（本次）
- 關閉 GPU Delegate（部分裝置會閃退）
- 動態讀取模型輸出 shape，避免硬編碼導致崩潰
- Image → Bitmap 與推理全程 try-catch
- 通知列會顯示「YOLO 已載入」或「未載入模型」
- 沒有模型時不畫框、不閃退

## 模型自動下載
執行 `./download_model.sh` 即可從 Ultralytics 官方 release 下載模型到 `app/src/main/assets/yolov8n.tflite`。
GitHub Actions 也會自動下載。