package fr.didictateur.inanutshell.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import fr.didictateur.inanutshell.data.model.Recipe;
import java.util.List;

@Dao
public interface RecipeDao {
    
    @Query("SELECT * FROM recipes")
    List<Recipe> getAllRecipes();
    
    @Query("SELECT * FROM recipes WHERE id = :id")
    Recipe getRecipeById(long id);
    
    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :title || '%'")
    List<Recipe> searchRecipesByTitle(String title);
    
    @Query("SELECT * FROM recipes WHERE categories LIKE '%' || :category || '%'")
    List<Recipe> getRecipesByCategory(String category);
    
    @Query("SELECT * FROM recipes WHERE favorite = 1")
    List<Recipe> getFavoriteRecipes();
    
    @Insert
    long insertRecipe(Recipe recipe);
    
    @Insert
    void insertRecipes(List<Recipe> recipes);
    
    @Update
    void updateRecipe(Recipe recipe);
    
    @Delete
    void deleteRecipe(Recipe recipe);
    
    @Query("DELETE FROM recipes WHERE id = :id")
    void deleteRecipeById(long id);
    
    @Query("SELECT COUNT(*) FROM recipes")
    int getRecipeCount();
    
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    List<Recipe> getAllRecipesSorted();
    
    @Query("SELECT DISTINCT categories FROM recipes WHERE categories IS NOT NULL ORDER BY categories ASC")
    List<String> getAllCategories();
    
    // ===== MÉTHODES POUR LA SYNCHRONISATION =====
    
    @Query("SELECT * FROM recipes")
    List<Recipe> getAllRecipesSync();
    
    @Insert
    void insert(Recipe recipe);
    
    @Update  
    void update(Recipe recipe);
}
