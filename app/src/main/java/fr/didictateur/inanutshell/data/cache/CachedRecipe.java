package fr.didictateur.inanutshell.data.cache;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.Category;
import fr.didictateur.inanutshell.data.model.Tag;
import fr.didictateur.inanutshell.data.model.Tool;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.RecipeInstruction;

/**
 * Entité Room pour le cache des recettes
 * Stockage local des recettes pour l'accès hors ligne
 */
@Entity(tableName = "cached_recipes")
public class CachedRecipe {
    
    @PrimaryKey
    @NonNull
    public String id;
    
    public String name;
    public String slug;
    public String description;
    public String image;
    public String totalTime;
    public String prepTime;
    public String performTime;
    public String recipeYield;
    public String recipeCategoryJson;
    
    // JSON strings pour les objets complexes
    public String ingredientsJson;
    public String instructionsJson;
    public String categoriesJson;
    public String tagsJson;
    public String toolsJson;
    
    // Métadonnées de cache
    public long cachedAt; // Timestamp de mise en cache
    public long lastAccessedAt; // Dernière consultation
    public boolean isFavorite; // Cache du statut favori
    public float rating; // Cache de la note
    
    // Constructeur par défaut requis par Room
    public CachedRecipe() {}
    
    /**
     * Constructeur à partir d'une Recipe
     */
    public CachedRecipe(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.slug = recipe.getSlug();
        this.description = recipe.getDescription();
        this.image = recipe.getImage();
        this.totalTime = recipe.getTotalTime();
        this.prepTime = recipe.getPrepTime();
        this.performTime = recipe.getPerformTime();
        this.recipeYield = recipe.getRecipeYield();
        
        // Sérialiser les listes en JSON
        Gson gson = new Gson();
        this.recipeCategoryJson = gson.toJson(recipe.getCategories());
        this.ingredientsJson = gson.toJson(recipe.getRecipeIngredient());
        this.instructionsJson = gson.toJson(recipe.getRecipeInstructions());
        this.categoriesJson = gson.toJson(recipe.getCategories());
        this.tagsJson = gson.toJson(recipe.getTags());
        this.toolsJson = gson.toJson(recipe.getTools());
        
        // Métadonnées
        this.cachedAt = System.currentTimeMillis();
        this.lastAccessedAt = System.currentTimeMillis();
    }
    
    /**
     * Convertir en Recipe standard
     */
    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(this.id);
        recipe.setName(this.name);
        recipe.setSlug(this.slug);
        recipe.setDescription(this.description);
        recipe.setImage(this.image);
        recipe.setTotalTime(this.totalTime);
        recipe.setPrepTime(this.prepTime);
        recipe.setPerformTime(this.performTime);
        recipe.setRecipeYield(this.recipeYield);
        
        // Désérialiser les JSON
        Gson gson = new Gson();
        Type ingredientListType = new TypeToken<List<RecipeIngredient>>(){}.getType();
        Type instructionListType = new TypeToken<List<RecipeInstruction>>(){}.getType();
        Type categoryListType = new TypeToken<List<Category>>(){}.getType();
        Type tagListType = new TypeToken<List<Tag>>(){}.getType();
        Type toolListType = new TypeToken<List<Tool>>(){}.getType();
        
        if (ingredientsJson != null) {
            recipe.setRecipeIngredient(gson.fromJson(ingredientsJson, ingredientListType));
        }
        if (instructionsJson != null) {
            recipe.setRecipeInstructions(gson.fromJson(instructionsJson, instructionListType));
        }
        if (categoriesJson != null) {
            recipe.setCategories(gson.fromJson(categoriesJson, categoryListType));
        }
        if (recipeCategoryJson != null) {
            recipe.setCategories(gson.fromJson(recipeCategoryJson, categoryListType));
        }
        if (tagsJson != null) {
            recipe.setTags(gson.fromJson(tagsJson, tagListType));
        }
        if (toolsJson != null) {
            recipe.setTools(gson.fromJson(toolsJson, toolListType));
        }
        
        return recipe;
    }
    
    /**
     * Mettre à jour l'horodatage de dernière consultation
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = System.currentTimeMillis();
    }
    

}
