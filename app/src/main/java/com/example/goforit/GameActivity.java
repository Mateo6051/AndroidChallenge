package com.example.goforit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Bitmap backgroundImage = getIntent().getParcelableExtra("background_image");
        int gridSize = 20; // Taille par défaut, modifiable
        GameView gameView = new GameView(this, 0, backgroundImage, gridSize);
        setContentView(gameView);
    }
}