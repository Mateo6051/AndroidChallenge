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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NextLevelScreen extends AppCompatActivity {

    private static final String TAG = "NextLevelScreen";
    private TrophyManager trophyManager;
    private int currentLevel;
    private Trophy trophy;
    
    // Nombre maximum de niveaux dans le jeu
    private static final int MAX_LEVELS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_next_level);
        
        // Initialiser le gestionnaire de trophées
        trophyManager = new TrophyManager(this);

        // Récupérer le niveau
        currentLevel = getIntent().getIntExtra("level", 1);
        Log.d(TAG, "Current level: " + currentLevel);

        // Créer le trophée pour ce niveau
        trophy = trophyManager.addTrophy(currentLevel);

        // Mettre à jour le texte
        TextView title = findViewById(R.id.next_level_title);
        title.setText("Niveau " + currentLevel + " terminé");
        
        // Configurer le trophée
        setupTrophy();
        
        // Configurer la progression
        setupProgress();
        
        // Configurer les félicitations selon le niveau
        setupCongratulations();

        // Bouton pour le niveau suivant ou terminer
        Button takePhotoButton = findViewById(R.id.take_photo_button);
        Button mainMenuButton = findViewById(R.id.main_menu_button);
        
        // Gestion des boutons en fonction du niveau
        if (currentLevel >= MAX_LEVELS) {
            // Au dernier niveau, on n'a besoin que du bouton "Terminer"
            takePhotoButton.setText("Terminer");
            // Cacher le bouton "Menu principal" car redondant
            mainMenuButton.setVisibility(View.GONE);
        } else {
            // Pour les autres niveaux, garder les deux boutons
            takePhotoButton.setText("Niveau suivant");
            mainMenuButton.setVisibility(View.VISIBLE);
        }
        
        takePhotoButton.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked");
            
            // Si nous avons atteint le niveau maximum, retourner au menu principal
            if (currentLevel >= MAX_LEVELS) {
                Intent intent = new Intent(NextLevelScreen.this, MainActivity.class);
                startActivity(intent);
            } else {
                // Sinon, passer au niveau suivant
                Intent intent = new Intent(NextLevelScreen.this, TakePictureActivity.class);
                intent.putExtra("level", currentLevel + 1);
                startActivity(intent);
            }
            finish();
        });

        // Bouton pour retourner au menu principal
        mainMenuButton.setOnClickListener(v -> {
            Log.d(TAG, "Main Menu button clicked");
            Intent intent = new Intent(NextLevelScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Configure l'affichage du trophée
     */
    private void setupTrophy() {
        ImageView trophyImage = findViewById(R.id.trophyImage);
        TextView levelNumber = findViewById(R.id.levelNumber);
        
        // Définir l'icône de trophée en fonction du type
        switch (trophy.getType()) {
            case GOLD:
                trophyImage.setImageResource(R.drawable.trophy_gold);
                break;
            case SILVER:
                trophyImage.setImageResource(R.drawable.trophy_silver);
                break;
            case BRONZE:
                trophyImage.setImageResource(R.drawable.trophy_bronze);
                break;
        }
        
        // Définir le numéro de niveau
        levelNumber.setText(String.valueOf(currentLevel));
        
        // Animer l'apparition du trophée
        View trophyContainer = findViewById(R.id.trophyContainer);
        Animation trophyAnimation = AnimationUtils.loadAnimation(this, R.anim.trophy_appear);
        trophyContainer.startAnimation(trophyAnimation);
    }
    
    /**
     * Configure l'affichage de la progression
     */
    private void setupProgress() {
        ProgressBar progressBar = findViewById(R.id.levelProgress);
        TextView progressText = findViewById(R.id.progressText);
        
        int progress = Math.min(100, (currentLevel * 100) / MAX_LEVELS);
        
        // Mettre à jour la barre et le texte
        progressBar.setProgress(progress);
        progressText.setText(progress + "%");
    }
    
    /**
     * Configure le message de félicitations en fonction du niveau
     */
    private void setupCongratulations() {
        TextView congratsText = findViewById(R.id.congratsText);
        TextView detailText = findViewById(R.id.detailText);
        
        if (currentLevel >= MAX_LEVELS) {
            // Message spécial pour avoir terminé tous les niveaux
            congratsText.setText("FÉLICITATIONS !");
            detailText.setText("Vous avez terminé tous les niveaux du jeu ! Vous êtes un véritable champion !");
            
            // Ajouter une animation spéciale pour la célébration finale
            Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
            congratsText.startAnimation(pulseAnimation);
        } else {
            // Message standard pour les niveaux intermédiaires
            congratsText.setText("Bravo !");
            detailText.setText("Vous avez terminé ce niveau avec succès !");
        }
    }
}