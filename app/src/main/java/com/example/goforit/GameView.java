package com.example.goforit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.Random;
import java.util.Set;

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

    private Rect restartButton;
    private Paint buttonPaint;
    private Paint textPaint;

    private RectF restartButtonTouchArea;
    private Bitmap restartIcon;

    // Éléments pour le bouton solution
    private RectF solutionButtonTouchArea;
    private Bitmap solutionIcon;
    private boolean showSolution = false;

    // Constantes pour l'animation de pulsation
    private static final float PULSE_MIN = 0.95f;
    private static final float PULSE_MAX = 1.05f;
    private static final float PULSE_STEP = 0.01f;
    private static final long PULSE_INTERVAL = 16; // ~60 FPS

    private float pulseFactor = 1.0f;
    private boolean pulseGrowing = true;
    private long lastPulseTime = 0;

    // Variables pour l'effet de ripple
    private boolean isRippleActive = false;
    private float rippleRadius = 0;
    private float maxRippleRadius = 0;
    private long rippleStartTime = 0;
    private float rippleCenterX = 0;
    private float rippleCenterY = 0;
    private static final long RIPPLE_DURATION = 400; // durée en millisecondes
    private MediaPlayer moveSoundPlayer;

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

    private boolean isSoundPlaying = false;

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
                    lastMoveDirection = direction;

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

        if (direction != Direction.STOPPED || isBallMoving()) {
            float perFrameRotationSpeed = 30.0f;
            Direction rotationDirection = (direction != Direction.STOPPED) ? direction : lastMoveDirection;

            switch (rotationDirection) {
                case LEFT: currentRotationAngle -= perFrameRotationSpeed; break;
                case RIGHT: currentRotationAngle += perFrameRotationSpeed; break;
                case UP: currentRotationAngle -= perFrameRotationSpeed; break;
                case DOWN: currentRotationAngle += perFrameRotationSpeed; break;
            }
        }

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
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        sensorManager.unregisterListener(this);
        if (moveSoundPlayer != null) {
            moveSoundPlayer.release();
            moveSoundPlayer = null;
        }
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

                if (moveSoundPlayer != null) {
                    moveSoundPlayer.release();
                }
                int randomIndex = new Random().nextInt(3);
                moveSoundPlayer = MediaPlayer.create(getContext(), getMoveSoundResId(randomIndex));
                moveSoundPlayer.start();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (restartButtonTouchArea != null && restartButtonTouchArea.contains(x, y)) {
                // Démarrer l'effet de ripple
                startRippleEffect(x, y);

                // Redémarrer le jeu
                restartGame();
                return true;
            }

            // Reste du code pour les mouvements de balle...

            // Gestion du clic sur le bouton solution
            if (solutionButtonTouchArea != null && solutionButtonTouchArea.contains(x, y)) {
                // Simplement activer/désactiver l'affichage de la solution
                showSolution = !showSolution;
                return true;
            }
            
            // Si le joueur touche l'écran ailleurs, arrêter le mouvement ou changer de direction
            if (direction != Direction.STOPPED) {
                direction = Direction.STOPPED;
                return true;
            } else {
                // Si le joueur est déjà arrêté, on peut ajouter une logique pour commencer un mouvement
                // basé sur la position du toucher par rapport à la position du joueur
                int playerScreenX = offsetX + playerX * cellSize + cellSize / 2;
                int playerScreenY = offsetY + playerY * cellSize + cellSize / 2;

                // Calculer la direction basée sur l'angle entre la position du joueur et le toucher
                double angle = Math.toDegrees(Math.atan2(y - playerScreenY, x - playerScreenX));

                // Convertir l'angle en direction
                if (angle > -45 && angle <= 45) {
                    direction = Direction.RIGHT;
                } else if (angle > 45 && angle <= 135) {
                    direction = Direction.DOWN;
                } else if (angle > 135 || angle <= -135) {
                    direction = Direction.LEFT;
                } else {
                    direction = Direction.UP;
                }

                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void restartGame() {
        playerX = maze.getStart().x;
        playerY = maze.getStart().y;
        direction = Direction.STOPPED;
        rotationAngle = 0;

        // Ne pas réinitialiser l'affichage de la solution lors du redémarrage
        // Conserver l'état actuel de showSolution
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int totalSize = cellSize * (gridSize + 2);
        Rect destRect = new Rect(offsetX, offsetY, offsetX + totalSize, offsetY + totalSize);

        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage, null, destRect, null);
        } else {
            canvas.drawColor(Color.WHITE);
        }

        Paint paint = new Paint();

        // Dessiner le chemin solution si activé
        if (showSolution) {
            drawSolutionPath(canvas);
        }

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

        drawModernRestartButton(canvas);
        drawSolutionButton(canvas);
    }

    /**
     * Dessine un bouton de redémarrage flottant moderne et unique avec animation de pulsation
     * et effets visuels avancés
     */
    private void drawModernRestartButton(Canvas canvas) {
        // Mettre à jour l'animation de pulsation
        updatePulseAnimation();

        // Paramètres du bouton - simplifié mais moderne
        int buttonSize = (int) (cellSize * 1.7f);
        int margin = cellSize / 2;

        // Position du bouton dans le coin inférieur droit
        int centerX = canvas.getWidth() - buttonSize / 2 - margin;
        int centerY = canvas.getHeight() - buttonSize / 2 - margin * 2;

        // Animation de pulsation simple
        float pulseFactor = 1.0f + (float) Math.sin(System.currentTimeMillis() / 800.0) * 0.07f;
        int pulsingButtonSize = (int) (buttonSize * pulseFactor);

        // Position du texte sous le bouton
        float textY = centerY + buttonSize / 2 + cellSize / 2;

        // Zone tactile étendue pour inclure le texte
        restartButtonTouchArea = new RectF(
                centerX - buttonSize / 2 - margin / 3,
                centerY - buttonSize / 2 - margin / 3,
                centerX + buttonSize / 2 + margin / 3,
                textY + cellSize / 3
        );

        // Ombre simple pour effet de profondeur
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(50);
        canvas.drawCircle(centerX + 3, centerY + 3, pulsingButtonSize / 2 + 2, shadowPaint);

        // Fond du bouton avec dégradé moderne mais simple
        Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Dégradé radial pour un effet 3D subtil
        RadialGradient gradient = new RadialGradient(
                centerX - pulsingButtonSize / 5,
                centerY - pulsingButtonSize / 5,
                pulsingButtonSize,
                new int[]{
                        Color.parseColor("#3B82F6"),  // Bleu vif
                        Color.parseColor("#2563EB"),  // Bleu principal
                        Color.parseColor("#1D4ED8")   // Bleu foncé
                },
                new float[]{0.3f, 0.6f, 1.0f},
                Shader.TileMode.CLAMP
        );

        buttonPaint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, pulsingButtonSize / 2, buttonPaint);

        // Contour léger pour définition
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setAlpha(100);
        canvas.drawCircle(centerX, centerY, pulsingButtonSize / 2 - 1, strokePaint);

        // Dessiner l'icône de restart
        int iconSize = (int) (pulsingButtonSize * 0.7f);

        if (restartIcon == null || restartIcon.getWidth() != iconSize) {
            try {
                Drawable drawable = getContext().getDrawable(R.drawable.restart_icon_modern);
                if (drawable != null) {
                    Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                    Canvas iconCanvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, iconSize, iconSize);
                    drawable.draw(iconCanvas);
                    restartIcon = bitmap;
                }
            } catch (Exception e) {
                restartIcon = null;
            }
        }

        if (restartIcon != null) {
            Paint iconPaint = new Paint();
            float iconX = centerX - iconSize / 2;
            float iconY = centerY - iconSize / 2;
            canvas.drawBitmap(restartIcon, iconX, iconY, iconPaint);
        }

        // Texte RESTART clairement visible
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(cellSize / 3);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(3, 1, 1, Color.BLACK);

        // Dessiner le texte
        canvas.drawText("RESTART", centerX, textY, textPaint);

        // Effet ripple sur interaction
        updateAndDrawRippleEffect(canvas);
    }

    /**
     * Mise à jour de l'animation de pulsation pour le bouton
     */
    private void updatePulseAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPulseTime < PULSE_INTERVAL) {
            return; // Pas encore temps de mettre à jour
        }

        lastPulseTime = currentTime;

        if (pulseGrowing) {
            pulseFactor += PULSE_STEP;
            if (pulseFactor >= PULSE_MAX) {
                pulseFactor = PULSE_MAX;
                pulseGrowing = false;
            }
        } else {
            pulseFactor -= PULSE_STEP;
            if (pulseFactor <= PULSE_MIN) {
                pulseFactor = PULSE_MIN;
                pulseGrowing = true;
            }
        }
    }

    /**
     * Démarre l'effet de ripple à partir d'un point de toucher
     */
    private void startRippleEffect(float x, float y) {
        isRippleActive = true;
        rippleRadius = 0;
        rippleCenterX = x;
        rippleCenterY = y;
        rippleStartTime = System.currentTimeMillis();

        // Calculer le rayon maximum en fonction de la taille du bouton
        if (restartButtonTouchArea != null) {
            float width = restartButtonTouchArea.width();
            float height = restartButtonTouchArea.height();
            maxRippleRadius = (float) Math.sqrt(width * width + height * height) / 2;
        } else {
            maxRippleRadius = cellSize * 2;
        }
    }

    /**
     * Met à jour et dessine l'effet de ripple si actif
     */
    private void updateAndDrawRippleEffect(Canvas canvas) {
        if (!isRippleActive) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - rippleStartTime;
        if (elapsedTime > RIPPLE_DURATION) {
            isRippleActive = false;
            return;
        }

        // Calculer le rayon actuel et l'opacité en fonction du temps écoulé
        float progress = Math.min(1.0f, elapsedTime / (float) RIPPLE_DURATION);
        rippleRadius = maxRippleRadius * progress;
        int alpha = (int) (255 * (1.0f - progress));

        // Dessiner le cercle de ripple
        Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setColor(Color.WHITE);
        ripplePaint.setAlpha(alpha);
        ripplePaint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(rippleCenterX, rippleCenterY, rippleRadius, ripplePaint);
    }

    private boolean isBallMoving() {
        float targetX = playerX * cellSize;
        float targetY = playerY * cellSize;
        return Math.abs(drawX - targetX) > 1 || Math.abs(drawY - targetY) > 1;
    }

    private int getMoveSoundResId(int index) {
        switch (index) {
            case 0: return R.raw.move_sound_1;
            case 1: return R.raw.move_sound_2;
            case 2: return R.raw.move_sound_3;
            default: return R.raw.move_sound_1;
        }
    }


    /**
     * Dessine les marqueurs du chemin de solution
     */
    private void drawSolutionPath(Canvas canvas) {
        // Récupérer le chemin directement depuis la classe Maze
        Set<Point> path = maze.getPath();
        if (path == null || path.isEmpty()) return;

        // Créer un effet visuel pour le chemin - lignes connectant les points
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#FFD700"));  // Couleur or
        linePaint.setAlpha(80);  // Très transparent pour les lignes
        linePaint.setStrokeWidth(cellSize / 8);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStyle(Paint.Style.STROKE);

        // Dessiner des connexions entre les points adjacents
        for (Point point : path) {
            // Ne pas dessiner de lignes pour les obstacles (vérification de sécurité)
            if (maze.isObstacle(point.x, point.y)) continue;

            // Coordonnées du point actuel
            int x1 = offsetX + point.x * cellSize + cellSize / 2;
            int y1 = offsetY + point.y * cellSize + cellSize / 2;

            // Vérifier les quatre directions adjacentes
            Point[] adjacents = {
                new Point(point.x + 1, point.y),  // Droite
                new Point(point.x - 1, point.y),  // Gauche
                new Point(point.x, point.y + 1),  // Bas
                new Point(point.x, point.y - 1)   // Haut
            };

            for (Point adj : adjacents) {
                // Si le point adjacent fait partie du chemin, dessiner une ligne
                if (path.contains(adj) && !maze.isObstacle(adj.x, adj.y)) {
                    int x2 = offsetX + adj.x * cellSize + cellSize / 2;
                    int y2 = offsetY + adj.y * cellSize + cellSize / 2;

                    canvas.drawLine(x1, y1, x2, y2, linePaint);
                }
            }
        }

        // Dessiner les marqueurs pour chaque point du chemin
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.parseColor("#FFD700")); // Couleur or
        circlePaint.setAlpha(160); // Semi-transparent
        circlePaint.setStyle(Paint.Style.FILL);

        int markerSize = cellSize / 4;

        for (Point point : path) {
            // Ne pas dessiner de marqueur pour les obstacles
            if (maze.isObstacle(point.x, point.y)) continue;

            int pixelX = offsetX + point.x * cellSize + cellSize / 2;
            int pixelY = offsetY + point.y * cellSize + cellSize / 2;

            // Effet de halo autour du marqueur
            Paint haloPaint = new Paint();
            haloPaint.setColor(Color.parseColor("#FFD700")); // Or
            haloPaint.setAlpha(40); // Très transparent
            haloPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pixelX, pixelY, markerSize * 1.5f, haloPaint);

            // Dessiner un cercle pour chaque point du chemin
            canvas.drawCircle(pixelX, pixelY, markerSize/2, circlePaint);

            // Point central blanc pour meilleure visibilité
            Paint centerPaint = new Paint();
            centerPaint.setColor(Color.WHITE);
            centerPaint.setAlpha(180);
            centerPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pixelX, pixelY, markerSize/5, centerPaint);
        }

        // Mettre en évidence la position actuelle du joueur
        int playerPixelX = offsetX + playerX * cellSize + cellSize / 2;
        int playerPixelY = offsetY + playerY * cellSize + cellSize / 2;

        Paint playerMarkerPaint = new Paint();
        playerMarkerPaint.setColor(Color.parseColor("#4ADE80"));  // Vert vif
        playerMarkerPaint.setStyle(Paint.Style.STROKE);
        playerMarkerPaint.setStrokeWidth(3);
        canvas.drawCircle(playerPixelX, playerPixelY, markerSize, playerMarkerPaint);
        
        // Effet spécial pour la sortie
        Point goal = maze.getGoal();
        if (goal != null) {
            int goalX = offsetX + goal.x * cellSize + cellSize / 2;
            int goalY = offsetY + goal.y * cellSize + cellSize / 2;

            // Effet de pulse sur le point d'arrivée
            float pulseFactor = 1.0f + (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.2f;

            Paint goalPaint = new Paint();
            goalPaint.setColor(Color.parseColor("#FFD700")); // Or
            goalPaint.setStyle(Paint.Style.STROKE);
            goalPaint.setStrokeWidth(4);
            goalPaint.setAlpha(200);

            canvas.drawCircle(goalX, goalY, markerSize * 1.5f * pulseFactor, goalPaint);

            // Texte "SORTIE" près du point d'arrivée
            Paint exitTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            exitTextPaint.setColor(Color.parseColor("#FFD700")); // Or
            exitTextPaint.setTextSize(cellSize / 3);
            exitTextPaint.setTextAlign(Paint.Align.CENTER);
            exitTextPaint.setShadowLayer(3, 1, 1, Color.BLACK);

            // Calculer la position du texte en fonction de l'emplacement de la sortie
            float textX = goalX;
            float textY = goalY + cellSize;
            
            // Ajuster la position selon l'emplacement de la sortie
            if (goal.y == 0) { // Sortie en haut
                textY = goalY - cellSize / 2;
            } else if (goal.y >= gridSize) { // Sortie en bas
                textY = goalY + cellSize;
            } else if (goal.x == 0) { // Sortie à gauche
                textY = goalY - cellSize / 2;
                textX = goalX + cellSize / 2;
            } else {
                textY = goalY - cellSize / 2; // Sortie à droite
                textX = goalX - cellSize / 2;
            }

            // Dessiner le texte
            canvas.drawText("SORTIE", textX, textY, exitTextPaint);
        }
    }

    /**
     * Dessine le bouton de solution
     */
    private void drawSolutionButton(Canvas canvas) {
        // Paramètres du bouton
        int buttonSize = (int)(cellSize * 1.7f);
        int margin = cellSize / 2;

        // Position du bouton dans le coin inférieur gauche (opposé au bouton restart)
        int centerX = buttonSize/2 + margin;
        int centerY = canvas.getHeight() - buttonSize/2 - margin * 2;

        // Animation de pulsation simple
        float pulseFactor = 1.0f + (float) Math.sin(System.currentTimeMillis() / 800.0) * 0.07f;
        int pulsingButtonSize = (int)(buttonSize * pulseFactor);

        // Position du texte sous le bouton
        float textY = centerY + buttonSize/2 + cellSize/2;

        // Zone tactile
        solutionButtonTouchArea = new RectF(
            centerX - buttonSize/2 - margin/3,
            centerY - buttonSize/2 - margin/3,
            centerX + buttonSize/2 + margin/3,
            textY + cellSize/3
        );

        // Ombre pour effet de profondeur
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(50);
        canvas.drawCircle(centerX + 3, centerY + 3, pulsingButtonSize/2 + 2, shadowPaint);

        // Fond du bouton avec dégradé
        Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Couleur du bouton différente selon l'état (activé/désactivé)
        int[] colors;
        if (showSolution) {
            // Couleur dorée quand activé
            colors = new int[] {
                Color.parseColor("#FFD700"),  // Or clair
                Color.parseColor("#FFC125"),  // Or moyen
                Color.parseColor("#DAA520")   // Or foncé
            };
        } else {
            // Couleur verte quand désactivé
            colors = new int[] {
                Color.parseColor("#4ADE80"),  // Vert clair
                Color.parseColor("#22C55E"),  // Vert moyen
                Color.parseColor("#16A34A")   // Vert foncé
            };
        }

        // Dégradé radial
        RadialGradient gradient = new RadialGradient(
            centerX - pulsingButtonSize/5,
            centerY - pulsingButtonSize/5,
            pulsingButtonSize,
            colors,
            new float[] {0.3f, 0.6f, 1.0f},
            Shader.TileMode.CLAMP
        );

        buttonPaint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, pulsingButtonSize/2, buttonPaint);

        // Contour léger
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setAlpha(100);
        canvas.drawCircle(centerX, centerY, pulsingButtonSize/2 - 1, strokePaint);

        // Dessiner l'icône
        int iconSize = (int)(pulsingButtonSize * 0.7f);

        if (solutionIcon == null || solutionIcon.getWidth() != iconSize) {
            try {
                Drawable drawable = getContext().getDrawable(R.drawable.solution_icon);
                if (drawable != null) {
                    Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                    Canvas iconCanvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, iconSize, iconSize);
                    drawable.draw(iconCanvas);
                    solutionIcon = bitmap;
                }
            } catch (Exception e) {
                solutionIcon = null;
            }
        }

        if (solutionIcon != null) {
            Paint iconPaint = new Paint();
            float iconX = centerX - iconSize/2;
            float iconY = centerY - iconSize/2;
            canvas.drawBitmap(solutionIcon, iconX, iconY, iconPaint);
        }

        // Texte "SOLUTION"
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(cellSize/3);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(3, 1, 1, Color.BLACK);

        // Dessiner le texte
        canvas.drawText(showSolution ? "CACHER" : "SOLUTION", centerX, textY, textPaint);
    }
}