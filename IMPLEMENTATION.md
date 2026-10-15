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

## 穩定性更新（閃退修復）
- 改用 `foregroundServiceType="mediaProjection"` + 對應權限（Android 14+ 需要）
- `onCreate` 全程 try-catch，避免啟動就死
- MainActivity 啟動服務時顯示 Toast
- 沒有模型或推理失敗時不畫框、不閃退

## 測試步驟
1. 先執行 `./download_model.sh`
2. 編譯安裝
3. 點「請求懸浮窗權限並啟動」
4. 看通知是否出現「人像識別」
5. 再點「請求畫面擷取權限」

## 模型自動下載
執行 `./download_model.sh` 即可從 Ultralytics 官方 release 下載模型到 `app/src/main/assets/yolov8n.tflite`。
GitHub Actions 也會自動下載。