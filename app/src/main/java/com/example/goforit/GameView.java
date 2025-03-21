package com.example.goforit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private int playerX = 0;
    private int playerY = 0;
    private int valeur_y;
    private boolean[][] obstacles; // Grille d'obstacles de taille variable
    private Direction direction = Direction.RIGHT;
    private Bitmap backgroundImage;

    private float touchX, touchY;
    private static final int SWIPE_THRESHOLD = 50;

    private int moveCounter = 0;
    private static final int MOVE_DELAY = 5;

    private int cellSize;
    private int gridSize; // Taille de la grille (nombre de cases)

    private enum Direction {
        UP, DOWN, LEFT, RIGHT, STOPPED
    }

    public GameView(Context context, int valeur_y, Bitmap backgroundImage, int gridSize) {
        super(context);
        this.valeur_y = valeur_y;
        this.backgroundImage = backgroundImage;
        this.gridSize = gridSize; // Taille de la grille passée en paramètre
        this.obstacles = new boolean[gridSize][gridSize]; // Initialiser la grille avec la taille donnée
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        // Initialisation des obstacles (exemple pour une grille quelconque)
        if (gridSize >= 10) { // S'assurer que les indices sont valides
            obstacles[3][4] = true;
            obstacles[7][8] = true;
            obstacles[gridSize - 1][gridSize - 1] = true; // Sortie au coin inférieur droit
        }

        calculateCellSize();
    }

    private void calculateCellSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth, screenHeight) / gridSize; // Ajuster la taille des cases
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (direction != Direction.STOPPED) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - touchX;
                float deltaY = event.getY() - touchY;

                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0) {
                            direction = Direction.RIGHT;
                        } else {
                            direction = Direction.LEFT;
                        }
                    }
                } else {
                    if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                        if (deltaY > 0) {
                            direction = Direction.DOWN;
                        } else {
                            direction = Direction.UP;
                        }
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
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

        // Vérifier les limites avec la taille de grille dynamique
        if (nextX < 0 || nextX >= gridSize || nextY < 0 || nextY >= gridSize) {
            direction = Direction.STOPPED;
            return;
        }

        if (obstacles[nextX][nextY]) {
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
            // Dessiner l'image de fond en l'étirant pour couvrir toute la grille
            if (backgroundImage != null) {
                int totalSize = cellSize * gridSize; // Taille totale de la grille
                Rect destRect = new Rect(0, 0, totalSize, totalSize);
                canvas.drawBitmap(backgroundImage, null, destRect, null);
            } else {
                canvas.drawColor(Color.WHITE);
            }

            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));

            int pixelX = playerX * cellSize;
            int pixelY = playerY * cellSize;
            canvas.drawRect(pixelX, pixelY, pixelX + cellSize, pixelY + cellSize, paint);

            paint.setColor(Color.BLACK);
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (obstacles[i][j]) {
                        canvas.drawRect(i * cellSize, j * cellSize,
                                i * cellSize + cellSize, j * cellSize + cellSize, paint);
                    }
                }
            }
        }
    }
}