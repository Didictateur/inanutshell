package fr.didictateur.inanutshell.data.cache;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * DAO pour les opérations de cache des recettes
 */
@Dao
public interface CachedRecipeDao {
    
    /**
     * Insérer ou mettre à jour une recette en cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CachedRecipe cachedRecipe);
    
    /**
     * Insérer ou mettre à jour plusieurs recettes en cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<CachedRecipe> cachedRecipes);
    
    /**
     * Récupérer toutes les recettes en cache
     */
    @Query("SELECT * FROM cached_recipes ORDER BY lastAccessedAt DESC")
    List<CachedRecipe> getAllCachedRecipes();
    
    /**
     * Récupérer une recette par son ID
     */
    @Query("SELECT * FROM cached_recipes WHERE id = :recipeId")
    CachedRecipe getCachedRecipeById(String recipeId);
    
    /**
     * Récupérer les recettes favorites en cache
     */
    @Query("SELECT * FROM cached_recipes WHERE isFavorite = 1 ORDER BY lastAccessedAt DESC")
    List<CachedRecipe> getFavoriteCachedRecipes();
    
    /**
     * Récupérer les recettes récemment consultées
     */
    @Query("SELECT * FROM cached_recipes ORDER BY lastAccessedAt DESC LIMIT :limit")
    List<CachedRecipe> getRecentlyAccessedRecipes(int limit);
    
    /**
     * Chercher des recettes par nom
     */
    @Query("SELECT * FROM cached_recipes WHERE name LIKE '%' || :searchQuery || '%' ORDER BY lastAccessedAt DESC")
    List<CachedRecipe> searchCachedRecipesByName(String searchQuery);
    
    /**
     * Mettre à jour le statut favori d'une recette
     */
    @Query("UPDATE cached_recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    void updateFavoriteStatus(String recipeId, boolean isFavorite);
    
    /**
     * Mettre à jour la note d'une recette
     */
    @Query("UPDATE cached_recipes SET rating = :rating WHERE id = :recipeId")
    void updateRating(String recipeId, float rating);
    
    /**
     * Mettre à jour l'horodatage de dernière consultation
     */
    @Query("UPDATE cached_recipes SET lastAccessedAt = :timestamp WHERE id = :recipeId")
    void updateLastAccessed(String recipeId, long timestamp);
    
    /**
     * Supprimer une recette du cache
     */
    @Delete
    void delete(CachedRecipe cachedRecipe);
    
    /**
     * Supprimer une recette du cache par ID
     */
    @Query("DELETE FROM cached_recipes WHERE id = :recipeId")
    void deleteById(String recipeId);
    
    /**
     * Supprimer les anciennes recettes du cache (plus anciennes que le timestamp donné)
     */
    @Query("DELETE FROM cached_recipes WHERE cachedAt < :threshold")
    void deleteOldRecipes(long threshold);
    
    /**
     * Compter le nombre de recettes en cache
     */
    @Query("SELECT COUNT(*) FROM cached_recipes")
    int getCachedRecipeCount();
    
    /**
     * Vider tout le cache
     */
    @Query("DELETE FROM cached_recipes")
    void clearAllCache();
    
    /**
     * Obtenir la taille totale du cache (approximation basée sur le nombre de recettes)
     */
    @Query("SELECT COUNT(*) FROM cached_recipes")
    int getCacheSize();
    
    /**
     * Récupérer les recettes les plus anciennes pour nettoyage
     */
    @Query("SELECT * FROM cached_recipes ORDER BY lastAccessedAt ASC LIMIT :limit")
    List<CachedRecipe> getOldestRecipes(int limit);
}
