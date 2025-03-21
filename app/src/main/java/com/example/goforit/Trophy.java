package com.example.goforit;

import java.util.Date;

/**
 * Classe représentant un trophée dans le jeu
 */
public class Trophy {
    public enum Type {
        BRONZE, SILVER, GOLD
    }

    private final int level;
    private final Type type;
    private final String title;
    private final String description;
    private final Date dateObtained;

    public Trophy(int level, Type type, String title, String description, Date dateObtained) {
        this.level = level;
        this.type = type;
        this.title = title;
        this.description = description;
        this.dateObtained = dateObtained;
    }

    public int getLevel() {
        return level;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDateObtained() {
        return dateObtained;
    }

    /**
     * Détermine le type de trophée en fonction du niveau
     * @param level Niveau terminé
     * @return Type de trophée correspondant
     */
    public static Type getTrophyTypeForLevel(int level) {
        if (level >= 7) {
            return Type.GOLD;
        } else if (level >= 3) {
            return Type.SILVER;
        } else {
            return Type.BRONZE;
        }
    }
} 