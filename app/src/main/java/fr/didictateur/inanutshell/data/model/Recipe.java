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
    private String userId = null; // ID de l'utilisateur qui a créé/modifié la recette

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
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    // Alias pour recipeYield pour compatibilité
    public String getYield() { return recipeYield; }
    public void setYield(String yield) { this.recipeYield = yield; }
    
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    
    public boolean hasDifficulty() { return difficulty != null && difficulty > 0; }
    
    // ===== MÉTHODES UTILITAIRES =====
    
    /**
     * Obtient les instructions sous forme de texte
     * @return instructions formatées, ou chaine vide si aucune
     */
    public String getInstructions() {
        if (recipeInstructions == null || recipeInstructions.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recipeInstructions.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append((i + 1)).append(". ").append(recipeInstructions.get(i).getText());
        }
        return sb.toString();
    }
    
    /**
     * Définit les instructions à partir d'un texte
     * @param instructions texte des instructions
     */
    public void setInstructions(String instructions) {
        if (instructions == null || instructions.trim().isEmpty()) {
            this.recipeInstructions = null;
            return;
        }
        
        // Simplification: pour l'instant, on crée une seule instruction
        // Dans une vraie implémentation, on analyserait le texte pour séparer les étapes
        this.recipeInstructions = new java.util.ArrayList<>();
        RecipeInstruction instruction = new RecipeInstruction();
        instruction.setText(instructions);
        this.recipeInstructions.add(instruction);
    }
    
    // ===== MÉTHODES POUR LA SYNCHRONISATION =====
    
    /**
     * Obtient le timestamp updatedAt en millisecondes
     * @return timestamp en millisecondes, ou 0 si pas défini
     */
    public long getUpdatedAtTimestamp() {
        if (updatedAt == null || updatedAt.isEmpty()) {
            return 0;
        }
        try {
            // Format ISO 8601 de Mealie: "2024-01-15T10:30:00.123Z"
            java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(updatedAt);
            return dateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            // Fallback: essayer de parser comme timestamp unix
            try {
                return Long.parseLong(updatedAt);
            } catch (NumberFormatException nfe) {
                return System.currentTimeMillis(); // Par défaut, maintenant
            }
        }
    }
    
    /**
     * Met à jour le timestamp updatedAt avec le timestamp actuel
     */
    public void updateTimestamp() {
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        this.updatedAt = now.toString();
    }
    
    /**
     * Obtient le temps de cuisson en minutes
     * @return temps de cuisson en minutes, ou 0 si pas défini
     */
    public int getCookingTime() {
        if (cookTime == null || cookTime.isEmpty()) {
            return 0;
        }
        try {
            // Extraire les minutes du format PT30M ou "30 min"
            String time = cookTime.toUpperCase();
            if (time.contains("PT") && time.contains("M")) {
                // Format ISO 8601: PT30M
                return Integer.parseInt(time.replace("PT", "").replace("M", ""));
            } else if (time.contains("MIN")) {
                // Format simple: "30 min"
                return Integer.parseInt(time.replaceAll("[^0-9]", ""));
            } else {
                // Essayer de parser comme nombre simple
                return Integer.parseInt(time.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Définit le temps de cuisson en minutes
     * @param minutes temps de cuisson en minutes
     */
    public void setCookingTime(int minutes) {
        if (minutes > 0) {
            this.cookTime = "PT" + minutes + "M";
        } else {
            this.cookTime = null;
        }
    }
    
    /**
     * Vérifie si cette recette a été modifiée récemment (moins de 24h)
     * @return true si modifiée récemment
     */
    public boolean isRecentlyModified() {
        long lastUpdate = getUpdatedAtTimestamp();
        if (lastUpdate == 0) return false;
        
        long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        return lastUpdate > dayAgo;
    }
    
    /**
     * Compare deux recettes pour détecter les conflits
     * @param other autre recette à comparer
     * @return true s'il y a des différences significatives
     */
    public boolean hasConflictWith(Recipe other) {
        if (other == null) return false;
        
        // Comparer les champs principaux
        if (!java.util.Objects.equals(name, other.name)) return true;
        if (!java.util.Objects.equals(description, other.description)) return true;
        if (Math.abs(getCookingTime() - other.getCookingTime()) > 15) return true;
        
        // Comparer les ingrédients (nombre)
        int localIngredients = recipeIngredient != null ? recipeIngredient.size() : 0;
        int otherIngredients = other.recipeIngredient != null ? other.recipeIngredient.size() : 0;
        if (Math.abs(localIngredients - otherIngredients) > 2) return true;
        
        // Comparer les instructions (nombre)
        int localInstructions = recipeInstructions != null ? recipeInstructions.size() : 0;
        int otherInstructions = other.recipeInstructions != null ? other.recipeInstructions.size() : 0;
        if (Math.abs(localInstructions - otherInstructions) > 1) return true;
        
        return false;
    }
    
    /**
     * Crée une copie de cette recette avec un nouveau timestamp
     * @return copie de la recette
     */
    public Recipe createUpdatedCopy() {
        Recipe copy = new Recipe();
        copy.id = this.id;
        copy.slug = this.slug;
        copy.name = this.name;
        copy.description = this.description;
        copy.image = this.image;
        copy.totalTime = this.totalTime;
        copy.prepTime = this.prepTime;
        copy.cookTime = this.cookTime;
        copy.performTime = this.performTime;
        copy.recipeYield = this.recipeYield;
        copy.recipeIngredient = this.recipeIngredient;
        copy.recipeInstructions = this.recipeInstructions;
        copy.recipeCategory = this.recipeCategory;
        copy.tags = this.tags;
        copy.rating = this.rating;
        copy.nutrition = this.nutrition;
        copy.tools = this.tools;
        copy.createdAt = this.createdAt;
        copy.favorite = this.favorite;
        copy.userRating = this.userRating;
        copy.userId = this.userId;
        copy.difficulty = this.difficulty;
        
        // Mettre à jour le timestamp
        copy.updateTimestamp();
        
        return copy;
    }
}
