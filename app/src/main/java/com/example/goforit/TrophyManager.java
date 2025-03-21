package com.example.goforit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Gestionnaire pour sauvegarder et charger les trophées de l'utilisateur
 */
public class TrophyManager {
    private static final String TAG = "TrophyManager";
    private static final String PREFS_NAME = "TrophyPrefs";
    private static final String KEY_TROPHIES = "trophies";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final Context context;
    private final List<Trophy> trophies = new ArrayList<>();

    public TrophyManager(Context context) {
        this.context = context;
        loadTrophies();
    }

    /**
     * Ajouter un nouveau trophée
     * @param level Niveau terminé
     * @return Le nouveau trophée créé
     */
    public Trophy addTrophy(int level) {
        // Vérifier si le trophée existe déjà
        for (Trophy trophy : trophies) {
            if (trophy.getLevel() == level) {
                return trophy; // Trophée déjà existant
            }
        }

        // Créer un nouveau trophée
        Trophy.Type type = Trophy.getTrophyTypeForLevel(level);
        String title = "Niveau " + level + " terminé";
        String description = "Vous avez terminé le niveau " + level + " avec succès !";
        Date now = new Date();

        Trophy trophy = new Trophy(level, type, title, description, now);
        trophies.add(trophy);
        saveTrophies();
        
        return trophy;
    }

    /**
     * Obtenir tous les trophées
     * @return Liste des trophées
     */
    public List<Trophy> getTrophies() {
        return trophies;
    }

    /**
     * Sauvegarder les trophées en SharedPreferences
     */
    private void saveTrophies() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray jsonArray = new JSONArray();

            for (Trophy trophy : trophies) {
                JSONObject jsonTrophy = new JSONObject();
                jsonTrophy.put("level", trophy.getLevel());
                jsonTrophy.put("type", trophy.getType().name());
                jsonTrophy.put("title", trophy.getTitle());
                jsonTrophy.put("description", trophy.getDescription());
                jsonTrophy.put("date", DATE_FORMAT.format(trophy.getDateObtained()));
                jsonArray.put(jsonTrophy);
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_TROPHIES, jsonArray.toString());
            editor.apply();
            Log.d(TAG, "Trophées sauvegardés : " + trophies.size());
        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors de la sauvegarde des trophées", e);
        }
    }

    /**
     * Charger les trophées depuis SharedPreferences
     */
    private void loadTrophies() {
        trophies.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String trophiesJson = prefs.getString(KEY_TROPHIES, "[]");

        try {
            JSONArray jsonArray = new JSONArray(trophiesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonTrophy = jsonArray.getJSONObject(i);
                
                int level = jsonTrophy.getInt("level");
                Trophy.Type type = Trophy.Type.valueOf(jsonTrophy.getString("type"));
                String title = jsonTrophy.getString("title");
                String description = jsonTrophy.getString("description");
                Date date = DATE_FORMAT.parse(jsonTrophy.getString("date"));

                Trophy trophy = new Trophy(level, type, title, description, date);
                trophies.add(trophy);
            }
            Log.d(TAG, "Trophées chargés : " + trophies.size());
        } catch (JSONException | ParseException e) {
            Log.e(TAG, "Erreur lors du chargement des trophées", e);
        }
    }
} 