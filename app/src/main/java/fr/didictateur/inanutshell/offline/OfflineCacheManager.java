package fr.didictateur.inanutshell.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.model.Recipe;

public class OfflineCacheManager {
    private static OfflineCacheManager instance;
    private Context context;
    private SharedPreferences cachePrefs;
    private File cacheDir;
    private File imagesCacheDir;
    private ExecutorService executorService;
    
    // Cache en mémoire pour accès rapide
    private Map<String, Recipe> recipeMemoryCache = new ConcurrentHashMap<>();
    private Map<String, Bitmap> imageMemoryCache = new ConcurrentHashMap<>();
    
    // Statistiques de cache
    private MutableLiveData<CacheStats> cacheStatsLiveData = new MutableLiveData<>();
    
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_MEMORY_CACHE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final String PREF_CACHE_SIZE = "cache_size";
    private static final String PREF_AUTO_DOWNLOAD = "auto_download_enabled";
    
    public static class CacheStats {
        public long totalSize;
        public int recipeCount;
        public int imageCount;
        public int hitCount;
        public int missCount;
        public double hitRate;
    }
    
    public enum CachePolicy {
        CACHE_FIRST,    // Utiliser le cache en priorité
        NETWORK_FIRST,  // Réseau en priorité, cache en secours
        CACHE_ONLY,     // Cache uniquement (mode hors ligne strict)
        NETWORK_ONLY    // Réseau uniquement (pas de cache)
    }
    
