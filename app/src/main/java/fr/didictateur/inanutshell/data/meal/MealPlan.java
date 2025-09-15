package fr.didictateur.inanutshell.data.meal;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import fr.didictateur.inanutshell.Converters;

import java.util.Date;

/**
 * Entité représentant un repas planifié dans le calendrier
 */
@Entity(tableName = "meal_plans")
@TypeConverters(Converters.class)
public class MealPlan {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "recipe_id")
    public String recipeId;
    
    @ColumnInfo(name = "recipe_name")
    public String recipeName;
    
    @ColumnInfo(name = "meal_date")
    public Date mealDate;
    
    @ColumnInfo(name = "meal_type")
    public MealType mealType;
    
    @ColumnInfo(name = "servings")
    public int servings;
    
    @ColumnInfo(name = "notes")
    public String notes;
    
    @ColumnInfo(name = "created_at")
    public Date createdAt;
    
    @ColumnInfo(name = "is_completed")
    public boolean isCompleted;
    
    // Champs de synchronisation avec Mealie
    @ColumnInfo(name = "server_id")
    public String serverId; // ID sur le serveur Mealie
    
    @ColumnInfo(name = "is_synced")
    public boolean isSynced; // Indique si synchronisé avec le serveur
    
    @ColumnInfo(name = "last_sync_date")
    public Date lastSyncDate; // Dernière synchronisation
    
    @ColumnInfo(name = "needs_sync")
    public boolean needsSync; // Indique si nécessite synchronisation
    
    // Enum pour les types de repas
    public enum MealType {
        BREAKFAST("Petit-déjeuner"),
        LUNCH("Déjeuner"), 
        DINNER("Dîner"),
        SNACK("Collation");
        
        private final String displayName;
        
        MealType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public MealPlan() {
        this.createdAt = new Date();
        this.servings = 4; // Valeur par défaut
        this.isCompleted = false;
    }
    
    @Ignore
    public MealPlan(String recipeId, String recipeName, Date date, MealType mealType) {
        this();
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.mealDate = date;
        this.mealType = mealType;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }
    
    public String getRecipeName() {
        return recipeName;
    }
    
    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }
    
    public Date getDate() {
        return mealDate;
    }
    
    public void setDate(Date date) {
        this.mealDate = date;
    }
    
    public MealType getMealType() {
        return mealType;
    }
    
    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }
    
    public int getServings() {
        return servings;
    }
    
    public void setServings(int servings) {
        this.servings = servings;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
    
    // Getters et Setters pour synchronisation
    public String getServerId() {
        return serverId;
    }
    
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public boolean isSynced() {
        return isSynced;
    }
    
    public void setSynced(boolean synced) {
        isSynced = synced;
        if (synced) {
            needsSync = false;
        }
    }
    
    public Date getLastSyncDate() {
        return lastSyncDate;
    }
    
    public void setLastSyncDate(Date lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
    
    public boolean needsSync() {
        return needsSync;
    }
    
    public void setNeedsSync(boolean needsSync) {
        this.needsSync = needsSync;
    }
    
    /**
     * Marque le meal plan comme modifié et nécessitant une synchronisation
     */
    public void markAsModified() {
        this.needsSync = true;
        this.isSynced = false;
    }
}
