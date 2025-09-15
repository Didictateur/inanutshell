package fr.didictateur.inanutshell.data.meal;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import fr.didictateur.inanutshell.data.AppDatabase;

import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager pour gérer les repas planifiés
 */
public class MealPlanManager {
    
    private static MealPlanManager instance;
    private MealPlanDao mealPlanDao;
    private ExecutorService executorService;
    private Context context;
    
    private MealPlanManager(Context context) {
        this.context = context;
        AppDatabase database = AppDatabase.getInstance(context);
        this.mealPlanDao = database.mealPlanDao();
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized MealPlanManager getInstance(Context context) {
        if (instance == null) {
            instance = new MealPlanManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // Créer un repas planifié
    public void createMealPlan(String recipeId, String recipeName, Date date, 
                              MealPlan.MealType mealType, int servings, String notes,
                              OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                // Vérifier s'il y a déjà un repas planifié pour cette date/heure
                MealPlan existing = mealPlanDao.getMealPlanForDateTime(date, mealType);
                if (existing != null) {
                    if (listener != null) {
                        listener.onError("Un repas est déjà planifié pour ce créneau");
                    }
                    return;
                }
                
                MealPlan mealPlan = new MealPlan(recipeId, recipeName, date, mealType);
                mealPlan.setServings(servings);
                mealPlan.setNotes(notes);
                
                long id = mealPlanDao.insert(mealPlan);
                
                if (listener != null) {
                    listener.onSuccess("Repas planifié avec succès");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la planification: " + e.getMessage());
                }
            }
        });
    }
    
