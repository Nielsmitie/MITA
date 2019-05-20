package com.example.test;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private long lastProcessingTimeMs;
    private static final Logger LOGGER = new Logger();

    private PaintView paintView;
    private Button button;

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
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap =
                Bitmap.createBitmap(
                        classifier.getImageSizeX(), classifier.getImageSizeY(), Bitmap.Config.ARGB_8888);
        // todo better way to cut the image instead of cropping it ?!
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth,
                        previewHeight,
                        classifier.getImageSizeX(),
                        classifier.getImageSizeY(),
                        Configuration.ORIENTATION_LANDSCAPE,
                        MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        // functionen zum laden des bildes als Canvas und classify nutzen
        // https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/app/src/main/java/org/tensorflow/lite/examples/classification/ClassifierActivity.java
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int[] intArray = new int[previewWidth * previewHeight];
                paintView.getBitmap().getPixels(intArray, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                // ImageUtils.convertYUV420SPToARGB8888(byteArray, previewWidth, previewHeight, rgbBytes);

                rgbFrameBitmap.setPixels(intArray, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                final Canvas canvas = new Canvas(croppedBitmap);
                canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                LOGGER.v("Detect: %s", results);

                Toast.makeText(getApplicationContext(),
                        results.get(0).getTitle(),
                        Toast.LENGTH_SHORT).show();
                        // canvas von ImageView auslesen
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
}
