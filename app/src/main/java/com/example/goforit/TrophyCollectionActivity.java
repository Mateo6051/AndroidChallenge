package com.example.goforit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Activité pour afficher la collection de trophées du joueur
 */
public class TrophyCollectionActivity extends AppCompatActivity {
    private static final String TAG = "TrophyCollection";
    private TrophyManager trophyManager;
    private RecyclerView trophyRecyclerView;
    private LinearLayout emptyStateContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Plein écran
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        setContentView(R.layout.activity_trophy_collection);
        
        // Initialiser le gestionnaire de trophées
        trophyManager = new TrophyManager(this);
        
        // Configurer les vues
        trophyRecyclerView = findViewById(R.id.trophyRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        Button btnReturnToMenu = findViewById(R.id.btnReturnToMenu);
        
        // Configurer le RecyclerView
        trophyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Charger les trophées
        loadTrophies();
        
        // Configurer le bouton de retour
        btnReturnToMenu.setOnClickListener(v -> {
            Log.d(TAG, "Retour au menu principal");
            Intent intent = new Intent(TrophyCollectionActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Charger et afficher les trophées
     */
    private void loadTrophies() {
        List<Trophy> trophies = trophyManager.getTrophies();
        Log.d(TAG, "Trophées chargés : " + trophies.size());
        
        if (trophies.isEmpty()) {
            // Afficher l'état vide
            trophyRecyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            // Afficher les trophées
            trophyRecyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            
            TrophyAdapter adapter = new TrophyAdapter(trophies);
            trophyRecyclerView.setAdapter(adapter);
        }
    }
    
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TrophyCollectionActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
} 