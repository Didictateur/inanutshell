package fr.didictateur.inanutshell.data.meal;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

/**
 * DAO pour la gestion des repas planifiés
 */
@Dao
public interface MealPlanDao {
    
    @Insert
    long insert(MealPlan mealPlan);
    
    @Update
    void update(MealPlan mealPlan);
    
    @Delete
    void delete(MealPlan mealPlan);
    
    @Query("DELETE FROM meal_plans WHERE id = :id")
    void deleteById(int id);
    
    @Query("SELECT * FROM meal_plans WHERE id = :id")
    MealPlan getById(int id);
    
    @Query("SELECT * FROM meal_plans ORDER BY meal_date ASC, meal_type ASC")
    LiveData<List<MealPlan>> getAllMealPlans();
    
    @Query("SELECT * FROM meal_plans WHERE meal_date = :date ORDER BY meal_type ASC")
    LiveData<List<MealPlan>> getMealPlansForDate(Date date);
    
    @Query("SELECT * FROM meal_plans WHERE meal_date BETWEEN :startDate AND :endDate ORDER BY meal_date ASC, meal_type ASC")
    LiveData<List<MealPlan>> getMealPlansForDateRange(Date startDate, Date endDate);
    
    @Query("SELECT * FROM meal_plans WHERE meal_date = :date AND meal_type = :mealType")
    MealPlan getMealPlanForDateTime(Date date, MealPlan.MealType mealType);
    
    @Query("SELECT * FROM meal_plans WHERE recipe_id = :recipeId ORDER BY meal_date DESC")
    LiveData<List<MealPlan>> getMealPlansByRecipe(String recipeId);
    
    @Query("SELECT DISTINCT meal_date FROM meal_plans WHERE meal_date BETWEEN :startDate AND :endDate ORDER BY meal_date ASC")
    List<Date> getPlannedDatesInRange(Date startDate, Date endDate);
    
    @Query("DELETE FROM meal_plans WHERE meal_date = :date AND meal_type = :mealType")
    void deleteMealPlan(Date date, MealPlan.MealType mealType);
    
    @Query("DELETE FROM meal_plans WHERE meal_date = :date")
    void deleteAllMealPlansForDate(Date date);
    
    @Query("DELETE FROM meal_plans WHERE meal_date BETWEEN :startDate AND :endDate")
    void deleteMealPlansInRange(Date startDate, Date endDate);
    
    @Query("SELECT COUNT(*) FROM meal_plans WHERE meal_date BETWEEN :startDate AND :endDate")
    int getMealPlanCountInRange(Date startDate, Date endDate);
    
    @Query("SELECT * FROM meal_plans WHERE is_completed = 0 AND meal_date <= :currentDate ORDER BY meal_date ASC")
    List<MealPlan> getUpcomingMealPlans(Date currentDate);
    
    // Méthodes de synchronisation avec Mealie
    @Query("SELECT * FROM meal_plans WHERE needs_sync = 1 OR is_synced = 0 ORDER BY created_at ASC")
    List<MealPlan> getUnsyncedMealPlansSync();
    
    @Query("SELECT * FROM meal_plans WHERE server_id = :serverId")
    MealPlan getMealPlanByServerIdSync(String serverId);
    
    @Query("UPDATE meal_plans SET is_synced = 1, needs_sync = 0, last_sync_date = :syncDate WHERE id = :id")
    void markAsSynced(int id, Date syncDate);
    
    @Query("UPDATE meal_plans SET needs_sync = 1, is_synced = 0 WHERE id = :id")
    void markAsNeedsSync(int id);
    
    @Query("SELECT COUNT(*) FROM meal_plans WHERE needs_sync = 1 OR is_synced = 0")
    int getUnsyncedCount();
    
    @Query("SELECT * FROM meal_plans WHERE last_sync_date IS NULL OR last_sync_date < :since ORDER BY created_at ASC")
    List<MealPlan> getMealPlansSyncedBefore(Date since);
    
    // Méthodes synchrones pour les opérations en arrière-plan
    @Insert
    long insertSync(MealPlan mealPlan);
    
    @Update
    void updateSync(MealPlan mealPlan);
    
    @Query("SELECT * FROM meal_plans WHERE id = :id")
    MealPlan getByIdSync(int id);
}
