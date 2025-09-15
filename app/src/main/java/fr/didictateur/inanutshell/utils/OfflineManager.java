package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import androidx.annotation.NonNull;
import fr.didictateur.inanutshell.data.cache.CacheDatabase;
import fr.didictateur.inanutshell.data.cache.CachedRecipe;
import fr.didictateur.inanutshell.data.cache.CachedRecipeDao;
import fr.didictateur.inanutshell.data.model.Recipe;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire principal pour les fonctionnalités hors ligne
 * Gère le cache des recettes, la détection de connexion et la synchronisation
 */
public class OfflineManager {
    
    private static final String TAG = "OfflineManager";
    private static OfflineManager instance;
    
    private final Context context;
    private final CacheDatabase cacheDatabase;
    private final CachedRecipeDao cachedRecipeDao;
    private final ConnectivityManager connectivityManager;
    private final ExecutorService executorService;
    
    // État de la connexion
    private boolean isOnline = true;
    private NetworkCallback networkCallback;
    
    // Configuration du cache
    private static final long CACHE_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L; // 7 jours
    private static final int MAX_CACHED_RECIPES = 100; // Maximum de recettes en cache
    
    // Callbacks
    public interface ConnectionStatusListener {
        void onConnectionStatusChanged(boolean isOnline);
    }
    
