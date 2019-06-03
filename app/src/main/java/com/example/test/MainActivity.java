package com.example.test;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private long lastProcessingTimeMs;
    private static final Logger LOGGER = new Logger();

    private PaintView paintView;
    private Button button;
    private Button clear_button;
    private ImageView imageView;

    private Classifier.Model model = Classifier.Model.FLOAT;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = -1;
    private Classifier classifier;

    private int previewWidth;
    private int previewHeight;
    private int[] rgbBytes;

    private Bitmap croppedBitmap = null;
    private Bitmap rgbFrameBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private static final boolean MAINTAIN_ASPECT = true;

    // todo clear image button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        paintView = (PaintView) findViewById(R.id.paintView);
        button = (Button) findViewById(R.id.classify);
        clear_button = (Button) findViewById(R.id.clearButton);
        imageView = (ImageView) findViewById(R.id.preview);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);

        previewWidth = metrics.widthPixels;
        previewHeight = metrics.heightPixels;

        try {
            classifier = Classifier.create(this, model, device, numThreads);
        } catch (IOException e) {
            e.printStackTrace();
        }

        croppedBitmap =
                Bitmap.createBitmap(
                        classifier.getImageSizeX(), classifier.getImageSizeY(), Bitmap.Config.ARGB_8888);


        // functionen zum laden des bildes als Canvas und classify nutzen
        // https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/app/src/main/java/org/tensorflow/lite/examples/classification/ClassifierActivity.java
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int[] intArray = new int[previewWidth * previewHeight];
                paintView.getBitmap().getPixels(intArray, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                // ImageUtils.convertYUV420SPToARGB8888(byteArray, previewWidth, previewHeight, rgbBytes);

                int[] result = calc_dims(intArray, previewWidth, previewHeight);
                int left = result[0];
                int right = result[1];
                int top = result[2];
                int bottom = result[3];

                int window_width = result[1] - result[0];
                int window_height = result[3] - result[2];

                frameToCropTransform =
                        ImageUtils.getTransformationMatrix(
                                window_width,
                                window_height,
                                classifier.getImageSizeX(),
                                classifier.getImageSizeY(),
                                Configuration.ORIENTATION_PORTRAIT,
                                false);
                cropToFrameTransform = new Matrix();
                frameToCropTransform.invert(cropToFrameTransform);
                rgbFrameBitmap = Bitmap.createBitmap(result[1] - result[0], result[3] - result[2], Bitmap.Config.ARGB_8888);

                LOGGER.w("orignial size width: %d, height: %d", previewWidth, previewHeight);

                LOGGER.w("Cropped image\nleft: %d, right: %d, top: %d, bottom: %d", result[0], result[1], result[2], result[3]);

                LOGGER.w("window width %d", window_width);
                LOGGER.w("window height %d", window_height);
                LOGGER.w("offset %d", top * previewWidth + left);
                LOGGER.w("stride %d", left + right);

                // rgbFrameBitmap.setPixels(intArray, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                //rgbFrameBitmap.setPixels(intArray, 0, result[0] + result[1], result[2],
                //        result[0], result[1] - result[0], result[3] - result[2]);
                /*
                rgbFrameBitmap.setPixels(intArray, result[2] * previewWidth + result[0],
                        result[1] + result[0], 0, 0,
                        result[1] - result[0], result[3] - result[2]);
                */
                int[] window_array = new int[window_height * window_width];
                paintView.getBitmap().getPixels(window_array, 0, window_width, left,
                        top, window_width, window_height);
                rgbFrameBitmap.setPixels(window_array, 0, window_width, 0, 0, window_width, window_height);

                final Canvas canvas = new Canvas(croppedBitmap);
                canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

                imageView.setImageBitmap(croppedBitmap);

                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                LOGGER.w("Detect: %s", results);

                Toast.makeText(getApplicationContext(),
                        results.get(0).getTitle(),
                        Toast.LENGTH_SHORT).show();
                        // canvas von ImageView auslesen
            }
        });

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.clear();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int[] calc_dims(int[] array, int width, int height){
        int top = height;
        int bottom = 0;
        int left = width;
        int right = 0;

        for(int i=0; i < width; i++){
            for(int j=0; j < height; j++){
                if(Color.WHITE != array[j + i * width]){
                    if(i > right) right = i;
                    if(i < left) left = i;
                    if(j > bottom) bottom = j;
                    if(j < top) top = j;
                }
            }
        }
        int[] result = {left, right, top, bottom};
        return result;
    }
}
