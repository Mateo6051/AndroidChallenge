package com.example.goforit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NextLevelScreen extends AppCompatActivity {

    private static final String TAG = "NextLevelScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_next_level);

        // Récupérer le niveau
        int currentLevel = getIntent().getIntExtra("level", 1);
        Log.d(TAG, "Current level: " + currentLevel);

        // Mettre à jour le texte
        TextView title = findViewById(R.id.next_level_title);
        title.setText("Level " + currentLevel + " completed");

        // Bouton pour prendre une nouvelle photo
        Button takePhotoButton = findViewById(R.id.take_photo_button);
        takePhotoButton.setOnClickListener(v -> {
            Log.d(TAG, "Take Photo button clicked");
            Intent intent = new Intent(NextLevelScreen.this, TakePictureActivity.class);
            intent.putExtra("level", currentLevel + 1);
            startActivity(intent);
            finish();
        });

        // Bouton pour retourner au menu principal
        Button mainMenuButton = findViewById(R.id.main_menu_button);
        mainMenuButton.setOnClickListener(v -> {
            Log.d(TAG, "Main Menu button clicked");
            Intent intent = new Intent(NextLevelScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}