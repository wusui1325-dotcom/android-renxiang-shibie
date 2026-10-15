package com.wusper.android.human.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * YOLOv8n TFLite detector.
 * Place yolov8n.tflite in app/src/main/assets/
 */
public class YoloDetector {

    private static final String MODEL_FILE = "yolov8n.tflite";
    private static final float CONF_THRESHOLD = 0.35f;

    private Interpreter interpreter;
    private final int inputWidth;
    private final int inputHeight;

    public YoloDetector(Context context) throws IOException {
        MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_FILE);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        // GPU Delegate 在部分裝置會閃退，預設關閉。需要時可手動開啟。
        interpreter = new Interpreter(model, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];
    }

    public List<Rect> detect(Bitmap bitmap, int screenWidth, int screenHeight) {
        if (interpreter == null || bitmap == null) return new ArrayList<>();

        try {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
            ByteBuffer inputBuffer = bitmapToByteBuffer(resized);
            resized.recycle();

            // 動態取得輸出 shape，避免硬編碼導致閃退
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Object output = createOutputArray(outputShape);
            interpreter.run(inputBuffer, output);

            List<RectF> boxes = parseOutput(output, outputShape, bitmap.getWidth(), bitmap.getHeight());

            List<Rect> result = new ArrayList<>();
            float scaleX = (float) screenWidth / Math.max(1, bitmap.getWidth());
            float scaleY = (float) screenHeight / Math.max(1, bitmap.getHeight());
            for (RectF box : boxes) {
                result.add(new Rect(
                        (int) (box.left * scaleX),
                        (int) (box.top * scaleY),
                        (int) (box.right * scaleX),
                        (int) (box.bottom * scaleY)
                ));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Object createOutputArray(int[] shape) {
        if (shape.length == 3) {
            return new float[shape[0]][shape[1]][shape[2]];
        } else if (shape.length == 2) {
            return new float[shape[0]][shape[1]];
        }
        return new float[1][84][8400];
    }

    @SuppressWarnings("unchecked")
    private List<RectF> parseOutput(Object output, int[] shape, int origW, int origH) {
        List<RectF> boxes = new ArrayList<>();
        try {
            float[][] data;
            if (output instanceof float[][][]) {
                data = ((float[][][]) output)[0];
            } else if (output instanceof float[][]) {
                data = (float[][]) output;
            } else {
                return boxes;
            }

            int rows = data.length;
            int cols = data[0].length;

            // 常見 YOLOv8: [84, 8400] → rows=84, cols=8400
            boolean isChannelFirst = rows < cols;
            int numAnchors = isChannelFirst ? cols : rows;

            for (int i = 0; i < Math.min(numAnchors, 2000); i++) { // 限制數量避免卡頓
                float cx, cy, w, h, score;
                if (isChannelFirst) {
                    cx = data[0][i];
                    cy = data[1][i];
                    w = data[2][i];
                    h = data[3][i];
                    score = data.length > 4 ? data[4][i] : 0f;
                } else {
                    cx = data[i][0];
                    cy = data[i][1];
                    w = data[i][2];
                    h = data[i][3];
                    score = data[i].length > 4 ? data[i][4] : 0f;
                }

                if (score < CONF_THRESHOLD) continue;

                float left = (cx - w / 2f) * origW;
                float top = (cy - h / 2f) * origH;
                float right = (cx + w / 2f) * origW;
                float bottom = (cy + h / 2f) * origH;

                if (right > left && bottom > top && left >= -100 && top >= -100) {
                    boxes.add(new RectF(Math.max(0, left), Math.max(0, top), right, bottom));
                }
            }
        } catch (Exception ignored) {
        }
        return boxes;
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3);
        buffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputWidth * inputHeight];
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            buffer.putFloat((pixel & 0xFF) / 255.0f);
        }
        buffer.rewind();
        return buffer;
    }

    public void close() {
        if (interpreter != null) {
            try {
                interpreter.close();
            } catch (Exception ignored) {
            }
            interpreter = null;
        }
    }
}
