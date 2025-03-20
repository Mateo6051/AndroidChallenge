package com.example.goforit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements
        SurfaceHolder.Callback {

    private GameThread thread;
    private int x=0;
    private int valeur_y;


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    public void update() {
        x = (x + 1) % 300;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));
            canvas.drawRect(x, 100, x+100, 200, paint);
        }
    }




}
