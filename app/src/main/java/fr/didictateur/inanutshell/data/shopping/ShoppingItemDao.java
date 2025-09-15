package fr.didictateur.inanutshell.data.shopping;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

/**
 * DAO pour la gestion des articles de courses
 */
@Dao
public interface ShoppingItemDao {
    
    @Insert
    long insert(ShoppingItem shoppingItem);
    
    @Insert
    List<Long> insertAll(List<ShoppingItem> items);
    
    @Update
    void update(ShoppingItem shoppingItem);
    
    @Update
    void updateAll(List<ShoppingItem> items);
    
    @Delete
    void delete(ShoppingItem shoppingItem);
    
    @Query("DELETE FROM shopping_items WHERE id = :id")
    void deleteById(int id);
    
    @Query("DELETE FROM shopping_items WHERE shopping_list_id = :listId")
    void deleteByListId(int listId);
    
    @Query("SELECT * FROM shopping_items WHERE id = :id")
    ShoppingItem getById(int id);
    
    @Query("SELECT * FROM shopping_items WHERE shopping_list_id = :listId ORDER BY category, name")
    LiveData<List<ShoppingItem>> getItemsByListId(int listId);
    
    @Query("SELECT * FROM shopping_items WHERE shopping_list_id = :listId AND is_checked = 0 ORDER BY category, name")
    LiveData<List<ShoppingItem>> getUncheckedItemsByListId(int listId);
    
    @Query("SELECT * FROM shopping_items WHERE shopping_list_id = :listId AND is_checked = 1 ORDER BY category, name")
    LiveData<List<ShoppingItem>> getCheckedItemsByListId(int listId);
    
    @Query("SELECT * FROM shopping_items WHERE shopping_list_id = :listId AND category = :category ORDER BY name")
    LiveData<List<ShoppingItem>> getItemsByCategory(int listId, ShoppingItem.Category category);
    
    @Query("SELECT * FROM shopping_items WHERE shopping_list_id = :listId AND name LIKE '%' || :searchQuery || '%' ORDER BY name")
    LiveData<List<ShoppingItem>> searchItemsInList(int listId, String searchQuery);
    
    @Query("SELECT COUNT(*) FROM shopping_items WHERE shopping_list_id = :listId")
    int getTotalItemsCount(int listId);
    
    @Query("SELECT COUNT(*) FROM shopping_items WHERE shopping_list_id = :listId AND is_checked = 1")
    int getCheckedItemsCount(int listId);
    
    @Query("SELECT COUNT(*) FROM shopping_items WHERE shopping_list_id = :listId AND is_checked = 0")
    int getUncheckedItemsCount(int listId);
    
    @Query("UPDATE shopping_items SET is_checked = :isChecked WHERE id = :id")
    void updateCheckedStatus(int id, boolean isChecked);
    
    @Query("UPDATE shopping_items SET is_checked = :isChecked WHERE shopping_list_id = :listId")
    void updateAllCheckedStatus(int listId, boolean isChecked);
    
    @Query("UPDATE shopping_items SET is_checked = :isChecked WHERE shopping_list_id = :listId AND category = :category")
    void updateCategoryCheckedStatus(int listId, ShoppingItem.Category category, boolean isChecked);
    
    @Query("SELECT DISTINCT category FROM shopping_items WHERE shopping_list_id = :listId ORDER BY category")
    List<ShoppingItem.Category> getCategoriesInList(int listId);
    
    @Query("SELECT * FROM shopping_items WHERE recipe_id = :recipeId")
    List<ShoppingItem> getItemsByRecipeId(String recipeId);
    
    @Query("UPDATE shopping_items SET quantity = :quantity, unit = :unit WHERE id = :id")
    void updateQuantity(int id, String quantity, String unit);
    
    @Query("DELETE FROM shopping_items WHERE shopping_list_id = :listId AND is_checked = 1")
    void deleteCheckedItems(int listId);
}
