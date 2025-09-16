package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire de l'historique des recherches utilisateur
 * Sauvegarde et récupère les recherches récentes avec persistance
 */
public class SearchHistoryManager {
    private static final String PREFS_NAME = "search_history";
    private static final String KEY_TEXT_SEARCHES = "text_searches";
    private static final String KEY_INGREDIENT_SEARCHES = "ingredient_searches";
    private static final int MAX_HISTORY_SIZE = 20;
    
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private static SearchHistoryManager instance;
    
    private SearchHistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    /**
     * Obtenir l'instance singleton du gestionnaire d'historique
     */
    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Ajouter une recherche textuelle à l'historique
     */
    public void addTextSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        query = query.trim();
        List<String> searches = getTextSearches();
        
        // Supprimer si déjà présent (pour le remettre en premier)
        searches.remove(query);
        
        // Ajouter en premier
        searches.add(0, query);
        
        // Limiter la taille
        if (searches.size() > MAX_HISTORY_SIZE) {
            searches = searches.subList(0, MAX_HISTORY_SIZE);
        }
        
        saveTextSearches(searches);
    }
    
    /**
     * Ajouter une recherche d'ingrédient à l'historique
     */
    public void addIngredientSearch(String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            return;
        }
        
        ingredient = ingredient.trim();
        List<String> searches = getIngredientSearches();
        
        // Supprimer si déjà présent (pour le remettre en premier)
        searches.remove(ingredient);
        
        // Ajouter en premier
        searches.add(0, ingredient);
        
        // Limiter la taille
        if (searches.size() > MAX_HISTORY_SIZE) {
            searches = searches.subList(0, MAX_HISTORY_SIZE);
        }
        
        saveIngredientSearches(searches);
    }
    
    /**
     * Obtenir l'historique des recherches textuelles
     */
    public List<String> getTextSearches() {
        String json = preferences.getString(KEY_TEXT_SEARCHES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> searches = gson.fromJson(json, listType);
        return searches != null ? searches : new ArrayList<>();
    }
    
    /**
     * Obtenir l'historique des recherches d'ingrédients
     */
    public List<String> getIngredientSearches() {
        String json = preferences.getString(KEY_INGREDIENT_SEARCHES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> searches = gson.fromJson(json, listType);
        return searches != null ? searches : new ArrayList<>();
    }
    
    /**
     * Obtenir les suggestions pour une requête textuelle
     */
    public List<String> getTextSuggestions(String query, int maxSuggestions) {
        if (query == null || query.trim().isEmpty()) {
            // Retourner les recherches récentes si aucune requête
            List<String> recent = getTextSearches();
            return recent.size() > maxSuggestions ? 
                recent.subList(0, maxSuggestions) : recent;
        }
        
        String lowercaseQuery = query.toLowerCase().trim();
        List<String> suggestions = new ArrayList<>();
        
        for (String search : getTextSearches()) {
            if (search.toLowerCase().contains(lowercaseQuery) && 
                !search.equalsIgnoreCase(query)) {
                suggestions.add(search);
                if (suggestions.size() >= maxSuggestions) {
                    break;
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Obtenir les suggestions pour une recherche d'ingrédient
     */
    public List<String> getIngredientSuggestions(String query, int maxSuggestions) {
        if (query == null || query.trim().isEmpty()) {
            // Retourner les recherches récentes si aucune requête
            List<String> recent = getIngredientSearches();
            return recent.size() > maxSuggestions ? 
                recent.subList(0, maxSuggestions) : recent;
        }
        
        String lowercaseQuery = query.toLowerCase().trim();
        List<String> suggestions = new ArrayList<>();
        
        for (String search : getIngredientSearches()) {
            if (search.toLowerCase().contains(lowercaseQuery) && 
                !search.equalsIgnoreCase(query)) {
                suggestions.add(search);
                if (suggestions.size() >= maxSuggestions) {
                    break;
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Supprimer une recherche textuelle de l'historique
     */
    public void removeTextSearch(String query) {
        List<String> searches = getTextSearches();
        searches.remove(query);
        saveTextSearches(searches);
    }
    
    /**
     * Supprimer une recherche d'ingrédient de l'historique
     */
    public void removeIngredientSearch(String ingredient) {
        List<String> searches = getIngredientSearches();
        searches.remove(ingredient);
        saveIngredientSearches(searches);
    }
    
    /**
     * Vider tout l'historique des recherches textuelles
     */
    public void clearTextSearches() {
        preferences.edit().remove(KEY_TEXT_SEARCHES).apply();
    }
    
    /**
     * Vider tout l'historique des recherches d'ingrédients
     */
    public void clearIngredientSearches() {
        preferences.edit().remove(KEY_INGREDIENT_SEARCHES).apply();
    }
    
    /**
     * Vider tout l'historique de recherche
     */
    public void clearAllSearches() {
        preferences.edit()
            .remove(KEY_TEXT_SEARCHES)
            .remove(KEY_INGREDIENT_SEARCHES)
            .apply();
    }
    
    /**
     * Obtenir le nombre de recherches textuelles dans l'historique
     */
    public int getTextSearchCount() {
        return getTextSearches().size();
    }
    
    /**
     * Obtenir le nombre de recherches d'ingrédients dans l'historique
     */
    public int getIngredientSearchCount() {
        return getIngredientSearches().size();
    }
    
    /**
     * Vérifier si l'historique est vide
     */
    public boolean isEmpty() {
        return getTextSearchCount() == 0 && getIngredientSearchCount() == 0;
    }
    
    /**
     * Obtenir toutes les recherches uniques (texte + ingrédients)
     */
    public List<String> getAllUniqueSearches() {
        Set<String> uniqueSearches = new LinkedHashSet<>();
        uniqueSearches.addAll(getTextSearches());
        uniqueSearches.addAll(getIngredientSearches());
        return new ArrayList<>(uniqueSearches);
    }
    
    /**
     * Rechercher dans l'historique avec une requête
     */
    public List<String> searchHistory(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowercaseQuery = query.toLowerCase().trim();
        List<String> results = new ArrayList<>();
        
        // Rechercher dans l'historique textuel
        for (String search : getTextSearches()) {
            if (search.toLowerCase().contains(lowercaseQuery)) {
                results.add(search);
                if (results.size() >= maxResults) {
                    return results;
                }
            }
        }
        
        // Rechercher dans l'historique des ingrédients
        for (String search : getIngredientSearches()) {
            if (search.toLowerCase().contains(lowercaseQuery) && !results.contains(search)) {
                results.add(search);
                if (results.size() >= maxResults) {
                    break;
                }
            }
        }
        
        return results;
    }
    
    /**
     * Sauvegarder l'historique des recherches textuelles
     */
    private void saveTextSearches(List<String> searches) {
        String json = gson.toJson(searches);
        preferences.edit().putString(KEY_TEXT_SEARCHES, json).apply();
    }
    
    /**
     * Sauvegarder l'historique des recherches d'ingrédients
     */
    private void saveIngredientSearches(List<String> searches) {
        String json = gson.toJson(searches);
        preferences.edit().putString(KEY_INGREDIENT_SEARCHES, json).apply();
    }
}
