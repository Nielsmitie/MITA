package com.example.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {

    private final int XYSIZE = 28;
    //private float[][] pixelMap;
    private int[] pixelMap;
    private Paint drawPaint, canvasPaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    // todo change the initial color of the empty pixel map
    private final int background = 0;


    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        //pixelMap = new float[XYSIZE][XYSIZE];
        pixelMap = new int[XYSIZE * XYSIZE];
        setupDrawing();
    }

    public int[] getPixelMap(){
        return pixelMap;
    }

    public Bitmap getBitmap(){
        // return Bitmap.createBitmap(pixelMap, XYSIZE, XYSIZE, Bitmap.Config.ARGB_4444);
        return canvasBitmap;
    }

    private void setupDrawing() {
        int paintColor = 0xFF000000;

        drawPaint = new Paint();
        drawPaint.setColor(paintColor);

        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.FILL);
        canvasPaint = new Paint(Paint.DITHER_FLAG);

        for(int i=0; i < XYSIZE*XYSIZE; i++){
            pixelMap[i] = background;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(event.getX() < this.getWidth() && event.getX() > 0 && event.getY() < this.getHeight() &&
                event.getY() > 0){
            float colorStrength = event.getPressure();
            drawPaint.setStrokeWidth(colorStrength);
            drawPaint.setAlpha((int)(colorStrength * 255f));

            float width_scaling = this.getWidth() / (float) XYSIZE;
            float height_scaling = this.getHeight() / (float) XYSIZE;

            float x_test = event.getX() - (event.getX() % width_scaling);
            float y_test = event.getY() - (event.getY() % height_scaling);

            int x = (int)(x_test / width_scaling);
            int y = (int)(y_test / height_scaling);

            // int tmp = Color.argb((int)(colorStrength * 255), 255, 255, 255);
            // tmp = ColorUtils.compositeColors(tmp, Color.WHITE);
            // todo change the value in pixel map for strokes
            pixelMap[y * XYSIZE + x] = (int)(colorStrength * 255);

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    drawCanvas.drawRect(x_test, y_test, x_test+ width_scaling, y_test + height_scaling, drawPaint);
                    break;
                case MotionEvent.ACTION_MOVE:
                    drawCanvas.drawRect(x_test, y_test, x_test+ width_scaling, y_test + height_scaling, drawPaint);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                default:
                    return false;
            }

            invalidate();
            return true;
        }
        return false;
    }

    public void reset(){
        onSizeChanged(this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        invalidate();
        pixelMap = new int[XYSIZE * XYSIZE];
        for(int i=0; i < XYSIZE*XYSIZE; i++){
            pixelMap[i] = background;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas){
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
    }
}
