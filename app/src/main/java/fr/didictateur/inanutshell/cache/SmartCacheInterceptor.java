package fr.didictateur.inanutshell.cache;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import fr.didictateur.inanutshell.network.NetworkStateManager;

/**
 * Intercepteur intelligent pour la gestion du cache HTTP avec stratégies adaptatives
 */
public class SmartCacheInterceptor implements Interceptor {
    private static final String TAG = "SmartCacheInterceptor";
    
    private final NetworkStateManager networkManager;
    private final CacheStrategy strategy;
    
    // Configuration du cache
    public static final long CACHE_SIZE = 50 * 1024 * 1024; // 50 MB
    public static final String CACHE_DIR_NAME = "http_cache";
    
    /**
     * Stratégies de cache disponibles
     */
    public enum CacheStrategy {
        AGGRESSIVE,     // Cache agressif - max performance
        BALANCED,       // Équilibré - performance et fraîcheur
        FRESH,          // Fraîcheur - données à jour
        OFFLINE_FIRST   // Hors-ligne prioritaire
    }
    
    /**
     * Configuration du cache par type de ressource
     */
    public static class CacheConfig {
        public final int maxAgeOnline;      // Durée en secondes pour le cache online
        public final int maxStaleOffline;   // Durée en secondes pour le cache offline
        public final boolean forceCacheOffline; // Forcer le cache quand offline
        
        public CacheConfig(int maxAgeOnline, int maxStaleOffline, boolean forceCacheOffline) {
            this.maxAgeOnline = maxAgeOnline;
            this.maxStaleOffline = maxStaleOffline;
            this.forceCacheOffline = forceCacheOffline;
        }
    }
    
    public SmartCacheInterceptor(Context context, CacheStrategy strategy) {
        this.networkManager = NetworkStateManager.getInstance(context);
        this.strategy = strategy;
    }
    
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        
        // Obtenir la configuration de cache pour cette requête
        CacheConfig config = getCacheConfigForRequest(request);
        
        // Modifier la requête selon l'état du réseau
        Request modifiedRequest = modifyRequestForCache(request, config);
        
        // Exécuter la requête
        Response response = chain.proceed(modifiedRequest);
        
