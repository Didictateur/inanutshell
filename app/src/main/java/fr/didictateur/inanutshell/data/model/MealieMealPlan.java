package fr.didictateur.inanutshell.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Modèle pour la planification des repas compatible avec l'API Mealie
 * Basé sur l'API Mealie v1.x meal-plans endpoint
 */
public class MealieMealPlan {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("date")
    private String date; // Format ISO 8601 (YYYY-MM-DD)
    
    @SerializedName("entry_type")
    private String entryType; // "breakfast", "lunch", "dinner", "side"
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("recipe_id")
    private String recipeId;
    
    @SerializedName("recipe")
    private Recipe recipe; // Recette complète si disponible
    
    @SerializedName("group_id")
    private String groupId;
    
    @SerializedName("user_id")
    private String userId;
    
    // Constructeurs
    public MealieMealPlan() {}
    
    public MealieMealPlan(String date, String entryType, String title, String recipeId) {
        this.date = date;
        this.entryType = entryType;
        this.title = title;
        this.recipeId = recipeId;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
