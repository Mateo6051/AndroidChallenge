package com.example.goforit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TakePictureActivity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private RelativeLayout placeholderContainer;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Button btnStartTheGame;
    private Button btnTakePhoto;
    private CardView takePhotoCardView;
    private CardView startGameCardView;
    private Bitmap imageBitmap;
    private int level;
    private static final int MAX_LEVEL = 10; // Nombre maximum de niveaux

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Plein écran
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_take_picture);

        level = getIntent().getIntExtra("level", 1); // Récupérer le niveau

        // Mettre à jour les éléments UI avec le niveau actuel
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSubtitle.setText("Niveau " + level);
        
        TextView levelProgressText = findViewById(R.id.levelProgressText);
        levelProgressText.setText(level + "/" + MAX_LEVEL);

        // Initialiser les boutons et cartes
        btnStartTheGame = findViewById(R.id.btnStartTheGame);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        startGameCardView = (CardView) btnStartTheGame.getParent();
        takePhotoCardView = (CardView) btnTakePhoto.getParent();
        
        // Définir le texte du bouton de démarrage
        btnStartTheGame.setText("Commencer le niveau " + level);
        startGameCardView.setVisibility(View.GONE);

        // Initialiser les éléments d'image
        imageView = findViewById(R.id.imageView);
        placeholderContainer = findViewById(R.id.placeholderContainer);
        
        // Animer le conteneur de l'image
        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(1000);
        findViewById(R.id.imageContainer).startAnimation(pulse);

        // Configurer les listeners de boutons
        btnTakePhoto.setOnClickListener(v -> {
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            scaleAnimation.setDuration(300);
            takePhotoCardView.startAnimation(scaleAnimation);
            checkCameraPermission();
        });

        btnStartTheGame.setOnClickListener(v -> {
            if (imageBitmap == null) {
                Toast.makeText(this, "Veuillez d'abord prendre une photo", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            scaleAnimation.setDuration(300);
            startGameCardView.startAnimation(scaleAnimation);
            
            Intent intent = new Intent(TakePictureActivity.this, GameActivity.class);
            intent.putExtra("background_image", imageBitmap);
            intent.putExtra("level", level);
            intent.putExtra("gridSize", 10 + (level - 1) * 2); // Taille augmente de 2 par niveau
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Aucune application d'appareil photo disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            
            // Masquer le placeholder et afficher l'image
            placeholderContainer.setVisibility(View.GONE);
            imageView.setImageBitmap(imageBitmap);
            
            // Animer l'apparition de l'image
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeIn.setDuration(500);
            imageView.startAnimation(fadeIn);
            
            // Afficher le bouton de démarrage
            startGameCardView.setVisibility(View.VISIBLE);
            Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            slideUp.setDuration(500);
            startGameCardView.startAnimation(slideUp);
            
            // Mettre à jour le texte du bouton de prise de photo
            btnTakePhoto.setText("Reprendre une photo");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permission d'accès à l'appareil photo refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}