    // Modifier un repas planifié
    public void updateMealPlan(MealPlan mealPlan, OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                mealPlanDao.update(mealPlan);
                if (listener != null) {
                    listener.onSuccess("Repas modifié avec succès");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la modification: " + e.getMessage());
                }
            }
        });
    }
    
    // Supprimer un repas planifié
    public void deleteMealPlan(MealPlan mealPlan, OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                mealPlanDao.delete(mealPlan);
                if (listener != null) {
                    listener.onSuccess("Repas supprimé du planning");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }
    
    // Supprimer un repas par date et type
    public void deleteMealPlan(Date date, MealPlan.MealType mealType, OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                mealPlanDao.deleteMealPlan(date, mealType);
                if (listener != null) {
                    listener.onSuccess("Repas supprimé du planning");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }
    
    // Obtenir les repas d'une date
    public LiveData<List<MealPlan>> getMealPlansForDate(Date date) {
        return mealPlanDao.getMealPlansForDate(date);
    }
    
    // Obtenir les repas d'une semaine
    public LiveData<List<MealPlan>> getMealPlansForWeek(Date weekStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(weekStart);
        
        // Début de semaine (lundi)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date startDate = calendar.getTime();
        
        // Fin de semaine (dimanche)
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date endDate = calendar.getTime();
        
        return mealPlanDao.getMealPlansForDateRange(startDate, endDate);
    }
    
    // Obtenir les repas d'un mois
    public LiveData<List<MealPlan>> getMealPlansForMonth(Date month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(month);
        
        // Premier jour du mois
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = calendar.getTime();
        
        // Dernier jour du mois
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = calendar.getTime();
        
        return mealPlanDao.getMealPlansForDateRange(startDate, endDate);
    }
    
    // Copier une semaine de planning
    public void copyWeekPlanning(Date sourceWeek, Date targetWeek, OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                Calendar sourceCal = Calendar.getInstance();
                sourceCal.setTime(sourceWeek);
                sourceCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                Date sourceStart = sourceCal.getTime();
                
                sourceCal.add(Calendar.DAY_OF_WEEK, 6);
                Date sourceEnd = sourceCal.getTime();
                
                // Récupérer les repas de la semaine source
                List<MealPlan> sourceMeals = mealPlanDao.getMealPlansForDateRange(sourceStart, sourceEnd).getValue();
                if (sourceMeals == null || sourceMeals.isEmpty()) {
                    if (listener != null) {
                        listener.onError("Aucun repas à copier pour cette semaine");
                    }
                    return;
                }
                
                Calendar targetCal = Calendar.getInstance();
                targetCal.setTime(targetWeek);
                targetCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                
                int copiedCount = 0;
                for (MealPlan sourceMeal : sourceMeals) {
                    // Calculer le décalage en jours
                    Calendar sourceMealCal = Calendar.getInstance();
                    sourceMealCal.setTime(sourceMeal.getDate());
                    
                    int dayOffset = sourceMealCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
                    
                    Calendar targetMealCal = (Calendar) targetCal.clone();
                    targetMealCal.add(Calendar.DAY_OF_WEEK, dayOffset);
                    
                    // Vérifier si un repas existe déjà
                    MealPlan existing = mealPlanDao.getMealPlanForDateTime(
                        targetMealCal.getTime(), sourceMeal.getMealType());
                    
                    if (existing == null) {
                        MealPlan newMeal = new MealPlan();
                        newMeal.setRecipeId(sourceMeal.getRecipeId());
                        newMeal.setRecipeName(sourceMeal.getRecipeName());
                        newMeal.setDate(targetMealCal.getTime());
                        newMeal.setMealType(sourceMeal.getMealType());
                        newMeal.setServings(sourceMeal.getServings());
                        newMeal.setNotes(sourceMeal.getNotes());
                        
                        mealPlanDao.insert(newMeal);
                        copiedCount++;
                    }
                }
                
                if (listener != null) {
                    listener.onSuccess(copiedCount + " repas copiés avec succès");
                }
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la copie: " + e.getMessage());
                }
            }
        });
    }
    
    // Générer un planning automatique
    public void generateWeeklyMenu(Date weekStart, List<String> preferredRecipeIds, 
                                  OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                if (preferredRecipeIds == null || preferredRecipeIds.isEmpty()) {
                    if (listener != null) {
                        listener.onError("Aucune recette disponible pour la génération");
                    }
                    return;
                }
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(weekStart);
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                
                int generatedCount = 0;
                
                // Générer pour chaque jour de la semaine
                for (int day = 0; day < 7; day++) {
                    Date currentDate = calendar.getTime();
                    
                    // Générer déjeuner et dîner (pas petit-déjeuner)
                    MealPlan.MealType[] mealTypes = {MealPlan.MealType.LUNCH, MealPlan.MealType.DINNER};
                    
                    for (MealPlan.MealType mealType : mealTypes) {
                        // Vérifier si un repas existe déjà
                        MealPlan existing = mealPlanDao.getMealPlanForDateTime(currentDate, mealType);
                        
                        if (existing == null) {
                            // Sélectionner une recette aléatoire
                            int randomIndex = (int) (Math.random() * preferredRecipeIds.size());
                            String recipeId = preferredRecipeIds.get(randomIndex);
                            
                            // Récupérer le nom de la recette (supposer qu'on l'a)
                            String recipeName = "Recette " + recipeId; // À améliorer
                            
                            MealPlan mealPlan = new MealPlan(recipeId, recipeName, currentDate, mealType);
                            mealPlan.setServings(4); // Valeur par défaut
                            
                            mealPlanDao.insert(mealPlan);
                            generatedCount++;
                        }
                    }
                    
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                if (listener != null) {
                    listener.onSuccess(generatedCount + " repas générés automatiquement");
                }
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur lors de la génération: " + e.getMessage());
                }
            }
        });
    }
    
    // Marquer un repas comme terminé
    public void markMealAsCompleted(MealPlan mealPlan, OnMealPlanActionListener listener) {
        executorService.execute(() -> {
            try {
                mealPlan.setCompleted(true);
                mealPlanDao.update(mealPlan);
                
                if (listener != null) {
                    listener.onSuccess("Repas marqué comme terminé");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Erreur: " + e.getMessage());
                }
            }
        });
    }
    
    // Interface pour les callbacks
    public interface OnMealPlanActionListener {
        void onSuccess(String message);
        void onError(String error);
    }
}
