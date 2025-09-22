package fr.didictateur.inanutshell.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import fr.didictateur.inanutshell.data.database.AppDatabase;
import fr.didictateur.inanutshell.data.database.RecipeDao;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.sync.SyncManager;
import fr.didictateur.inanutshell.sync.model.SyncItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository qui gère les recettes avec synchronisation automatique
 * Intercepte toutes les opérations CRUD pour déclencher la synchronisation
 */
public class RecipeRepository {
    
    private final RecipeDao recipeDao;
    private final SyncManager syncManager;
    private final ExecutorService executorService;
    private final MutableLiveData<List<Recipe>> recipesLiveData;
    
    private static volatile RecipeRepository instance;
    
    private RecipeRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.recipeDao = database.recipeDao();
        this.syncManager = SyncManager.getInstance(context);
        this.executorService = Executors.newCachedThreadPool();
        this.recipesLiveData = new MutableLiveData<>();
        
        // Charger les recettes initiales
        loadRecipes();
    }
    
    public static RecipeRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (RecipeRepository.class) {
                if (instance == null) {
                    instance = new RecipeRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    // ===== LECTURES (pas de sync nécessaire) =====
    
    public LiveData<List<Recipe>> getAllRecipes() {
        return recipesLiveData;
    }
    
    public void getAllRecipesAsync(RecipeCallback callback) {
        executorService.execute(() -> {
            try {
                List<Recipe> recipes = recipeDao.getAllRecipes();
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void getRecipeById(long id, SingleRecipeCallback callback) {
        executorService.execute(() -> {
            try {
                Recipe recipe = recipeDao.getRecipeById(id);
                callback.onSuccess(recipe);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void searchRecipes(String query, RecipeCallback callback) {
        executorService.execute(() -> {
            try {
                List<Recipe> recipes = recipeDao.searchRecipesByTitle(query);
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void getFavoriteRecipes(RecipeCallback callback) {
        executorService.execute(() -> {
            try {
                List<Recipe> recipes = recipeDao.getFavoriteRecipes();
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void getRecipesByCategory(String category, RecipeCallback callback) {
        executorService.execute(() -> {
            try {
                List<Recipe> recipes = recipeDao.getRecipesByCategory(category);
                callback.onSuccess(recipes);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    // ===== ÉCRITURES (avec synchronisation automatique) =====
    
    /**
     * Insère une nouvelle recette et déclenche la synchronisation
     */
    public void insertRecipe(Recipe recipe, SingleRecipeCallback callback) {
        executorService.execute(() -> {
            try {
                // Mettre à jour le timestamp
                recipe.updateTimestamp();
                
                // Insérer en local
                long id = recipeDao.insertRecipe(recipe);
                recipe.setId(String.valueOf(id));
                
                // Déclencher la synchronisation
                syncManager.syncRecipe(recipe, SyncItem.Action.CREATE);
                
                // Rafraîchir la liste
                loadRecipes();
                
                callback.onSuccess(recipe);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Met à jour une recette existante et déclenche la synchronisation
     */
    public void updateRecipe(Recipe recipe, SingleRecipeCallback callback) {
        executorService.execute(() -> {
            try {
                // Mettre à jour le timestamp
                recipe.updateTimestamp();
                
                // Mettre à jour en local
                recipeDao.updateRecipe(recipe);
                
                // Déclencher la synchronisation
                syncManager.syncRecipe(recipe, SyncItem.Action.UPDATE);
                
                // Rafraîchir la liste
                loadRecipes();
                
                callback.onSuccess(recipe);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Supprime une recette et déclenche la synchronisation
     */
    public void deleteRecipe(Recipe recipe, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                // Supprimer en local
                recipeDao.deleteRecipe(recipe);
                
                // Déclencher la synchronisation
                syncManager.syncRecipe(recipe, SyncItem.Action.DELETE);
                
                // Rafraîchir la liste
                loadRecipes();
                
                callback.onSuccess();
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Met à jour le statut favori d'une recette
     */
    public void toggleFavorite(Recipe recipe, SingleRecipeCallback callback) {
        executorService.execute(() -> {
            try {
                // Inverser le statut favori
                recipe.setFavorite(!recipe.isFavorite());
                
                // Utiliser la méthode de mise à jour standard
                updateRecipe(recipe, callback);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Met à jour la note utilisateur d'une recette
     */
    public void updateUserRating(Recipe recipe, float rating, SingleRecipeCallback callback) {
        executorService.execute(() -> {
            try {
                // Mettre à jour la note
                recipe.setUserRating(rating);
                
                // Utiliser la méthode de mise à jour standard
                updateRecipe(recipe, callback);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Insère plusieurs recettes (généralement depuis la synchronisation)
     */
    public void insertRecipes(List<Recipe> recipes, RecipeCallback callback) {
        executorService.execute(() -> {
            try {
                // Mettre à jour les timestamps
                for (Recipe recipe : recipes) {
                    if (recipe.getUpdatedAt() == null || recipe.getUpdatedAt().isEmpty()) {
                        recipe.updateTimestamp();
                    }
                }
                
                // Insérer en local
                recipeDao.insertRecipes(recipes);
                
                // Note: Pas de sync automatique pour l'insertion en lot
                // (généralement utilisée par la synchronisation elle-même)
                
                // Rafraîchir la liste
                loadRecipes();
                
                callback.onSuccess(recipes);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    // ===== MÉTHODES UTILITAIRES =====
    
    /**
     * Force le rechargement des recettes
     */
    public void refreshRecipes() {
        loadRecipes();
    }
    
    /**
     * Obtient les statistiques des recettes
     */
    public void getRecipeStats(StatsCallback callback) {
        executorService.execute(() -> {
            try {
                int totalCount = recipeDao.getRecipeCount();
                List<Recipe> favorites = recipeDao.getFavoriteRecipes();
                List<String> categories = recipeDao.getAllCategories();
                
                RecipeStats stats = new RecipeStats(totalCount, favorites.size(), categories.size());
                callback.onStats(stats);
                
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Synchronisation directe avec le serveur (appelée par SyncManager)
     */
    public List<Recipe> getAllRecipesSync() {
        return recipeDao.getAllRecipes();
    }
    
    /**
     * Insertion directe sans synchronisation (utilisée par SyncManager)
     */
    public void insertRecipeSync(Recipe recipe) {
        recipeDao.insertRecipe(recipe);
        loadRecipes();
    }
    
    /**
     * Mise à jour directe sans synchronisation (utilisée par SyncManager)
     */
    public void updateRecipeSync(Recipe recipe) {
        recipeDao.updateRecipe(recipe);
        loadRecipes();
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    private void loadRecipes() {
        executorService.execute(() -> {
            try {
                List<Recipe> recipes = recipeDao.getAllRecipesSorted();
                recipesLiveData.postValue(recipes);
            } catch (Exception e) {
                // Log error but don't crash
                e.printStackTrace();
            }
        });
    }
    
    // ===== INTERFACES DE CALLBACK =====
    
    public interface RecipeCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
    }
    
    public interface SingleRecipeCallback {
        void onSuccess(Recipe recipe);
        void onError(String error);
    }
    
    public interface OperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface StatsCallback {
        void onStats(RecipeStats stats);
        void onError(String error);
    }
    
    // ===== CLASSES UTILITAIRES =====
    
    public static class RecipeStats {
        public final int totalRecipes;
        public final int favoriteRecipes;
        public final int categoriesCount;
        
        public RecipeStats(int totalRecipes, int favoriteRecipes, int categoriesCount) {
            this.totalRecipes = totalRecipes;
            this.favoriteRecipes = favoriteRecipes;
            this.categoriesCount = categoriesCount;
        }
    }
}