    public interface CacheCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
    }
    
    public interface RecipeCacheCallback {
        void onSuccess(Recipe recipe);
        void onError(String error);
    }
    
    private List<ConnectionStatusListener> connectionListeners = new ArrayList<>();
    
    private OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheDatabase = CacheDatabase.getInstance(context);
        this.cachedRecipeDao = cacheDatabase.cachedRecipeDao();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.executorService = Executors.newFixedThreadPool(2);
        
        initNetworkCallback();
        checkInitialConnectionStatus();
    }
    
    public static synchronized OfflineManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineManager(context);
        }
        return instance;
    }
    
    /**
     * Initialiser le callback réseau pour surveiller la connexion
     */
    private void initNetworkCallback() {
        networkCallback = new NetworkCallback();
        
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }
    
    /**
     * Vérifier le statut initial de la connexion
     */
    private void checkInitialConnectionStatus() {
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                isOnline = false;
            }
        }
        Log.d(TAG, "Statut initial de connexion: " + (isOnline ? "En ligne" : "Hors ligne"));
    }
    
    /**
     * Ajouter un listener pour les changements de connexion
     */
    public void addConnectionStatusListener(ConnectionStatusListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }
    
    /**
     * Retirer un listener
     */
    public void removeConnectionStatusListener(ConnectionStatusListener listener) {
        connectionListeners.remove(listener);
    }
    
    /**
     * Obtenir l'état actuel de la connexion
     */
    public boolean isOnline() {
        return isOnline;
    }
    
    /**
     * Mettre en cache une recette
     */
    public void cacheRecipe(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) return;
        
        executorService.execute(() -> {
            try {
                CachedRecipe cachedRecipe = new CachedRecipe(recipe);
                cachedRecipeDao.insertOrUpdate(cachedRecipe);
                
                // Nettoyer le cache si nécessaire
                cleanupCacheIfNeeded();
                
                Log.d(TAG, "Recette mise en cache: " + recipe.getName());
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la mise en cache de la recette: " + e.getMessage());
            }
        });
    }
    
    /**
     * Récupérer une recette du cache
     */
    public void getCachedRecipe(String recipeId, RecipeCacheCallback callback) {
        if (recipeId == null || callback == null) return;
        
        executorService.execute(() -> {
            try {
                CachedRecipe cachedRecipe = cachedRecipeDao.getCachedRecipeById(recipeId);
                if (cachedRecipe != null) {
                    // Mettre à jour l'horodatage de dernière consultation
                    cachedRecipeDao.updateLastAccessed(recipeId, System.currentTimeMillis());
                    
                    Recipe recipe = cachedRecipe.toRecipe();
                    callback.onSuccess(recipe);
                } else {
                    callback.onError("Recette non trouvée dans le cache");
                }
            } catch (Exception e) {
                callback.onError("Erreur lors de la récupération du cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Récupérer toutes les recettes en cache
     */
    public void getAllCachedRecipes(CacheCallback callback) {
        if (callback == null) return;
        
        executorService.execute(() -> {
            try {
                List<CachedRecipe> cachedRecipes = cachedRecipeDao.getAllCachedRecipes();
                List<Recipe> recipes = new ArrayList<>();
                
                for (CachedRecipe cachedRecipe : cachedRecipes) {
                    recipes.add(cachedRecipe.toRecipe());
                }
                
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError("Erreur lors de la récupération du cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Rechercher dans le cache
     */
    public void searchCachedRecipes(String query, CacheCallback callback) {
        if (callback == null) return;
        
        executorService.execute(() -> {
            try {
                List<CachedRecipe> cachedRecipes;
                
                if (query == null || query.trim().isEmpty()) {
                    cachedRecipes = cachedRecipeDao.getAllCachedRecipes();
                } else {
                    cachedRecipes = cachedRecipeDao.searchCachedRecipesByName(query.trim());
                }
                
                List<Recipe> recipes = new ArrayList<>();
                for (CachedRecipe cachedRecipe : cachedRecipes) {
                    recipes.add(cachedRecipe.toRecipe());
                }
                
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError("Erreur lors de la recherche dans le cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Mettre à jour le statut favori dans le cache
     */
    public void updateFavoriteStatusInCache(String recipeId, boolean isFavorite) {
        if (recipeId == null) return;
        
        executorService.execute(() -> {
            try {
                cachedRecipeDao.updateFavoriteStatus(recipeId, isFavorite);
                Log.d(TAG, "Statut favori mis à jour dans le cache pour: " + recipeId);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la mise à jour du favori dans le cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Mettre à jour la note dans le cache
     */
    public void updateRatingInCache(String recipeId, float rating) {
        if (recipeId == null) return;
        
        executorService.execute(() -> {
            try {
                cachedRecipeDao.updateRating(recipeId, rating);
                Log.d(TAG, "Note mise à jour dans le cache pour: " + recipeId);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la mise à jour de la note dans le cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Nettoyer le cache si nécessaire
     */
    private void cleanupCacheIfNeeded() {
        try {
            int cacheSize = cachedRecipeDao.getCachedRecipeCount();
            
            if (cacheSize > MAX_CACHED_RECIPES) {
                // Supprimer les recettes les plus anciennes
                int recipesToRemove = cacheSize - MAX_CACHED_RECIPES + 10; // Marge de sécurité
                List<CachedRecipe> oldestRecipes = cachedRecipeDao.getOldestRecipes(recipesToRemove);
                
                for (CachedRecipe recipe : oldestRecipes) {
                    cachedRecipeDao.delete(recipe);
                }
                
                Log.d(TAG, "Cache nettoyé: " + recipesToRemove + " recettes supprimées");
            }
            
            // Supprimer les recettes expirées
            long expirationThreshold = System.currentTimeMillis() - CACHE_EXPIRATION_TIME;
            cachedRecipeDao.deleteOldRecipes(expirationThreshold);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du nettoyage du cache: " + e.getMessage());
        }
    }
    
    /**
     * Vider complètement le cache
     */
    public void clearCache() {
        executorService.execute(() -> {
            try {
                cachedRecipeDao.clearAllCache();
                Log.d(TAG, "Cache vidé complètement");
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du vidage du cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Obtenir des statistiques sur le cache
     */
    public void getCacheStats(CacheStatsCallback callback) {
        if (callback == null) return;
        
        executorService.execute(() -> {
            try {
                int totalRecipes = cachedRecipeDao.getCachedRecipeCount();
                callback.onSuccess(totalRecipes);
            } catch (Exception e) {
                callback.onError("Erreur lors de la récupération des stats: " + e.getMessage());
            }
        });
    }
    
    public interface CacheStatsCallback {
        void onSuccess(int totalCachedRecipes);
        void onError(String error);
    }
    
    /**
     * Nettoyer les ressources
     */
    public void cleanup() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        connectionListeners.clear();
        executorService.shutdown();
    }
    
    /**
     * Callback réseau pour surveiller les changements de connexion
     */
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        
        @Override
        public void onAvailable(@NonNull Network network) {
            if (!isOnline) {
                isOnline = true;
                Log.d(TAG, "Connexion rétablie");
                notifyConnectionListeners();
            }
        }
        
        @Override
        public void onLost(@NonNull Network network) {
            isOnline = false;
            Log.d(TAG, "Connexion perdue - Mode hors ligne activé");
            notifyConnectionListeners();
        }
        
        private void notifyConnectionListeners() {
            for (ConnectionStatusListener listener : connectionListeners) {
                try {
                    listener.onConnectionStatusChanged(isOnline);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la notification du listener: " + e.getMessage());
                }
            }
        }
    }
}
