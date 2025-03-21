    package com.example.goforit;

    import android.graphics.Canvas;
    import android.view.SurfaceHolder;

    public class GameThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private GameView gameView;

        private boolean running;

        private Canvas canvas;

        public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
            super();
            this.surfaceHolder = surfaceHolder;
            this.gameView = gameView;
        }

        @Override
        public void run() {
            long previousTime = System.currentTimeMillis();
            long sleepTime;
            while (running) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - previousTime;
                previousTime = currentTime;

                canvas = null;
                try {
                    canvas = this.surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        this.gameView.update();
                        this.gameView.draw(canvas);
                    }
                } catch (Exception e) {
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                sleepTime = 16 - elapsedTime;  // Target ~60 FPS (1000/60 = 16.67 ms per frame)
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setRunning(boolean isRunning) {
            running = isRunning;
        }
    }