package com.wusper.android.human.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * YOLOv8n (or similar) TFLite detector focused on person class (COCO class 0).
 * Place yolov8n.tflite in app/src/main/assets/
 * Model expected input: 640x640 RGB, float32 [1,640,640,3] or quantized.
 * This is a simplified post-processing skeleton; adjust for your exact model output shape.
 */
public class YoloDetector {

    private static final String MODEL_FILE = "yolov8n.tflite";
    private static final int INPUT_SIZE = 640;
    private static final int PERSON_CLASS = 0;
    private static final float CONF_THRESHOLD = 0.4f;
    private static final float IOU_THRESHOLD = 0.5f;

    private Interpreter interpreter;
    private final int inputWidth;
    private final int inputHeight;
    private final boolean isQuantized;

    public YoloDetector(Context context) throws IOException {
        MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_FILE);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        try {
            options.addDelegate(new GpuDelegate());
        } catch (Exception ignored) {
            // GPU not available, fall back to CPU
        }
        interpreter = new Interpreter(model, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];
        isQuantized = interpreter.getInputTensor(0).dataType().toString().contains("UINT8")
                || interpreter.getInputTensor(0).dataType().toString().contains("INT8");
    }

    public List<Rect> detect(Bitmap bitmap, int screenWidth, int screenHeight) {
        if (interpreter == null || bitmap == null) return new ArrayList<>();

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer inputBuffer = bitmapToByteBuffer(resized);

        // Output shape varies by model. Common YOLOv8 TFLite: [1, 84, 8400] or similar
        // Adjust output buffer size and parsing according to your exported model.
        float[][][] output = new float[1][84][8400]; // example shape, change if needed
        interpreter.run(inputBuffer, output);

        List<RectF> boxes = parseYoloOutput(output[0], bitmap.getWidth(), bitmap.getHeight());
        // Simple NMS
        boxes = nms(boxes, IOU_THRESHOLD);

        List<Rect> result = new ArrayList<>();
        float scaleX = (float) screenWidth / bitmap.getWidth();
        float scaleY = (float) screenHeight / bitmap.getHeight();
        for (RectF box : boxes) {
            result.add(new Rect(
                    (int) (box.left * scaleX),
                    (int) (box.top * scaleY),
                    (int) (box.right * scaleX),
                    (int) (box.bottom * scaleY)
            ));
        }
        return result;
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3);
        buffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputWidth * inputHeight];
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;
            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }
        return buffer;
    }

    private List<RectF> parseYoloOutput(float[][] output, int origW, int origH) {
        List<RectF> boxes = new ArrayList<>();
        // Simplified parsing for common YOLOv8 format.
        // output[class/coord][anchor]
        // This needs to match your specific model export.
        // For production, use proper YOLO post-processing (sigmoid, NMS, etc.).
        int numAnchors = output[0].length;
        for (int i = 0; i < numAnchors; i++) {
            float conf = output[4][i]; // example confidence location
            if (conf < CONF_THRESHOLD) continue;

            // Assume person class score is high enough (filter class 0)
            float classScore = output[5 + PERSON_CLASS][i];
            if (classScore < CONF_THRESHOLD) continue;

            float cx = output[0][i];
            float cy = output[1][i];
            float w = output[2][i];
            float h = output[3][i];

            float left = (cx - w / 2) * origW;
            float top = (cy - h / 2) * origH;
            float right = (cx + w / 2) * origW;
            float bottom = (cy + h / 2) * origH;

            boxes.add(new RectF(left, top, right, bottom));
        }
        return boxes;
    }

    private List<RectF> nms(List<RectF> boxes, float iouThreshold) {
        // Basic NMS skeleton - sort by confidence and suppress overlaps
        // For brevity, return as-is; implement full NMS for production
        return boxes;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
