package com.example.goforit;

import android.app.Activity;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
    private static final String TAG = "GameView";
    private GameThread thread;
    private int playerX = 0; // Position x du joueur dans la grille (0-9)
    private int playerY = 0; // Position y du joueur dans la grille (0-9)
    private int valeur_y; // Gardé pour compatibilité avec MainActivity
    private boolean[][] obstacles = new boolean[10][10]; // Grille 10x10 pour obstacles
    private Direction direction = Direction.RIGHT; // Direction initiale
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float tiltThreshold = 2.0f;
    private float neutralZone = 1.0f;
    private long lastDirectionChange = 0;
    private static final long DIRECTION_CHANGE_COOLDOWN = 500;
    private float[] lastAccValues = new float[3];
    private static final float ALPHA = 0.8f;
    private int moveCounter = 0;
    private static final int MOVE_DELAY = 5;
    private int cellSize;
    private Bitmap stoneBitmapStone;
    private Bitmap stoneBitmapBall;

    private int gridSize;
    private Bitmap backgroundImage;
    private int level;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT, STOPPED
    }
    public GameView(Context context, int valeur_y) {
        super(context);
        this.valeur_y = valeur_y;
        this.backgroundImage = backgroundImage;
        this.gridSize = gridSize;
        this.obstacles = new boolean[gridSize][gridSize];
        this.level = ((Activity) context).getIntent().getIntExtra("level", 1);
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        if (gridSize >= 10) {
            obstacles[3][4] = true;
            obstacles[7][8] = true;
            obstacles[gridSize - 1][gridSize - 1] = true;
        }

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Log.e(TAG, "Accéléromètre non disponible sur cet appareil");
        } else {
            Log.d(TAG, "Accéléromètre initialisé avec succès");
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        calculateCellSize();
    }

    private void calculateCellSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth, screenHeight) / gridSize;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        stoneBitmapStone = BitmapFactory.decodeResource(getResources(), R.drawable.stone_icon);
        stoneBitmapBall = BitmapFactory.decodeResource(getResources(), R.drawable.red_ball);
        int newWidth = cellSize;
        int newHeight = cellSize;
        stoneBitmapStone = Bitmap.createScaledBitmap(stoneBitmapStone, newWidth, newHeight, true);
        stoneBitmapBall = Bitmap.createScaledBitmap(stoneBitmapBall, newWidth, newHeight, true);

        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        sensorManager.unregisterListener(this);
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
        setFocusable(false); // Désactiver le focus pour libérer les événements tactiles
    }

    private float[] filterAccValues(float[] values) {
        lastAccValues[0] = ALPHA * lastAccValues[0] + (1 - ALPHA) * values[0];
        lastAccValues[1] = ALPHA * lastAccValues[1] + (1 - ALPHA) * values[1];
        lastAccValues[2] = ALPHA * lastAccValues[2] + (1 - ALPHA) * values[2];
        return lastAccValues;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && direction == Direction.STOPPED) {
            float[] filteredValues = filterAccValues(event.values);
            float x = filteredValues[0];
            float y = filteredValues[1];
            float z = filteredValues[2];
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastDirectionChange < DIRECTION_CHANGE_COOLDOWN) {
                return;
            }

            if (z < 6.0f) {
                return;
            }

            if (Math.abs(x) < neutralZone && Math.abs(y) < neutralZone) {
                return;
            }

            Direction newDirection = Direction.STOPPED;
            
            if (Math.abs(x) > Math.abs(y)) {
                if (Math.abs(x) > tiltThreshold) {
                    newDirection = (x > 0) ? Direction.LEFT : Direction.RIGHT;
                }
            } else {
                if (Math.abs(y) > tiltThreshold) {
                    newDirection = (y > 0) ? Direction.DOWN : Direction.UP;
                }
            }

            if (newDirection != Direction.STOPPED) {
                direction = newDirection;
                lastDirectionChange = currentTime;
                Log.d(TAG, "Nouvelle direction: " + direction + " - acc: x=" + x + ", y=" + y + ", z=" + z);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas besoin d'implémentation spécifique pour le moment
    }

    public void update() {
        if (direction == Direction.STOPPED) {
            return;
        }

        moveCounter++;
        if (moveCounter < MOVE_DELAY) {
            return;
        }
        moveCounter = 0;

        int nextX = playerX;
        int nextY = playerY;

        switch (direction) {
            case UP:
                nextY--;
                break;
            case DOWN:
                nextY++;
                break;
            case LEFT:
                nextX--;
                break;
            case RIGHT:
                nextX++;
                break;
        }

        if (nextX < 0 || nextX >= gridSize || nextY < 0 || nextY >= gridSize) {
            direction = Direction.STOPPED;
            return;
        }

        if (nextX == gridSize - 1 && nextY == gridSize - 1) {
            direction = Direction.STOPPED;
            Intent intent = new Intent(getContext(), NextLevelScreen.class);
            intent.putExtra("level", level);
            getContext().startActivity(intent);
            ((Activity) getContext()).finish();
            return;
        }

        if (obstacles[nextX][nextY]) {
            Log.d(TAG, "Collision avec un obstacle détectée");
            direction = Direction.STOPPED;
            return;
        }

        playerX = nextX;
        playerY = nextY;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            if (backgroundImage != null) {
                int totalSize = cellSize * gridSize;
                Rect destRect = new Rect(0, 0, totalSize, totalSize);
                canvas.drawBitmap(backgroundImage, null, destRect, null);
            } else {
                canvas.drawColor(Color.WHITE);
            }

            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));

            int pixelX = playerX * cellSize;
            int pixelY = playerY * cellSize;

            canvas.drawBitmap(stoneBitmapBall, pixelX, pixelY, null);

            paint.setColor(Color.BLACK);
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (obstacles[i][j]) {
                        float left = i * cellSize;
                        float top = j * cellSize;
                        canvas.drawBitmap(stoneBitmapStone, left, top, null);
                    }
                }
            }
        }
    }
}