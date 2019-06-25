package com.example.test;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private long lastProcessingTimeMs;
    private static final Logger LOGGER = new Logger();

    private DrawingView drawingView;
    private Button button;
    private Button clear_button;
    private ImageView viewer;

    private Bitmap croppedBitmap = null;

    private TestNet testNet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        drawingView = findViewById(R.id.drawingView);
        button = findViewById(R.id.classify);
        clear_button = findViewById(R.id.clearButton);
        viewer = findViewById(R.id.imageView2);

        testNet = new TestNet(this);

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.reset();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                croppedBitmap = drawingView.getBitmap();

                viewer.setImageBitmap(croppedBitmap);

                final long startTime = SystemClock.uptimeMillis();
                final List<TestNet.Recognition> results = testNet.recognizeImage(drawingView.getPixelMap());
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                LOGGER.w("Detect: %s", results);

                Toast.makeText(getApplicationContext(),
                        results.get(0).getTitle(),
                        Toast.LENGTH_SHORT).show();
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
