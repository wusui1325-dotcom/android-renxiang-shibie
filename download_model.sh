#!/bin/bash
# 自動下載 YOLO 模型到 assets
# 使用 Ultralytics 官方 yolo-flutter-app release 的 yolo26n_w8a32.tflite

set -e
ASSETS_DIR="app/src/main/assets"
MODEL_NAME="yolov8n.tflite"
MODEL_URL="https://github.com/ultralytics/yolo-flutter-app/releases/download/v0.6.6/yolo26n_w8a32.tflite"

mkdir -p "$ASSETS_DIR"

if [ -f "$ASSETS_DIR/$MODEL_NAME" ]; then
  echo "模型已存在：$ASSETS_DIR/$MODEL_NAME"
  exit 0
fi

echo "正在下載 YOLO 模型..."
curl -L -o "$ASSETS_DIR/$MODEL_NAME" "$MODEL_URL"
echo "下載完成：$ASSETS_DIR/$MODEL_NAME"
ls -lh "$ASSETS_DIR/$MODEL_NAME"