    private OfflineCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.cachePrefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE);
        this.executorService = Executors.newCachedThreadPool();
        
        initializeCacheDirectories();
        updateCacheStats();
    }
    
    public static synchronized OfflineCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineCacheManager(context);
        }
        return instance;
    }
    
    private void initializeCacheDirectories() {
        cacheDir = new File(context.getCacheDir(), "offline_data");
        imagesCacheDir = new File(cacheDir, "images");
        
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        if (!imagesCacheDir.exists()) {
            imagesCacheDir.mkdirs();
        }
    }
    
    public void cacheRecipe(Recipe recipe) {
        executorService.execute(() -> {
            try {
                // Cache en mémoire
                recipeMemoryCache.put(recipe.getId(), recipe);
                
                // Cache sur disque
                File recipeFile = new File(cacheDir, "recipe_" + recipe.getId() + ".json");
                // TODO: Sérialiser la recette en JSON
                
                // Télécharger et cacher l'image si disponible
                if (recipe.getImage() != null && !recipe.getImage().isEmpty()) {
                    cacheRecipeImage(recipe.getId(), recipe.getImage());
                }
                
                updateCacheStats();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void cacheRecipeImage(String recipeId, String imageUrl) {
        executorService.execute(() -> {
            try {
                // TODO: Télécharger l'image depuis l'URL
                // Pour l'instant, simulation
                File imageFile = new File(imagesCacheDir, "recipe_" + recipeId + ".jpg");
                
                // Vérifier si l'image existe déjà
                if (!imageFile.exists()) {
                    // TODO: Implémenter le téléchargement réel
                    // downloadImage(imageUrl, imageFile);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public Recipe getCachedRecipe(String recipeId, CachePolicy policy) {
        switch (policy) {
            case CACHE_ONLY:
            case CACHE_FIRST:
                return getCachedRecipeLocal(recipeId);
                
            case NETWORK_FIRST:
                // Essayer le réseau d'abord, puis le cache en cas d'échec
                Recipe networkRecipe = fetchFromNetwork(recipeId);
                return networkRecipe != null ? networkRecipe : getCachedRecipeLocal(recipeId);
                
            case NETWORK_ONLY:
                return fetchFromNetwork(recipeId);
                
            default:
                return getCachedRecipeLocal(recipeId);
        }
    }
    
    private Recipe getCachedRecipeLocal(String recipeId) {
        // Vérifier le cache mémoire d'abord
        Recipe recipe = recipeMemoryCache.get(recipeId);
        if (recipe != null) {
            incrementHitCount();
            return recipe;
        }
        
        // Vérifier le cache disque
        try {
            File recipeFile = new File(cacheDir, "recipe_" + recipeId + ".json");
            if (recipeFile.exists()) {
                // TODO: Désérialiser depuis JSON
                // Recipe cachedRecipe = deserializeRecipe(recipeFile);
                // recipeMemoryCache.put(recipeId, cachedRecipe);
                incrementHitCount();
                // return cachedRecipe;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        incrementMissCount();
        return null;
    }
    
    private Recipe fetchFromNetwork(String recipeId) {
        // TODO: Implémenter la récupération réseau
        return null;
    }
    
    public Bitmap getCachedImage(String recipeId) {
        // Vérifier le cache mémoire
        Bitmap bitmap = imageMemoryCache.get(recipeId);
        if (bitmap != null) {
            return bitmap;
        }
        
        // Vérifier le cache disque
        try {
            File imageFile = new File(imagesCacheDir, "recipe_" + recipeId + ".jpg");
            if (imageFile.exists()) {
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    // Ajouter au cache mémoire avec vérification de taille
                    addToImageMemoryCache(recipeId, bitmap);
                    return bitmap;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void addToImageMemoryCache(String key, Bitmap bitmap) {
        // Vérifier la taille du cache mémoire
        long currentSize = calculateMemoryCacheSize();
        long bitmapSize = bitmap.getByteCount();
        
        if (currentSize + bitmapSize > MAX_MEMORY_CACHE_SIZE) {
            evictFromMemoryCache();
        }
        
        imageMemoryCache.put(key, bitmap);
    }
    
    private long calculateMemoryCacheSize() {
        long totalSize = 0;
        for (Bitmap bitmap : imageMemoryCache.values()) {
            totalSize += bitmap.getByteCount();
        }
        return totalSize;
    }
    
    private void evictFromMemoryCache() {
        // Supprimer quelques éléments du cache mémoire (LRU simple)
        List<String> keysToRemove = new ArrayList<>();
        int removeCount = imageMemoryCache.size() / 4; // Supprimer 25%
        
        for (String key : imageMemoryCache.keySet()) {
            keysToRemove.add(key);
            if (keysToRemove.size() >= removeCount) {
                break;
            }
        }
        
        for (String key : keysToRemove) {
            imageMemoryCache.remove(key);
        }
    }
    
    public void preloadRecipes(List<Recipe> recipes) {
        if (!isAutoDownloadEnabled()) {
            return;
        }
        
        executorService.execute(() -> {
            for (Recipe recipe : recipes) {
                cacheRecipe(recipe);
            }
        });
    }
    
    public void clearCache() {
        executorService.execute(() -> {
            // Vider les caches mémoire
            recipeMemoryCache.clear();
            imageMemoryCache.clear();
            
            // Supprimer les fichiers de cache
            deleteDirectoryContents(cacheDir);
            
            updateCacheStats();
        });
    }
    
    private void deleteDirectoryContents(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryContents(file);
                    }
                    file.delete();
                }
            }
        }
    }
    
    public void cleanupOldCache() {
        executorService.execute(() -> {
            long maxAge = 7 * 24 * 60 * 60 * 1000; // 7 jours
            long currentTime = System.currentTimeMillis();
            
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (currentTime - file.lastModified() > maxAge) {
                        file.delete();
                    }
                }
            }
            
            updateCacheStats();
        });
    }
    
    public void optimizeCache() {
        executorService.execute(() -> {
            long currentSize = calculateCacheSize();
            
            if (currentSize > MAX_CACHE_SIZE) {
                // Supprimer les fichiers les plus anciens
                List<File> files = new ArrayList<>();
                collectFiles(cacheDir, files);
                
                // Trier par date de dernière modification
                files.sort((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                
                long sizeToRemove = currentSize - (MAX_CACHE_SIZE * 3 / 4); // Réduire à 75% de la limite
                long removedSize = 0;
                
                for (File file : files) {
                    if (removedSize >= sizeToRemove) {
                        break;
                    }
                    removedSize += file.length();
                    file.delete();
                }
            }
            
            updateCacheStats();
        });
    }
    
    private void collectFiles(File directory, List<File> files) {
        File[] dirFiles = directory.listFiles();
        if (dirFiles != null) {
            for (File file : dirFiles) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    collectFiles(file, files);
                }
            }
        }
    }
    
    private long calculateCacheSize() {
        return calculateDirectorySize(cacheDir);
    }
    
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }
    
    private void updateCacheStats() {
        executorService.execute(() -> {
            CacheStats stats = new CacheStats();
            stats.totalSize = calculateCacheSize();
            stats.recipeCount = recipeMemoryCache.size();
            stats.imageCount = imageMemoryCache.size();
            
            // Calculer le taux de succès du cache
            int totalRequests = getHitCount() + getMissCount();
            if (totalRequests > 0) {
                stats.hitRate = (double) getHitCount() / totalRequests;
            }
            
            stats.hitCount = getHitCount();
            stats.missCount = getMissCount();
            
            cacheStatsLiveData.postValue(stats);
        });
    }
    
    private void incrementHitCount() {
        int count = cachePrefs.getInt("hit_count", 0);
        cachePrefs.edit().putInt("hit_count", count + 1).apply();
    }
    
    private void incrementMissCount() {
        int count = cachePrefs.getInt("miss_count", 0);
        cachePrefs.edit().putInt("miss_count", count + 1).apply();
    }
    
    private int getHitCount() {
        return cachePrefs.getInt("hit_count", 0);
    }
    
    private int getMissCount() {
        return cachePrefs.getInt("miss_count", 0);
    }
    
    public void setAutoDownloadEnabled(boolean enabled) {
        cachePrefs.edit().putBoolean(PREF_AUTO_DOWNLOAD, enabled).apply();
    }
    
    public boolean isAutoDownloadEnabled() {
        return cachePrefs.getBoolean(PREF_AUTO_DOWNLOAD, true);
    }
    
    public LiveData<CacheStats> getCacheStatsLiveData() {
        return cacheStatsLiveData;
    }
    
    public void resetStats() {
        cachePrefs.edit()
            .remove("hit_count")
            .remove("miss_count")
            .apply();
        updateCacheStats();
    }
}
