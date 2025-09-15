package fr.didictateur.inanutshell.data.shopping;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import fr.didictateur.inanutshell.Converters;

import java.util.Date;

/**
 * Entité représentant un article de liste de courses
 */
@Entity(tableName = "shopping_items")
@TypeConverters(Converters.class)
public class ShoppingItem {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "shopping_list_id") // Correction du nom pour correspondre au DAO
    public int shoppingListId;
    
    @ColumnInfo(name = "name")
    public String name;
    
    @ColumnInfo(name = "quantity")
    public String quantity; // "2 kg", "3 unités", etc.
    
    @ColumnInfo(name = "unit")
    public String unit; // kg, g, L, mL, pièces, etc.
    
    @ColumnInfo(name = "category")
    public Category category;
    
    @ColumnInfo(name = "is_checked")
    public boolean isChecked;
    
    @ColumnInfo(name = "recipe_id")
    public String recipeId; // Optionnel, si généré depuis une recette
    
    @ColumnInfo(name = "notes")
    public String notes;
    
    @ColumnInfo(name = "created_at")
    public Date createdAt;
    
    @ColumnInfo(name = "checked_at")
    public Date checkedAt;
    
    // Enum pour les catégories/rayons
    public enum Category {
        FRUITS_VEGETABLES("Fruits & Légumes", "🥬", "#4CAF50"),
        MEAT_FISH("Viande & Poisson", "🥩", "#FF5722"),
        DAIRY("Produits laitiers", "🥛", "#2196F3"),
        BAKERY("Boulangerie", "🍞", "#FF9800"),
        PANTRY("Épicerie", "🥫", "#795548"),
        FROZEN("Surgelés", "❄️", "#00BCD4"),
        BEVERAGES("Boissons", "🥤", "#9C27B0"),
        CLEANING("Entretien", "🧽", "#607D8B"),
        PERSONAL_CARE("Hygiène", "🧴", "#E91E63"),
        OTHER("Autre", "📦", "#9E9E9E");
        
        private final String displayName;
        private final String emoji;
        private final String color;
        
        Category(String displayName, String emoji, String color) {
            this.displayName = displayName;
            this.emoji = emoji;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getEmoji() {
            return emoji;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    public ShoppingItem() {
        this.createdAt = new Date();
        this.isChecked = false;
    }
    
    @Ignore
    public ShoppingItem(int listId, String name, String quantity, Category category) {
        this();
        this.shoppingListId = listId;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getListId() {
        return shoppingListId;
    }
    
    public void setListId(int listId) {
        this.shoppingListId = listId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getQuantity() {
        return quantity;
    }
    
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public boolean isChecked() {
        return isChecked;
    }
    
    public void setChecked(boolean checked) {
        isChecked = checked;
        if (checked && checkedAt == null) {
            checkedAt = new Date();
        } else if (!checked) {
            checkedAt = null;
        }
    }
    
    public String getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
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
    
    public Date getCheckedAt() {
        return checkedAt;
    }
    
    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }
}
