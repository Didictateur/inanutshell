package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.network.NetworkManager;

/**
 * Utilitaire pour charger les images de recettes avec Glide
 */
public class ImageLoader {
    
    /**
     * Charger une image de recette dans une ImageView
     */
    public static void loadRecipeImage(Context context, String recipeId, ImageView imageView) {
        if (context == null || imageView == null) {
            return;
        }
        
        // Obtenir l'URL complète de l'image
        String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipeId);
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.placeholder_recipe) // Image pendant le chargement
                .error(R.drawable.placeholder_recipe) // Image en cas d'erreur
                .fallback(R.drawable.placeholder_recipe) // Image si l'URL est null
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache sur disque
                .centerCrop(); // Ajustement de l'image
        
        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * Charger une image depuis une URL complète
     */
    public static void loadImageFromUrl(Context context, String imageUrl, ImageView imageView) {
        if (context == null || imageView == null) {
            return;
        }
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.placeholder_recipe)
                .fallback(R.drawable.placeholder_recipe)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();
        
        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * Charger une image de recette avec taille spécifique (pour les vignettes)
     */
    public static void loadRecipeImageThumbnail(Context context, String recipeId, ImageView imageView, int width, int height) {
        if (context == null || imageView == null) {
            return;
        }
        
        String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipeId);
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.placeholder_recipe)
                .fallback(R.drawable.placeholder_recipe)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(width, height) // Redimensionner
                .centerCrop();
        
        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * Précharger une image de recette (pour améliorer les performances)
     */
    public static void preloadRecipeImage(Context context, String recipeId) {
        if (context == null) {
            return;
        }
        
        String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipeId);
        
        Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload();
    }
    
    /**
     * Vider le cache des images
     */
    public static void clearCache(Context context) {
        if (context != null) {
            Glide.get(context).clearMemory();
            
            // Vider le cache disque en arrière-plan
            new Thread(() -> {
                Glide.get(context).clearDiskCache();
            }).start();
        }
    }
}
