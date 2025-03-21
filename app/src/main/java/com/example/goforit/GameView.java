package com.example.goforit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private int playerX = 0;
    private int playerY = 0;
    private int valeur_y;
    private boolean[][] obstacles = new boolean[10][10];
    private Direction direction = Direction.STOPPED;

    // Gestion du gyroscope
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private float tiltThreshold = 1.5f;
    private float neutralZone = 0.8f;
    private long lastDirectionChange = 0;
    private static final long DIRECTION_CHANGE_COOLDOWN = 500;
    
    // Filtre pour les valeurs du gyroscope
    private float[] lastGyroValues = new float[2];
    private static final float ALPHA = 0.8f;

    // Contrôle de la vitesse
    private int moveCounter = 0;
    private static final int MOVE_DELAY = 5;

    private int cellSize;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT, STOPPED
    }

    public GameView(Context context, int valeur_y) {
        super(context);
        this.valeur_y = valeur_y;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        // Initialisation des obstacles
        obstacles[3][4] = true;
        obstacles[7][8] = true;
        obstacles[9][9] = true;

        // Initialisation du gyroscope
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope == null) {
            Log.e(TAG, "Gyroscope non disponible sur cet appareil");
        } else {
            Log.d(TAG, "Gyroscope initialisé avec succès");
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }

        calculateCellSize();
    }

    private void calculateCellSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth, screenHeight) / 10;
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
    }

    // Application d'un filtre passe-bas pour lisser les valeurs du gyroscope
    private float[] filterGyroValues(float[] values) {
        lastGyroValues[0] = ALPHA * lastGyroValues[0] + (1 - ALPHA) * values[0];
        lastGyroValues[1] = ALPHA * lastGyroValues[1] + (1 - ALPHA) * values[1];
        return lastGyroValues;
    }

    // Gestion des données du gyroscope
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && direction == Direction.STOPPED) {
            // Appliquer un filtre pour réduire le bruit
            float[] filteredValues = filterGyroValues(event.values);
            float x = filteredValues[0]; // Rotation autour de l'axe X 
            float y = filteredValues[1]; // Rotation autour de l'axe Y
            long currentTime = System.currentTimeMillis();

            // Vérifier le cooldown pour éviter les changements trop rapides
            if (currentTime - lastDirectionChange < DIRECTION_CHANGE_COOLDOWN) {
                return;
            }

            // Si l'inclinaison est trop faible (position neutre), ignorer
            if (Math.abs(x) < neutralZone && Math.abs(y) < neutralZone) {
                return;
            }

            // Déterminer la direction en fonction de l'inclinaison
            Direction newDirection = Direction.STOPPED;
            
            // Vérifier si l'inclinaison est suffisamment forte
            if (Math.abs(x) > tiltThreshold || Math.abs(y) > tiltThreshold) {
                // N'accepter que la composante dominante
                if (Math.abs(x) > Math.abs(y) * 1.2) { // 20% plus forte
                    // Inversion de UP et DOWN
                    newDirection = (x < 0) ? Direction.UP : Direction.DOWN;
                } else if (Math.abs(y) > Math.abs(x) * 1.2) { // 20% plus forte
                    newDirection = (y < 0) ? Direction.LEFT : Direction.RIGHT;
                }
            }

            if (newDirection != Direction.STOPPED) {
                direction = newDirection;
                lastDirectionChange = currentTime;
                Log.d(TAG, "Nouvelle direction: " + direction);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas besoin d'implémentation spécifique
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

        if (nextX < 0 || nextX >= 10 || nextY < 0 || nextY >= 10) {
            Log.d(TAG, "Collision avec un mur détectée");
            direction = Direction.STOPPED;
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
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));

            int pixelX = playerX * cellSize;
            int pixelY = playerY * cellSize;
            canvas.drawRect(pixelX, pixelY, pixelX + cellSize, pixelY + cellSize, paint);

            paint.setColor(Color.BLACK);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (obstacles[i][j]) {
                        canvas.drawRect(i * cellSize, j * cellSize,
                                i * cellSize + cellSize, j * cellSize + cellSize, paint);
                    }
                }
            }
        }
    }
}