        // Modifier la réponse pour optimiser le cache
        return modifyResponseForCache(response, config);
    }
    
    /**
     * Obtient la configuration de cache appropriée pour une requête
     */
    private CacheConfig getCacheConfigForRequest(Request request) {
        String url = request.url().toString();
        
        // Configuration par type de ressource
        if (url.contains("/recipes")) {
            // Recettes - peuvent être mises en cache plus longtemps
            switch (strategy) {
                case AGGRESSIVE:
                    return new CacheConfig(300, 86400 * 7, true); // 5min online, 7j offline
                case BALANCED:
                    return new CacheConfig(120, 86400 * 3, true); // 2min online, 3j offline
                case FRESH:
                    return new CacheConfig(30, 86400, true);      // 30s online, 1j offline
                case OFFLINE_FIRST:
                    return new CacheConfig(600, 86400 * 30, true); // 10min online, 30j offline
                default:
                    return new CacheConfig(120, 86400 * 3, true);
            }
        } else if (url.contains("/categories") || url.contains("/tags")) {
            // Catégories et tags - changent rarement
            switch (strategy) {
                case AGGRESSIVE:
                    return new CacheConfig(1800, 86400 * 14, true); // 30min online, 14j offline
                case BALANCED:
                    return new CacheConfig(600, 86400 * 7, true);   // 10min online, 7j offline
                case FRESH:
                    return new CacheConfig(180, 86400 * 2, true);   // 3min online, 2j offline
                case OFFLINE_FIRST:
                    return new CacheConfig(3600, 86400 * 30, true); // 1h online, 30j offline
                default:
                    return new CacheConfig(600, 86400 * 7, true);
            }
        } else if (url.contains("/users") || url.contains("/auth")) {
            // Données utilisateur - fraîcheur importante
            switch (strategy) {
                case AGGRESSIVE:
                    return new CacheConfig(60, 86400, false);     // 1min online, 1j offline, pas de force
                case BALANCED:
                    return new CacheConfig(30, 3600, false);     // 30s online, 1h offline, pas de force
                case FRESH:
                    return new CacheConfig(0, 300, false);       // Pas de cache online, 5min offline
                case OFFLINE_FIRST:
                    return new CacheConfig(180, 86400 * 7, true); // 3min online, 7j offline
                default:
                    return new CacheConfig(30, 3600, false);
            }
        } else {
            // Autres ressources - configuration par défaut
            switch (strategy) {
                case AGGRESSIVE:
                    return new CacheConfig(180, 86400 * 3, true);
                case BALANCED:
                    return new CacheConfig(60, 86400, true);
                case FRESH:
                    return new CacheConfig(30, 1800, false);
                case OFFLINE_FIRST:
                    return new CacheConfig(300, 86400 * 7, true);
                default:
                    return new CacheConfig(60, 86400, true);
            }
        }
    }
    
    /**
     * Modifie la requête pour optimiser le cache
     */
    private Request modifyRequestForCache(Request request, CacheConfig config) {
        Request.Builder builder = request.newBuilder();
        
        NetworkStateManager.NetworkState networkState = networkManager.getCurrentNetworkState();
        
        if (!networkState.isConnected) {
            // Pas de réseau - utiliser uniquement le cache
            if (config.forceCacheOffline) {
                CacheControl cacheControl = new CacheControl.Builder()
                    .onlyIfCached()
                    .maxStale(config.maxStaleOffline, TimeUnit.SECONDS)
                    .build();
                builder.cacheControl(cacheControl);
                
                Log.d(TAG, "Offline mode: using cache only for " + request.url());
            }
        } else if (networkState.isMetered) {
            // Connexion limitée - favoriser le cache
            CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(config.maxAgeOnline * 2, TimeUnit.SECONDS) // Double la durée sur réseau limité
                .build();
            builder.cacheControl(cacheControl);
            
            Log.d(TAG, "Metered connection: extended cache for " + request.url());
        } else {
            // Connexion normale - configuration standard
            if (config.maxAgeOnline > 0) {
                CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(config.maxAgeOnline, TimeUnit.SECONDS)
                    .build();
                builder.cacheControl(cacheControl);
            }
        }
        
        return builder.build();
    }
    
    /**
     * Modifie la réponse pour optimiser le cache
     */
    private Response modifyResponseForCache(Response response, CacheConfig config) {
        NetworkStateManager.NetworkState networkState = networkManager.getCurrentNetworkState();
        
        // Si la requête vient du cache, pas besoin de modifier
        if (response.cacheResponse() != null && response.networkResponse() == null) {
            Log.d(TAG, "Response from cache: " + response.request().url());
            return response;
        }
        
        // Construire les headers de cache appropriés
        Response.Builder builder = response.newBuilder();
        
        if (networkState.isConnected) {
            // En ligne - définir les headers de cache
            String cacheControlValue = buildCacheControlHeader(config, networkState);
            builder.header("Cache-Control", cacheControlValue);
            
            // Ajouter Etag si absent pour améliorer la validation
            if (response.header("ETag") == null) {
                String etag = "\"" + System.currentTimeMillis() + "\"";
                builder.header("ETag", etag);
            }
            
            Log.d(TAG, "Response cached with: " + cacheControlValue + " for " + response.request().url());
        }
        
        return builder.build();
    }
    
    /**
     * Construit la valeur de l'header Cache-Control
     */
    private String buildCacheControlHeader(CacheConfig config, NetworkStateManager.NetworkState networkState) {
        StringBuilder cacheControl = new StringBuilder();
        
        // Max age pour le cache
        if (config.maxAgeOnline > 0) {
            int maxAge = config.maxAgeOnline;
            
            // Ajuster selon le type de connexion
            if (networkState.isMetered) {
                maxAge *= 2; // Double sur connexion limitée
            }
            
            cacheControl.append("max-age=").append(maxAge);
        }
        
        // Stale while revalidate pour une meilleure expérience
        if (config.maxStaleOffline > 0) {
            if (cacheControl.length() > 0) cacheControl.append(", ");
            cacheControl.append("stale-while-revalidate=").append(config.maxStaleOffline / 2);
        }
        
        // Public cache si approprié
        String url = "";
        if (!url.contains("/auth") && !url.contains("/users")) {
            if (cacheControl.length() > 0) cacheControl.append(", ");
            cacheControl.append("public");
        }
        
        return cacheControl.toString();
    }
    
    /**
     * Crée une instance de Cache OkHttp optimisée
     */
    public static Cache createOptimizedCache(Context context) {
        File cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        return new Cache(cacheDir, CACHE_SIZE);
    }
    
    /**
     * Nettoie le cache selon les critères définis
     */
    public static void cleanCache(Context context, boolean aggressive) {
        try {
            File cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
            
            if (aggressive) {
                // Nettoyage complet
                deleteRecursively(cacheDir);
                Log.i(TAG, "Aggressive cache cleanup completed");
            } else {
                // Nettoyage partiel - supprimer les anciens fichiers
                long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                cleanOldFiles(cacheDir, cutoffTime);
                Log.i(TAG, "Partial cache cleanup completed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning cache", e);
        }
    }
    
    /**
     * Supprime récursivement un répertoire
     */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
    
    /**
     * Nettoie les anciens fichiers
     */
    private static void cleanOldFiles(File dir, long cutoffTime) {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                cleanOldFiles(file, cutoffTime);
            } else if (file.lastModified() < cutoffTime) {
                file.delete();
            }
        }
    }
    
    /**
     * Obtient les statistiques du cache
     */
    public static CacheStats getCacheStats(Cache cache) {
        if (cache == null) {
            return new CacheStats(0, 0, 0, 0, 0);
        }
        
        try {
            return new CacheStats(
                cache.size(),
                cache.maxSize(),
                cache.hitCount(),
                cache.requestCount(),
                cache.networkCount()
            );
        } catch (java.io.IOException e) {
            android.util.Log.w("SmartCacheInterceptor", "Erreur lors de la lecture des stats de cache", e);
            return new CacheStats(0, cache.maxSize(), 0, 0, 0);
        }
    }
    
    /**
     * Classe pour les statistiques de cache
     */
    public static class CacheStats {
        public final long size;
        public final long maxSize;
        public final int hitCount;
        public final int requestCount;
        public final int networkCount;
        public final double hitRate;
        
        public CacheStats(long size, long maxSize, int hitCount, int requestCount, int networkCount) {
            this.size = size;
            this.maxSize = maxSize;
            this.hitCount = hitCount;
            this.requestCount = requestCount;
            this.networkCount = networkCount;
            this.hitRate = requestCount > 0 ? (double) hitCount / requestCount : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d bytes, hits=%d/%d (%.1f%%), network=%d}", 
                size, maxSize, hitCount, requestCount, hitRate * 100, networkCount);
        }
    }
}
