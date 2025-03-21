package com.example.goforit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TakePictureActivity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Button btnStartTheGame;
    private Bitmap imageBitmap; // Stocker la photo pour la passer à GameView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        btnStartTheGame = findViewById(R.id.btnStartTheGame);
        btnStartTheGame.setText("Commencer"); // Renommer le bouton
        btnStartTheGame.setVisibility(View.GONE);

        imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.GONE);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);

        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());

        // Ajouter le listener pour lancer GameView
        btnStartTheGame.setOnClickListener(v -> {
            Intent intent = new Intent(TakePictureActivity.this, GameActivity.class); // Nouvelle activité pour GameView
            intent.putExtra("background_image", imageBitmap); // Passer l'image
            startActivity(intent);
            finish(); // Terminer TakePictureActivity
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
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data"); // Stocker l'image
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(imageBitmap);
            btnStartTheGame.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }
}