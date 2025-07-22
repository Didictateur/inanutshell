package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {
    private static final String FAVORITE_FOLDERS_KEY = "favorite_folders";
    private static final String FAVORITE_RECIPES_KEY = "favorite_recipes";
    
    private Context context;
    private SharedPreferences prefs;
    
    public FavoriteManager(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    // Gestion des dossiers favoris
    public boolean isFolderFavorite(long folderId) {
        Set<String> favorites = prefs.getStringSet(FAVORITE_FOLDERS_KEY, new HashSet<>());
        return favorites.contains(String.valueOf(folderId));
    }
    
    public void toggleFolderFavorite(long folderId) {
        Set<String> favorites = new HashSet<>(prefs.getStringSet(FAVORITE_FOLDERS_KEY, new HashSet<>()));
        String folderIdStr = String.valueOf(folderId);
        
        if (favorites.contains(folderIdStr)) {
            favorites.remove(folderIdStr);
        } else {
            favorites.add(folderIdStr);
        }
        
        prefs.edit().putStringSet(FAVORITE_FOLDERS_KEY, favorites).apply();
    }
    
    public Set<Long> getFavoriteFolders() {
        Set<String> favoriteStrings = prefs.getStringSet(FAVORITE_FOLDERS_KEY, new HashSet<>());
        Set<Long> favorites = new HashSet<>();
        for (String s : favoriteStrings) {
            try {
                favorites.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                // Ignorer les IDs invalides
            }
        }
        return favorites;
    }
    
    // Gestion des recettes favorites
    public boolean isRecipeFavorite(long recipeId) {
        Set<String> favorites = prefs.getStringSet(FAVORITE_RECIPES_KEY, new HashSet<>());
        return favorites.contains(String.valueOf(recipeId));
    }
    
    public void toggleRecipeFavorite(long recipeId) {
        Set<String> favorites = new HashSet<>(prefs.getStringSet(FAVORITE_RECIPES_KEY, new HashSet<>()));
        String recipeIdStr = String.valueOf(recipeId);
        
        if (favorites.contains(recipeIdStr)) {
            favorites.remove(recipeIdStr);
        } else {
            favorites.add(recipeIdStr);
        }
        
        prefs.edit().putStringSet(FAVORITE_RECIPES_KEY, favorites).apply();
    }
    
    public Set<Long> getFavoriteRecipes() {
        Set<String> favoriteStrings = prefs.getStringSet(FAVORITE_RECIPES_KEY, new HashSet<>());
        Set<Long> favorites = new HashSet<>();
        for (String s : favoriteStrings) {
            try {
                favorites.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                // Ignorer les IDs invalides
            }
        }
        return favorites;
    }
}
