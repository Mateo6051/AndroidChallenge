package com.example.goforit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private int playerX = 0; // Position x du joueur dans la grille (0-9)
    private int playerY = 0; // Position y du joueur dans la grille (0-9)
    private int valeur_y; // Gardé pour compatibilité avec MainActivity
    private boolean[][] obstacles = new boolean[10][10]; // Grille 10x10 pour obstacles
    private Direction direction = Direction.RIGHT; // Direction initiale

    // Variables pour détecter le swipe
    private float touchX, touchY; // Point de départ du swipe
    private static final int SWIPE_THRESHOLD = 50; // Distance minimale pour valider un swipe

    // Contrôle de la vitesse
    private int moveCounter = 0; // Compteur pour ralentir le déplacement
    private static final int MOVE_DELAY = 5; // Plus cette valeur est grande, plus le déplacement est lent

    // Enum pour les directions
    private enum Direction {
        UP, DOWN, LEFT, RIGHT, STOPPED
    }

    public GameView(Context context, int valeur_y) {
        super(context);
        this.valeur_y = valeur_y;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        // Initialisation des obstacles (exemple)
        obstacles[3][4] = true; // Obstacle en (3,4)
        obstacles[7][8] = true; // Obstacle en (7,8)
        obstacles[9][9] = true;
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

    // Gestion des swipes pour changer la direction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (direction != Direction.STOPPED) {
            return true; // Ignore les swipes pendant le mouvement
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

    // Logique de déplacement et détection des obstacles
    public void update() {
        if (direction == Direction.STOPPED) {
            return; // Pas de mouvement si arrêté
        }

        // Ralentir le déplacement avec un compteur
        moveCounter++;
        if (moveCounter < MOVE_DELAY) {
            return; // Ne bouge pas encore
        }
        moveCounter = 0; // Réinitialiser le compteur après un déplacement

        // Mouvement continu dans la direction actuelle
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

        // Vérification des limites de la grille (0-9)
        if (nextX < 0 || nextX >= 10 || nextY < 0 || nextY >= 10) {
            direction = Direction.STOPPED; // Arrêt si hors grille
            return;
        }

        // Vérification des obstacles
        if (obstacles[nextX][nextY]) {
            direction = Direction.STOPPED; // Arrêt si collision
            return;
        }

        // Mise à jour de la position si aucun obstacle ou bord
        playerX = nextX;
        playerY = nextY;
    }

    // Affichage (laissé pour les autres membres)
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));

            // Dessiner le joueur
            int pixelX = playerX * 40;
            int pixelY = playerY * 40;
            canvas.drawRect(pixelX, pixelY, pixelX + 40, pixelY + 40, paint);

            // Dessiner les obstacles
            paint.setColor(Color.BLACK);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (obstacles[i][j]) {
                        canvas.drawRect(i * 40, j * 40, i * 40 + 40, j * 40 + 40, paint);
                    }
                }
            }
        }
    }
}