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
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private long lastProcessingTimeMs;
    private static final Logger LOGGER = new Logger();

    private DrawingView drawingView;
    private Button button;
    private Button clear_button;
    private ImageView taskView;
    private Button start;
    private TextView scoreValue;

    private Bitmap croppedBitmap = null;

    private TestNet testNet;

    private int label = -1;

    private String[] classes = {"apple", "bench", "campfire", "clock", "elephant", "hammer",
            "lollipop", "pig", "spoon", "tshirt", "whale"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        drawingView = findViewById(R.id.drawingView);
        button = findViewById(R.id.classify);
        clear_button = findViewById(R.id.clearButton);
        taskView = findViewById(R.id.imageView);
        start = findViewById(R.id.start_button);
        scoreValue = findViewById(R.id.ScoreValue);

        testNet = new TestNet(this);

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.reset();
            }
        });
        // LOGGER.w("PACKAGE " + this.getPackageName());

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.reset();
                Random rand = new Random();
                label = rand.nextInt(11);
                LOGGER.w("label " + label);
                //label = 10;
                String file_path = "@drawable/" + classes[label] + "_1";
                int image = getResources().getIdentifier(file_path, null, "com.example.test");
                taskView.setImageResource(image);
            }
        });



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                croppedBitmap = drawingView.getBitmap();

                // taskView.setImageBitmap(croppedBitmap);
                if(label != -1) {
                    final long startTime = SystemClock.uptimeMillis();
                    final List<TestNet.Recognition> results = testNet.recognizeImage(drawingView.getPixelMap());
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                    LOGGER.w("Detect: %s", results);

                    for (int i = 0; i < results.size(); i++) {
                        LOGGER.w("Detected " + results.get(i).getTitle() + " with " +
                                results.get(i).getConfidence() + " confidence at pos." + (i + 1));
                        if (results.get(i).getId().equals(String.valueOf(label))) {
                            String output = "Detected " + results.get(i).getTitle() + " with " +
                                    results.get(i).getConfidence() + " confidence at pos." + (i + 1);

                            Toast.makeText(getApplicationContext(),
                                    output,
                                    Toast.LENGTH_SHORT).show();

                            scoreValue.setText(String.valueOf((int) ((results.get(i).getConfidence() * 3.414 * 10000) * (classes.length - i))));

                            break;
                        }
                    }
                }
                start.setText("Nächstes");
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
