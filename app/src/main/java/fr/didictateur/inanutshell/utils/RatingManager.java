package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des notations utilisant SharedPreferences pour la persistance
 */
public class RatingManager {
    
    private static final String PREFS_NAME = "ratings_prefs";
    private static final String RATINGS_PREFIX = "rating_";
    
    private static RatingManager instance;
    private SharedPreferences preferences;
    
    private RatingManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized RatingManager getInstance(Context context) {
        if (instance == null) {
            instance = new RatingManager(context);
        }
        return instance;
    }
    
    /**
     * Obtenir la note d'une recette (0 si non notée)
     */
    public float getRating(String recipeId) {
        return preferences.getFloat(RATINGS_PREFIX + recipeId, 0f);
    }
    
    /**
     * Définir la note d'une recette (1-5 étoiles)
     */
    public void setRating(String recipeId, float rating) {
        if (rating < 0f || rating > 5f) {
            android.util.Log.w("RatingManager", "Rating should be between 0 and 5, got: " + rating);
            rating = Math.max(0f, Math.min(5f, rating));
        }
        
        preferences.edit()
            .putFloat(RATINGS_PREFIX + recipeId, rating)
            .apply();
        
        android.util.Log.d("RatingManager", "Set rating " + rating + " for recipe " + recipeId);
    }
    
    /**
     * Supprimer la note d'une recette
     */
    public void removeRating(String recipeId) {
        preferences.edit()
            .remove(RATINGS_PREFIX + recipeId)
            .apply();
        
        android.util.Log.d("RatingManager", "Removed rating for recipe " + recipeId);
    }
    
    /**
     * Vérifier si une recette a été notée
     */
    public boolean hasRating(String recipeId) {
        return preferences.contains(RATINGS_PREFIX + recipeId);
    }
    
    /**
     * Obtenir toutes les recettes notées avec leurs notes
     */
    public Map<String, Float> getAllRatings() {
        Map<String, Float> ratings = new HashMap<>();
        Map<String, ?> allPrefs = preferences.getAll();
        
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(RATINGS_PREFIX)) {
                String recipeId = key.substring(RATINGS_PREFIX.length());
                Float rating = (Float) entry.getValue();
                if (rating != null) {
                    ratings.put(recipeId, rating);
                }
            }
        }
        
        return ratings;
    }
    
    /**
     * Obtenir le nombre de recettes notées
     */
    public int getRatedRecipesCount() {
        return getAllRatings().size();
    }
    
    /**
     * Obtenir la note moyenne de toutes les recettes notées
     */
    public float getAverageRating() {
        Map<String, Float> allRatings = getAllRatings();
        if (allRatings.isEmpty()) {
            return 0f;
        }
        
        float sum = 0f;
        for (Float rating : allRatings.values()) {
            sum += rating;
        }
        
        return sum / allRatings.size();
    }
    
    /**
     * Effacer toutes les notations
     */
    public void clearAllRatings() {
        SharedPreferences.Editor editor = preferences.edit();
        Map<String, ?> allPrefs = preferences.getAll();
        
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(RATINGS_PREFIX)) {
                editor.remove(key);
            }
        }
        
        editor.apply();
        android.util.Log.d("RatingManager", "Cleared all ratings");
    }
}
