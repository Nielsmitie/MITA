package com.example.test;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TestNet {

    private String modelPath = "g1_quick_draw.tflite";
    private String labelPath = "labels.txt";

    private int imageWidth = 28;
    private int imageHeight = 28;
    private int bytesPerChannel = 4;

    private int MAX_RESULTS = 3;
    private float[][] labelProbArray = null;

    private MappedByteBuffer tfliteModel;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private Interpreter tflite;
    private ByteBuffer imgData = null;
    private List<String> labels;

    private static final Logger LOGGER = new Logger();

    public TestNet(Activity activity) {
        try {
            tfliteModel = loadModelFile(activity);
        }catch (IOException e){
            LOGGER.w("File not loaded");
        }
        tfliteOptions.setNumThreads(-1);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        try {
            labels = loadLabelList(activity);
        }catch (IOException e){
            LOGGER.w("File not loaded");
        }

        imgData =
                ByteBuffer.allocateDirect(
                        imageHeight
                                * imageWidth
                                //* DIM_PIXEL_SIZE
                                * bytesPerChannel);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labels.size()];
    }

    public List<TestNet.Recognition> recognizeImage(final int[] pixel_map) {
        // Log this method so that it can be analyzed with systrace.
        convertBitmapToByteBuffer(pixel_map);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("runInference");
        long startTime = SystemClock.uptimeMillis();
        tflite.run(imgData, labelProbArray);
        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        LOGGER.v("Timecost to run model inference: " + (endTime - startTime));

        // Find the best classifications.
        PriorityQueue<TestNet.Recognition> pq =
                new PriorityQueue<>(
                        3,
                        new Comparator<TestNet.Recognition>() {
                            @Override
                            public int compare(TestNet.Recognition lhs, TestNet.Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < labels.size(); ++i) {
            pq.add(
                    new TestNet.Recognition(
                            "" + i,
                            labels.size() > i ? labels.get(i) : "unknown",
                            labelProbArray[0][i],
                            null));
        }
        final ArrayList<TestNet.Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        Trace.endSection();
        return recognitions;
    }

    private void convertBitmapToByteBuffer(int[] intValues) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < imageWidth; ++i) {
            for (int j = 0; j < imageHeight; ++j) {
                final int val = intValues[pixel++];
                // todo take the int value and place it as float into the byteBuffer
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        LOGGER.v("Timecost to put values into ByteBuffer: " + (endTime - startTime));
    }

    private void addPixelValue(int pixelValue) {
        // todo change the what value is actually placed into the buffer object
        /*
        //int alpha = Color.alpha(pixelValue);
        int red = Color.red(pixelValue);
        int green = Color.green(pixelValue);
        int blue = Color.blue(pixelValue);
        // red = green = blue
        int grey = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
        LOGGER.w(Float.toString(grey));
        */
        //int grey = Color.argb(alpha, red, green, blue);
        LOGGER.w(Float.toString(pixelValue));
        imgData.putFloat(pixelValue);
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labels = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }


    /** An immutable result returned by a Classifier describing what was recognized. */
    public static class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        /** Display name for the recognition. */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        /** Optional location within the source image for the location of the recognized object. */
        private RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }

}
