package fr.didictateur.inanutshell.data.shopping;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import fr.didictateur.inanutshell.Converters;

import java.util.Date;

/**
 * Entité représentant une liste de courses
 */
@Entity(tableName = "shopping_lists")
@TypeConverters(Converters.class)
public class ShoppingList {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "name")
    public String name;
    
    @ColumnInfo(name = "description")
    public String description;
    
    @ColumnInfo(name = "created_at")
    public Date createdAt;
    
    @ColumnInfo(name = "updated_at")
    public Date updatedAt;
    
    @ColumnInfo(name = "is_completed")
    public boolean isCompleted;
    
    @ColumnInfo(name = "completed_at")
    public Date completedAt;
    
    @ColumnInfo(name = "total_items")
    public int totalItems;
    
    @ColumnInfo(name = "checked_items")
    public int checkedItems;
    
    @ColumnInfo(name = "generation_source")
    public GenerationSource generationSource;
    
    @ColumnInfo(name = "source_id")
    public String sourceId; // ID de la recette ou du planning
    
    // Enum pour la source de génération
    public enum GenerationSource {
        MANUAL("Manuelle"),
        RECIPE("Recette"),
        MEAL_PLAN("Planning repas"),
        TEMPLATE("Modèle");
        
        private final String displayName;
        
        GenerationSource(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public ShoppingList() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isCompleted = false;
        this.totalItems = 0;
        this.checkedItems = 0;
        this.generationSource = GenerationSource.MANUAL;
    }
    
    @Ignore
    public ShoppingList(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    @Ignore
    public ShoppingList(String name, GenerationSource source, String sourceId) {
        this();
        this.name = name;
        this.generationSource = source;
        this.sourceId = sourceId;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = new Date();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Date();
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        isCompleted = completed;
        if (completed && completedAt == null) {
            completedAt = new Date();
        } else if (!completed) {
            completedAt = null;
        }
        this.updatedAt = new Date();
    }
    
    public Date getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
    
    public int getCheckedItems() {
        return checkedItems;
    }
    
    public void setCheckedItems(int checkedItems) {
        this.checkedItems = checkedItems;
    }
    
    public GenerationSource getGenerationSource() {
        return generationSource;
    }
    
    public void setGenerationSource(GenerationSource generationSource) {
        this.generationSource = generationSource;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
    
    // Méthodes utilitaires
    public int getProgress() {
        if (totalItems == 0) return 0;
        return (int) ((checkedItems * 100.0f) / totalItems);
    }
    
    public int getProgressPercentage() {
        return getProgress();
    }
    
    public void updateItemCounts(int totalItems, int checkedItems) {
        this.totalItems = totalItems;
        this.checkedItems = checkedItems;
        this.updatedAt = new Date();
        
        // Marquer comme terminée si tous les articles sont cochés
        if (totalItems > 0 && checkedItems >= totalItems) {
            setCompleted(true);
        } else if (isCompleted && checkedItems < totalItems) {
            setCompleted(false);
        }
    }
}
