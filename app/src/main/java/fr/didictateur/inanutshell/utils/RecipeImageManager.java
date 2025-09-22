package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.network.NetworkManager;

/**
 * Gestionnaire avancé pour les images de recettes
 * Gère les images multiples, la suppression, les miniatures optimisées
 */
public class RecipeImageManager {
    
    private static final String TAG = "RecipeImageManager";
    private static RecipeImageManager instance;
    
    private Context context;
    private ExecutorService executor;
    private Map<String, List<String>> recipeImages; // recipeId -> list of image URLs
    private Map<String, Bitmap> thumbnailCache;
    
    // Callbacks pour les opérations
    public interface ImageOperationCallback {
        void onSuccess();
        void onError(String message);
    }
    
    public interface ImageListCallback {
        void onImagesLoaded(List<String> imageUrls);
        void onError(String message);
    }
    
    public interface ThumbnailCallback {
        void onThumbnailReady(Bitmap thumbnail);
        void onError(String message);
    }
    
    private RecipeImageManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(3);
        this.recipeImages = new HashMap<>();
        this.thumbnailCache = new HashMap<>();
    }
    
    public static synchronized RecipeImageManager getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeImageManager(context);
        }
        return instance;
    }
    
    // ===================== GESTION DES IMAGES MULTIPLES =====================
    
    /**
     * Ajouter une image à une recette (support images multiples)
     */
    public void addImageToRecipe(String recipeId, Uri imageUri, ImageOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Upload de l'image vers le serveur
                fr.didictateur.inanutshell.data.network.ImageUploadService.uploadRecipeImage(
                    context, recipeId, imageUri, new fr.didictateur.inanutshell.data.network.ImageUploadService.ImageUploadListener() {
                        @Override
                        public void onUploadSuccess(String imageUrl) {
                            // Ajouter l'URL à la liste des images de la recette
                            List<String> images = recipeImages.getOrDefault(recipeId, new ArrayList<>());
                            images.add(imageUrl);
                            recipeImages.put(recipeId, images);
                            
                            // Créer une miniature en cache
                            createAndCacheThumbnail(recipeId, imageUri, imageUrl);
                            
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }
                        
                        @Override
                        public void onUploadError(String error) {
                            Log.e(TAG, "Erreur upload image: " + error);
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                        
                        @Override
                        public void onUploadProgress(int progress) {
                            // Progress handled by caller if needed
                        }
                    }
                );
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'ajout de l'image", e);
                if (callback != null) {
                    callback.onError("Erreur lors de l'ajout de l'image: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Supprimer une image spécifique d'une recette
     */
    public void removeImageFromRecipe(String recipeId, String imageUrl, ImageOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Appel API pour supprimer l'image du serveur
                NetworkManager networkManager = NetworkManager.getInstance();
                
                // Construction de l'URL de suppression
                String deleteUrl = imageUrl.replace("/images/", "/images/delete/");
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(deleteUrl)
                    .delete()
                    .header("Authorization", "Bearer " + networkManager.getAuthToken())
                    .build();
                
                networkManager.getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, java.io.IOException e) {
                        Log.e(TAG, "Erreur suppression image", e);
                        if (callback != null) {
                            callback.onError("Erreur réseau lors de la suppression");
                        }
                    }
                    
                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                        if (response.isSuccessful()) {
                            // Supprimer de la liste locale
                            List<String> images = recipeImages.get(recipeId);
                            if (images != null) {
                                images.remove(imageUrl);
                                recipeImages.put(recipeId, images);
                            }
                            
                            // Supprimer la miniature du cache
                            removeThumbnailFromCache(recipeId, imageUrl);
                            
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            Log.e(TAG, "Erreur suppression: " + response.code());
                            if (callback != null) {
                                callback.onError("Erreur serveur: " + response.code());
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la suppression", e);
                if (callback != null) {
                    callback.onError("Erreur: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Obtenir toutes les images d'une recette
     */
    public void getRecipeImages(String recipeId, ImageListCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifier d'abord le cache local
                List<String> cachedImages = recipeImages.get(recipeId);
                if (cachedImages != null && !cachedImages.isEmpty()) {
                    if (callback != null) {
                        callback.onImagesLoaded(new ArrayList<>(cachedImages));
                    }
                    return;
                }
                
                // Sinon charger depuis l'API
                NetworkManager networkManager = NetworkManager.getInstance();
                String imagesUrl = networkManager.getBaseUrl() + "/api/recipes/" + recipeId + "/images";
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(imagesUrl)
                    .header("Authorization", "Bearer " + networkManager.getAuthToken())
                    .build();
                
                networkManager.getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, java.io.IOException e) {
                        Log.e(TAG, "Erreur chargement images", e);
                        if (callback != null) {
                            callback.onError("Erreur réseau");
                        }
                    }
                    
                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                        if (response.isSuccessful()) {
                            try {
                                String responseBody = response.body().string();
                                List<String> imageUrls = parseImageUrls(responseBody);
                                
                                // Mettre en cache
                                recipeImages.put(recipeId, imageUrls);
                                
                                if (callback != null) {
                                    callback.onImagesLoaded(imageUrls);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur parsing images", e);
                                if (callback != null) {
                                    callback.onError("Erreur de données");
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Erreur serveur: " + response.code());
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement images", e);
                if (callback != null) {
                    callback.onError("Erreur: " + e.getMessage());
                }
            }
        });
    }
    
    // ===================== GESTION DES MINIATURES =====================
    
    /**
     * Obtenir une miniature optimisée (avec cache)
     */
    public void getThumbnail(String recipeId, String imageUrl, int width, int height, ThumbnailCallback callback) {
        String cacheKey = recipeId + "_" + width + "x" + height;
        
        // Vérifier le cache mémoire
        Bitmap cachedThumbnail = thumbnailCache.get(cacheKey);
        if (cachedThumbnail != null && !cachedThumbnail.isRecycled()) {
            if (callback != null) {
                callback.onThumbnailReady(cachedThumbnail);
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                // Vérifier le cache disque
                Bitmap diskThumbnail = ImageUtils.loadThumbnailFromCache(context, cacheKey);
                if (diskThumbnail != null) {
                    thumbnailCache.put(cacheKey, diskThumbnail);
                    if (callback != null) {
                        callback.onThumbnailReady(diskThumbnail);
                    }
                    return;
                }
                
                // Créer la miniature depuis l'image originale
                // Note: Ici on devrait télécharger l'image depuis l'URL puis créer la miniature
                // Pour simplifier, on utilise l'URL directement avec Glide dans l'UI
                
                if (callback != null) {
                    callback.onError("Miniature non trouvée en cache");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur création miniature", e);
                if (callback != null) {
                    callback.onError("Erreur: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Créer et mettre en cache une miniature
     */
    private void createAndCacheThumbnail(String recipeId, Uri imageUri, String imageUrl) {
        try {
            // Créer plusieurs tailles de miniatures
            int[] sizes = {150, 300, 600}; // petite, moyenne, grande
            
            for (int size : sizes) {
                Bitmap thumbnail = ImageUtils.createOptimizedThumbnail(context, imageUri, size, size);
                if (thumbnail != null) {
                    String cacheKey = recipeId + "_" + size + "x" + size;
                    
                    // Cache mémoire
                    thumbnailCache.put(cacheKey, thumbnail);
                    
                    // Cache disque
                    ImageUtils.saveThumbnailToCache(context, thumbnail, cacheKey);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur création miniatures", e);
        }
    }
    
    /**
     * Supprimer une miniature du cache
     */
    private void removeThumbnailFromCache(String recipeId, String imageUrl) {
        try {
            // Supprimer de tous les caches
            int[] sizes = {150, 300, 600};
            
            for (int size : sizes) {
                String cacheKey = recipeId + "_" + size + "x" + size;
                
                // Cache mémoire
                Bitmap cached = thumbnailCache.remove(cacheKey);
                if (cached != null && !cached.isRecycled()) {
                    cached.recycle();
                }
                
                // Cache disque
                File cacheFile = new File(context.getCacheDir(), "thumbnails/thumb_" + cacheKey + ".jpg");
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression miniatures", e);
        }
    }
    
    // ===================== UTILITAIRES =====================
    
    /**
     * Parser les URLs d'images depuis une réponse JSON
     */
    private List<String> parseImageUrls(String jsonResponse) {
        List<String> urls = new ArrayList<>();
        
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray imagesArray = jsonObject.getJSONArray("images");
            
            for (int i = 0; i < imagesArray.length(); i++) {
                String imageUrl = imagesArray.getString(i);
                urls.add(imageUrl);
            }
            
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Erreur parsing JSON images", e);
        }
        
        return urls;
    }
    
    /**
     * Vider tous les caches
     */
    public void clearAllCaches() {
        // Cache mémoire
        for (Bitmap bitmap : thumbnailCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        thumbnailCache.clear();
        
        // Cache des URLs
        recipeImages.clear();
        
        // Cache disque
        ImageUtils.clearThumbnailCache(context);
    }
    
    /**
     * Nettoyer les ressources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        clearAllCaches();
    }
}
