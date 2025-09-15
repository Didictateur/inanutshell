package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/**
 * Gestionnaire des favoris utilisant SharedPreferences pour la persistance
 */
public class FavoritesManager {
    
    private static final String PREFS_NAME = "favorites_prefs";
    private static final String FAVORITES_KEY = "favorite_recipes";
    
    private static FavoritesManager instance;
    private SharedPreferences preferences;
    private Set<String> favoriteRecipeIds;
    
    private FavoritesManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFavorites();
    }
    
    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }
    
    private void loadFavorites() {
        favoriteRecipeIds = new HashSet<>(preferences.getStringSet(FAVORITES_KEY, new HashSet<>()));
    }
    
    private void saveFavorites() {
        preferences.edit()
            .putStringSet(FAVORITES_KEY, favoriteRecipeIds)
            .apply();
    }
    
    public boolean isFavorite(String recipeId) {
        return favoriteRecipeIds.contains(recipeId);
    }
    
    public void addToFavorites(String recipeId) {
        favoriteRecipeIds.add(recipeId);
        saveFavorites();
        android.util.Log.d("FavoritesManager", "Added recipe " + recipeId + " to favorites");
    }
    
    public void removeFromFavorites(String recipeId) {
        favoriteRecipeIds.remove(recipeId);
        saveFavorites();
        android.util.Log.d("FavoritesManager", "Removed recipe " + recipeId + " from favorites");
    }
    
    public void toggleFavorite(String recipeId) {
        if (isFavorite(recipeId)) {
            removeFromFavorites(recipeId);
        } else {
            addToFavorites(recipeId);
        }
    }
    
    public Set<String> getFavoriteRecipeIds() {
        return new HashSet<>(favoriteRecipeIds);
    }
    
    public int getFavoriteCount() {
        return favoriteRecipeIds.size();
    }
    
    public void clearAllFavorites() {
        favoriteRecipeIds.clear();
        saveFavorites();
        android.util.Log.d("FavoritesManager", "Cleared all favorites");
    }
}
