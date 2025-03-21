package com.example.goforit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TrophyManager trophyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        trophyManager = new TrophyManager(this);

        ImageView logoImage = findViewById(R.id.logoImage);
        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(1000);
        logoImage.startAnimation(pulse);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnViewTrophies = findViewById(R.id.btnViewTrophies);

        btnStart.setOnClickListener(v -> {
            Log.d(TAG, "Démarrage du jeu");
            Intent intent = new Intent(MainActivity.this, TakePictureActivity.class);
            intent.putExtra("level", 1);
            startActivity(intent);
        });

        btnViewTrophies.setOnClickListener(v -> {
            Log.d(TAG, "Affichage des trophées");
            Intent intent = new Intent(MainActivity.this, TrophyCollectionActivity.class);
            startActivity(intent);
        });

        displayTrophyPreviews();
    }

    /**
     * Affiche un aperçu des trophées gagnés
     */
    private void displayTrophyPreviews() {
        List<Trophy> trophies = trophyManager.getTrophies();

        ImageView trophyPreview1 = findViewById(R.id.trophyPreview1);
        ImageView trophyPreview2 = findViewById(R.id.trophyPreview2);
        ImageView trophyPreview3 = findViewById(R.id.trophyPreview3);

        trophyPreview1.setVisibility(View.INVISIBLE);
        trophyPreview2.setVisibility(View.INVISIBLE);
        trophyPreview3.setVisibility(View.INVISIBLE);

        for (Trophy trophy : trophies) {
            if (trophy.getType() == Trophy.Type.GOLD) {
                trophyPreview1.setVisibility(View.VISIBLE);
            } else if (trophy.getType() == Trophy.Type.SILVER) {
                trophyPreview2.setVisibility(View.VISIBLE);
            } else if (trophy.getType() == Trophy.Type.BRONZE) {
                trophyPreview3.setVisibility(View.VISIBLE);
            }
        }
    }
}