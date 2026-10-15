# 實作筆記

## 低功耗實時人型偵測建議
1. **YOLOv8n TFLite**（推薦）：速度快、準確度高，適合手機實時
2. 降低處理幀率或使用 GPU Delegate
3. Overlay 僅繪製 Bounding Box
4. Foreground Service + MediaProjection

## 目前進度
- OverlayView 動態畫框
- DetectionManager stub（備用）
- MediaProjection + ImageReader 完整流程
- **YoloDetector**：TFLite YOLO 推理骨架（person class）
- ImageUtils：Image → Bitmap 轉換
- OverlayService 優先使用 YoloDetector（若 assets 有模型）

## 如何使用 YOLO
1. 下載或匯出 YOLOv8n TFLite 模型（Ultralytics 官方）：
   ```bash
   # 在有 ultralytics 的環境
   yolo export model=yolov8n.pt format=tflite imgsz=640
   ```
   或從社群找已轉換的 `yolov8n.tflite`。
2. 將 `yolov8n.tflite` 放到：
   `app/src/main/assets/yolov8n.tflite`
3. 重新編譯。若模型載入成功，會自動使用真實 YOLO 偵測；否則回退到 stub。

## 注意
- `YoloDetector` 的 output shape 與 post-processing 需依實際匯出模型調整（目前為常見 [1,84,8400] 範例）。
- 建議開啟 GPU Delegate 以提升速度。
- 處理 Image 時注意記憶體與幀率控制，避免過熱。

## 下一步
- 微調 YoloDetector post-processing 與 NMS
- 加入幀率限制與功耗優化
- 處理螢幕旋轉與座標映射
