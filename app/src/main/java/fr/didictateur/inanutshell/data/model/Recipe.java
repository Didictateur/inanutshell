package fr.didictateur.inanutshell.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.google.gson.annotations.SerializedName;
import fr.didictateur.inanutshell.data.database.DatabaseConverters;
import java.util.List;

@Entity(tableName = "recipes")
@TypeConverters(DatabaseConverters.class)
public class Recipe {
    @PrimaryKey
    @androidx.annotation.NonNull
    @SerializedName("id")
    private String id;
    
    @SerializedName("slug")
    private String slug;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("image")
    private String image;
    
    @SerializedName("totalTime")
    private String totalTime;
    
    @SerializedName("prepTime")
    private String prepTime;
    
    @SerializedName("cookTime")
    private String cookTime;
    
    @SerializedName("performTime")
    private String performTime;
    
    @SerializedName("recipeYield")
    private String recipeYield;
    
    @SerializedName("recipeIngredient")
    private List<RecipeIngredient> recipeIngredient;
    
    @SerializedName("recipeInstructions")
    private List<RecipeInstruction> recipeInstructions;
    
    @SerializedName("nutrition")
    private Nutrition nutrition;
    
    @SerializedName("tags")
    private List<Tag> tags;
    
    @SerializedName("categories")
    private List<Category> categories;
    
    @SerializedName("rating")
    private Integer rating;
    
    @SerializedName("recipeCategory")
    private List<String> recipeCategory;
    
    @SerializedName("recipeCuisine")
    private List<String> recipeCuisine;
    
    @SerializedName("tools")
    private List<Tool> tools;
    
    @SerializedName("dateAdded")
    private String dateAdded;
    
    @SerializedName("dateUpdated")
    private String dateUpdated;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;
    
    // Local fields (not from API)
    private boolean favorite = false;
    private float userRating = 0f;
    private Integer difficulty = null; // Niveau de difficulté 1-3

    // Constructeurs
    public Recipe() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getTotalTime() { return totalTime; }
    public void setTotalTime(String totalTime) { this.totalTime = totalTime; }

    public String getPrepTime() { return prepTime; }
    public void setPrepTime(String prepTime) { this.prepTime = prepTime; }

    public String getCookTime() { return cookTime; }
    public void setCookTime(String cookTime) { this.cookTime = cookTime; }
    
    public String getPerformTime() { return performTime; }
    public void setPerformTime(String performTime) { this.performTime = performTime; }

    public String getRecipeYield() { return recipeYield; }
    public void setRecipeYield(String recipeYield) { this.recipeYield = recipeYield; }

    public List<RecipeIngredient> getRecipeIngredient() { return recipeIngredient; }
    public void setRecipeIngredient(List<RecipeIngredient> recipeIngredient) { this.recipeIngredient = recipeIngredient; }

    public List<RecipeInstruction> getRecipeInstructions() { return recipeInstructions; }
    public void setRecipeInstructions(List<RecipeInstruction> recipeInstructions) { this.recipeInstructions = recipeInstructions; }

    public Nutrition getNutrition() { return nutrition; }
    public void setNutrition(Nutrition nutrition) { this.nutrition = nutrition; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public List<String> getRecipeCategory() { return recipeCategory; }
    public void setRecipeCategory(List<String> recipeCategory) { this.recipeCategory = recipeCategory; }

    public List<String> getRecipeCuisine() { return recipeCuisine; }
    public void setRecipeCuisine(List<String> recipeCuisine) { this.recipeCuisine = recipeCuisine; }

    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }

    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

    public String getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    
    public float getUserRating() { return userRating; }
    public void setUserRating(float userRating) { this.userRating = userRating; }
    
    public boolean hasUserRating() { return userRating > 0f; }
    
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    
    public boolean hasDifficulty() { return difficulty != null && difficulty > 0; }
}
