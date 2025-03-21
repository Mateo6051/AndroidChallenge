package com.example.goforit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
    private GameThread thread;
    private int playerX;
    private int playerY;
    private Direction direction = Direction.STOPPED;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float tiltThreshold = 2.0f;
    private float neutralZone = 1.0f;
    private long lastDirectionChange = 0;
    private static final long DIRECTION_CHANGE_COOLDOWN = 500;
    private float[] lastAccValues = new float[3];
    private static final float ALPHA = 0.8f;
    private int moveCounter = 0;
    private static final int MOVE_DELAY = 3;
    private int cellSize;
    private Bitmap stoneBitmapStone;
    private Bitmap stoneBitmapBall;

    private Bitmap doorBitmap;
    private int gridSize;
    private Bitmap backgroundImage;
    private int level;
    private int rotationAngle = 0;
    private Maze maze;
    private int offsetX;
    private int offsetY;
    private float drawX;
    private float drawY;
    private static final float MOVE_SPEED = 20.0f;
    private float currentRotationAngle = 0;
    private static final float ROTATION_SPEED = 100.0f;

    private Direction lastMoveDirection = Direction.STOPPED;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT, STOPPED
    }

    public GameView(Context context, Bitmap backgroundImage, int gridSize) {
        super(context);
        maze = new Maze();
        maze.generateMaze(gridSize, 8);

        playerX = maze.getStart().x;
        playerY = maze.getStart().y;
        drawX = playerX * cellSize;
        drawY = playerY * cellSize;
        this.backgroundImage = backgroundImage;
        this.gridSize = gridSize;
        this.level = ((Activity) context).getIntent().getIntExtra("level", 1);
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        calculateCellSize();
    }

    private void calculateCellSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int availableSize = Math.min(screenWidth, screenHeight) * 90 / 100;
        cellSize = availableSize / (gridSize + 2);

        int totalSize = cellSize * (gridSize + 2);
        offsetX = (screenWidth - totalSize) / 2;
        offsetY = (screenHeight - totalSize) / 2;
    }

    public void update() {
        if (direction != Direction.STOPPED) {
            moveCounter++;
            if (moveCounter >= MOVE_DELAY) {
                moveCounter = 0;
                int nextX = playerX;
                int nextY = playerY;

                switch (direction) {
                    case UP: nextY--; break;
                    case DOWN: nextY++; break;
                    case LEFT: nextX--; break;
                    case RIGHT: nextX++; break;
                }

                if (nextX < 0 || nextX >= gridSize + 2 || nextY < 0 || nextY >= gridSize + 2 || maze.isObstacle(nextX, nextY)) {
                    direction = Direction.STOPPED;
                } else {
                    playerX = nextX;
                    playerY = nextY;
                    lastMoveDirection = direction;  // ✅ Store last valid direction

                    if (maze.isGoal(playerX, playerY)) {
                        direction = Direction.STOPPED;
                        Intent intent = new Intent(getContext(), NextLevelScreen.class);
                        intent.putExtra("level", level);
                        getContext().startActivity(intent);
                        ((Activity) getContext()).finish();
                        return;
                    }
                }
            }
        }

        // Smooth position interpolation towards target cell
        float targetX = playerX * cellSize;
        float targetY = playerY * cellSize;

        if (Math.abs(drawX - targetX) > MOVE_SPEED) {
            drawX += (drawX < targetX) ? MOVE_SPEED : -MOVE_SPEED;
        } else {
            drawX = targetX;
        }

        if (Math.abs(drawY - targetY) > MOVE_SPEED) {
            drawY += (drawY < targetY) ? MOVE_SPEED : -MOVE_SPEED;
        } else {
            drawY = targetY;
        }

        // Smooth rotation based on last valid direction (avoids flip/midway change)
        if (direction != Direction.STOPPED || isBallMoving()) {
            float perFrameRotationSpeed = 30.0f;
            Direction rotationDirection = (direction != Direction.STOPPED) ? direction : lastMoveDirection;

            switch (rotationDirection) {
                case LEFT:
                    currentRotationAngle -= perFrameRotationSpeed;
                    break;
                case RIGHT:
                    currentRotationAngle += perFrameRotationSpeed;
                    break;
                case UP:
                    currentRotationAngle -= perFrameRotationSpeed;
                    break;
                case DOWN:
                    currentRotationAngle += perFrameRotationSpeed;
                    break;
            }
        }

        // Normalize rotation angle
        if (currentRotationAngle >= 360) currentRotationAngle -= 360;
        if (currentRotationAngle < 0) currentRotationAngle += 360;
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        stoneBitmapStone = BitmapFactory.decodeResource(getResources(), R.drawable.stone_icon);
        stoneBitmapBall = BitmapFactory.decodeResource(getResources(), R.drawable.red_ball);
        doorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.door);
        int newWidth = cellSize;
        int newHeight = cellSize;
        stoneBitmapStone = Bitmap.createScaledBitmap(stoneBitmapStone, newWidth, newHeight, true);
        stoneBitmapBall = Bitmap.createScaledBitmap(stoneBitmapBall, newWidth, newHeight, true);
        doorBitmap = Bitmap.createScaledBitmap(doorBitmap, newWidth, newHeight, true);

        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

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

    private float[] filterAccValues(float[] values) {
        lastAccValues[0] = ALPHA * lastAccValues[0] + (1 - ALPHA) * values[0];
        lastAccValues[1] = ALPHA * lastAccValues[1] + (1 - ALPHA) * values[1];
        lastAccValues[2] = ALPHA * lastAccValues[2] + (1 - ALPHA) * values[2];
        return lastAccValues;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && direction == Direction.STOPPED && !isBallMoving()) {
            float[] filteredValues = filterAccValues(event.values);
            float x = filteredValues[0];
            float y = filteredValues[1];
            float z = filteredValues[2];
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastDirectionChange < DIRECTION_CHANGE_COOLDOWN) return;
            if (z < 6.0f) return;
            if (Math.abs(x) < neutralZone && Math.abs(y) < neutralZone) return;

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
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int totalSize = cellSize * (gridSize + 2);
        Rect destRect = new Rect(offsetX, offsetY, offsetX + totalSize, offsetY + totalSize);

        // Draw background image or white background
        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage, null, destRect, null);
        } else {
            canvas.drawColor(Color.WHITE);
        }

        Paint paint = new Paint();

        // Draw obstacles and exit
        for (int i = 0; i < gridSize + 2; i++) {
            for (int j = 0; j < gridSize + 2; j++) {
                int pixelX = offsetX + i * cellSize;
                int pixelY = offsetY + j * cellSize;

                if (maze.isGoal(i, j)) {
                    // Draw the door icon instead of a green square
                    canvas.drawBitmap(doorBitmap, pixelX, pixelY, null);
                } else if (maze.isObstacle(i, j)) {
                    // Draw obstacles with stone bitmap
                    canvas.drawBitmap(stoneBitmapStone, pixelX, pixelY, null);
                }
            }
        }

        // Draw the player with rotation
        int pixelX = offsetX + (int) drawX;
        int pixelY = offsetY + (int) drawY;

        canvas.save();
        canvas.rotate(currentRotationAngle, pixelX + cellSize / 2, pixelY + cellSize / 2);
        canvas.drawBitmap(stoneBitmapBall, pixelX, pixelY, null);
        canvas.restore();
    }

    private boolean isBallMoving() {
        float targetX = playerX * cellSize;
        float targetY = playerY * cellSize;
        return Math.abs(drawX - targetX) > 1 || Math.abs(drawY - targetY) > 1;
    }
}