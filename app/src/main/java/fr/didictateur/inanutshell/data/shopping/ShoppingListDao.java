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
 * DAO pour la gestion des listes de courses
 */
@Dao
public interface ShoppingListDao {
    
    @Insert
    long insert(ShoppingList shoppingList);
    
    @Update
    void update(ShoppingList shoppingList);
    
    @Delete
    void delete(ShoppingList shoppingList);
    
    @Query("DELETE FROM shopping_lists WHERE id = :id")
    void deleteById(int id);
    
    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    ShoppingList getById(int id);
    
    @Query("SELECT * FROM shopping_lists ORDER BY created_at DESC")
    LiveData<List<ShoppingList>> getAllLists();
    
    @Query("SELECT * FROM shopping_lists WHERE is_completed = 0 ORDER BY updated_at DESC")
    LiveData<List<ShoppingList>> getActiveLists();
    
    @Query("SELECT * FROM shopping_lists WHERE is_completed = 1 ORDER BY completed_at DESC")
    LiveData<List<ShoppingList>> getCompletedLists();
    
    @Query("SELECT * FROM shopping_lists WHERE generation_source = :source ORDER BY created_at DESC")
    LiveData<List<ShoppingList>> getListsBySource(ShoppingList.GenerationSource source);
    
    @Query("SELECT * FROM shopping_lists WHERE source_id = :sourceId")
    LiveData<List<ShoppingList>> getListsBySourceId(String sourceId);
    
    @Query("SELECT COUNT(*) FROM shopping_lists")
    int getTotalListsCount();
    
    @Query("SELECT COUNT(*) FROM shopping_lists WHERE is_completed = 0")
    int getActiveListsCount();
    
    @Query("UPDATE shopping_lists SET is_completed = 1, completed_at = :completedAt WHERE id = :id")
    void markAsCompleted(int id, Date completedAt);
    
    @Query("UPDATE shopping_lists SET is_completed = 0, completed_at = null WHERE id = :id")
    void markAsActive(int id);
    
    @Query("UPDATE shopping_lists SET total_items = :totalItems, checked_items = :checkedItems, updated_at = :updatedAt WHERE id = :id")
    void updateItemCounts(int id, int totalItems, int checkedItems, Date updatedAt);
    
    @Query("DELETE FROM shopping_lists WHERE is_completed = 1 AND completed_at < :beforeDate")
    void deleteOldCompletedLists(Date beforeDate);
}
