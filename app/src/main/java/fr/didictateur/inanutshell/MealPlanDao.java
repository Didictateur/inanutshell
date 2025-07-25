package fr.didictateur.inanutshell;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MealPlanDao {
    
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY mealType")
    List<MealPlan> getMealPlansForDate(String date);
    
    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date, mealType")
    List<MealPlan> getMealPlansForDateRange(String startDate, String endDate);
    
    @Query("SELECT mp.*, r.titre as recetteTitre, r.ingredients as recetteIngredients FROM meal_plans mp " +
           "LEFT JOIN recette r ON mp.recetteId = r.id " +
           "WHERE mp.date = :date ORDER BY " +
           "CASE mp.mealType " +
           "WHEN 'breakfast' THEN 1 " +
           "WHEN 'lunch' THEN 2 " +
           "WHEN 'dinner' THEN 3 " +
           "ELSE 4 END")
    List<MealPlanWithRecette> getMealPlansWithRecetteForDate(String date);
    
    @Query("SELECT mp.*, r.titre as recetteTitre, r.tempsPrep, r.taille, r.ingredients as recetteIngredients " +
           "FROM meal_plans mp " +
           "LEFT JOIN recette r ON mp.recetteId = r.id " +
           "WHERE mp.date BETWEEN :startDate AND :endDate " +
           "ORDER BY mp.date, " +
           "CASE mp.mealType " +
           "WHEN 'breakfast' THEN 1 " +
           "WHEN 'lunch' THEN 2 " +
           "WHEN 'dinner' THEN 3 " +
           "ELSE 4 END")
    List<MealPlanWithRecette> getMealPlansWithRecetteForWeek(String startDate, String endDate);
    
    @Insert
    void insert(MealPlan mealPlan);
    
    @Update
    void update(MealPlan mealPlan);
    
    @Delete
    void delete(MealPlan mealPlan);
    
    @Query("DELETE FROM meal_plans WHERE date = :date AND mealType = :mealType")
    void deleteMealPlan(String date, String mealType);
}
