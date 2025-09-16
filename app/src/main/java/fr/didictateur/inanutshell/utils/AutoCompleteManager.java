package fr.didictateur.inanutshell.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;

/**
 * Gestionnaire d'autocomplétion pour les recherches
 * Combine l'historique des recherches avec les données des recettes existantes
 */
public class AutoCompleteManager {
    private static AutoCompleteManager instance;
    private final SearchHistoryManager historyManager;
    private final ExecutorService executor;
    
    private List<Recipe> cachedRecipes = new ArrayList<>();
    private Set<String> cachedIngredients = new LinkedHashSet<>();
    private Set<String> cachedRecipeNames = new LinkedHashSet<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    /**
     * Interface pour les callbacks d'autocomplétion
     */
    public interface AutoCompleteCallback {
        void onSuggestionsReady(List<String> suggestions);
    }
    
    private AutoCompleteManager(Context context) {
        historyManager = SearchHistoryManager.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Obtenir l'instance singleton
     */
    public static synchronized AutoCompleteManager getInstance(Context context) {
        if (instance == null) {
            instance = new AutoCompleteManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Mettre à jour le cache des recettes
     */
    public void updateRecipeCache(List<Recipe> recipes) {
        executor.execute(() -> {
            cachedRecipes = new ArrayList<>(recipes);
            extractDataFromRecipes();
            lastCacheUpdate = System.currentTimeMillis();
        });
    }
    
    /**
     * Obtenir des suggestions pour une recherche textuelle
     */
    public void getTextSuggestions(String query, int maxSuggestions, AutoCompleteCallback callback) {
        executor.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            
            if (query == null || query.trim().isEmpty()) {
                // Retourner les recherches récentes
                suggestions.addAll(historyManager.getTextSearches());
                if (suggestions.size() > maxSuggestions) {
                    suggestions = suggestions.subList(0, maxSuggestions);
                }
            } else {
                String lowercaseQuery = query.toLowerCase().trim();
                
                // 1. Ajouter les suggestions de l'historique
                List<String> historySuggestions = historyManager.getTextSuggestions(query, maxSuggestions / 2);
                suggestions.addAll(historySuggestions);
                
                // 2. Ajouter les noms de recettes correspondants
                ensureCacheValid();
                for (String recipeName : cachedRecipeNames) {
                    if (suggestions.size() >= maxSuggestions) {
                        break;
                    }
                    
                    if (recipeName.toLowerCase().contains(lowercaseQuery) && 
                        !containsIgnoreCase(suggestions, recipeName)) {
                        suggestions.add(recipeName);
                    }
                }
            }
            
            // Callback sur le thread principal
            if (callback != null) {
                callback.onSuggestionsReady(suggestions);
            }
        });
    }
    
    /**
     * Obtenir des suggestions pour une recherche d'ingrédient
     */
    public void getIngredientSuggestions(String query, int maxSuggestions, AutoCompleteCallback callback) {
        executor.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            
            if (query == null || query.trim().isEmpty()) {
                // Retourner les recherches récentes
                suggestions.addAll(historyManager.getIngredientSearches());
                if (suggestions.size() > maxSuggestions) {
                    suggestions = suggestions.subList(0, maxSuggestions);
                }
            } else {
                String lowercaseQuery = query.toLowerCase().trim();
                
                // 1. Ajouter les suggestions de l'historique
                List<String> historySuggestions = historyManager.getIngredientSuggestions(query, maxSuggestions / 2);
                suggestions.addAll(historySuggestions);
                
                // 2. Ajouter les ingrédients des recettes correspondants
                ensureCacheValid();
                for (String ingredient : cachedIngredients) {
                    if (suggestions.size() >= maxSuggestions) {
                        break;
                    }
                    
                    if (ingredient.toLowerCase().contains(lowercaseQuery) && 
                        !containsIgnoreCase(suggestions, ingredient)) {
                        suggestions.add(ingredient);
                    }
                }
            }
            
            // Callback sur le thread principal
            if (callback != null) {
                callback.onSuggestionsReady(suggestions);
            }
        });
    }
    
    /**
     * Obtenir des suggestions combinées (texte + ingrédients)
     */
    public void getCombinedSuggestions(String query, int maxSuggestions, AutoCompleteCallback callback) {
        executor.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            
            if (query == null || query.trim().isEmpty()) {
                // Mélanger l'historique récent
                List<String> allHistory = historyManager.getAllUniqueSearches();
                suggestions.addAll(allHistory);
                if (suggestions.size() > maxSuggestions) {
                    suggestions = suggestions.subList(0, maxSuggestions);
                }
            } else {
                String lowercaseQuery = query.toLowerCase().trim();
                
                // 1. Historique de recherche
                suggestions.addAll(historyManager.searchHistory(query, maxSuggestions / 3));
                
                ensureCacheValid();
                
                // 2. Noms de recettes
                for (String recipeName : cachedRecipeNames) {
                    if (suggestions.size() >= maxSuggestions) {
                        break;
                    }
                    
                    if (recipeName.toLowerCase().contains(lowercaseQuery) && 
                        !containsIgnoreCase(suggestions, recipeName)) {
                        suggestions.add(recipeName);
                    }
                }
                
                // 3. Ingrédients
                for (String ingredient : cachedIngredients) {
                    if (suggestions.size() >= maxSuggestions) {
                        break;
                    }
                    
                    if (ingredient.toLowerCase().contains(lowercaseQuery) && 
                        !containsIgnoreCase(suggestions, ingredient)) {
                        suggestions.add(ingredient);
                    }
                }
            }
            
            // Callback sur le thread principal
            if (callback != null) {
                callback.onSuggestionsReady(suggestions);
            }
        });
    }
    
    /**
     * Obtenir des suggestions populaires basées sur la fréquence d'utilisation
     */
    public void getPopularSuggestions(int maxSuggestions, AutoCompleteCallback callback) {
        executor.execute(() -> {
            List<String> popular = new ArrayList<>();
            
            // Prendre les plus récentes de l'historique (considérées comme populaires)
            List<String> recentText = historyManager.getTextSearches();
            List<String> recentIngredients = historyManager.getIngredientSearches();
            
            // Mélanger et limiter
            int textCount = Math.min(recentText.size(), maxSuggestions / 2);
            int ingredientCount = Math.min(recentIngredients.size(), maxSuggestions - textCount);
            
            popular.addAll(recentText.subList(0, textCount));
            popular.addAll(recentIngredients.subList(0, ingredientCount));
            
            if (callback != null) {
                callback.onSuggestionsReady(popular);
            }
        });
    }
    
    /**
     * Obtenir des suggestions pour les catégories existantes
     */
    public void getCategorySuggestions(String query, int maxSuggestions, AutoCompleteCallback callback) {
        executor.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            
            ensureCacheValid();
            
            Set<String> categories = new LinkedHashSet<>();
            for (Recipe recipe : cachedRecipes) {
                if (recipe.getRecipeCategory() != null && !recipe.getRecipeCategory().isEmpty()) {
                    for (String category : recipe.getRecipeCategory()) {
                        if (category != null && !category.trim().isEmpty()) {
                            categories.add(category.trim());
                        }
                    }
                }
            }
            
            if (query == null || query.trim().isEmpty()) {
                suggestions.addAll(categories);
            } else {
                String lowercaseQuery = query.toLowerCase().trim();
                for (String category : categories) {
                    if (category.toLowerCase().contains(lowercaseQuery)) {
                        suggestions.add(category);
                    }
                }
            }
            
            // Limiter les résultats
            if (suggestions.size() > maxSuggestions) {
                suggestions = suggestions.subList(0, maxSuggestions);
            }
            
            if (callback != null) {
                callback.onSuggestionsReady(suggestions);
            }
        });
    }
    
    /**
     * Vérifier si le cache est valide, sinon le reconstruire
     */
    private void ensureCacheValid() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_DURATION && !cachedRecipes.isEmpty()) {
            extractDataFromRecipes();
            lastCacheUpdate = now;
        }
    }
    
    /**
     * Extraire les données des recettes pour l'autocomplétion
     */
    private void extractDataFromRecipes() {
        cachedIngredients.clear();
        cachedRecipeNames.clear();
        
        for (Recipe recipe : cachedRecipes) {
            // Noms de recettes
            if (recipe.getName() != null && !recipe.getName().trim().isEmpty()) {
                cachedRecipeNames.add(recipe.getName().trim());
            }
            
            // Ingrédients
            if (recipe.getRecipeIngredient() != null) {
                for (RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                    if (ingredient.getFood() != null && !ingredient.getFood().trim().isEmpty()) {
                        cachedIngredients.add(ingredient.getFood().trim());
                    }
                }
            }
        }
    }
    
    /**
     * Vérifier si une liste contient une chaîne (ignorer la casse)
     */
    private boolean containsIgnoreCase(List<String> list, String item) {
        for (String str : list) {
            if (str.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Enregistrer une recherche dans l'historique
     */
    public void recordTextSearch(String query) {
        if (query != null && !query.trim().isEmpty()) {
            historyManager.addTextSearch(query.trim());
        }
    }
    
    /**
     * Enregistrer une recherche d'ingrédient dans l'historique
     */
    public void recordIngredientSearch(String ingredient) {
        if (ingredient != null && !ingredient.trim().isEmpty()) {
            historyManager.addIngredientSearch(ingredient.trim());
        }
    }
    
    /**
     * Obtenir des statistiques sur les données d'autocomplétion
     */
    public String getStats() {
        return "Recettes: " + cachedRecipes.size() + 
               ", Ingrédients uniques: " + cachedIngredients.size() + 
               ", Noms de recettes: " + cachedRecipeNames.size() +
               ", Historique texte: " + historyManager.getTextSearchCount() +
               ", Historique ingrédients: " + historyManager.getIngredientSearchCount();
    }
    
    /**
     * Nettoyer les ressources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
