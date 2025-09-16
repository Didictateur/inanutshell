package fr.didictateur.inanutshell.data.meal;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import fr.didictateur.inanutshell.AppDatabase;
import fr.didictateur.inanutshell.data.meal.MealPlanDao;
import fr.didictateur.inanutshell.data.meal.MealPlan;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MealPlanManager {
    private static MealPlanManager instance;
    private final MealPlanDao mealPlanDao;
    private final ExecutorService executor;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private MealPlanManager(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.mealPlanDao = database.mealPlanDao();
        this.executor = Executors.newFixedThreadPool(3);
    }

    public static synchronized MealPlanManager getInstance(Context context) {
        if (instance == null) {
            instance = new MealPlanManager(context.getApplicationContext());
        }
        return instance;
    }

    public void createMealPlan(MealPlan mealPlan, MealPlanCallback callback) {
        executor.execute(() -> {
            try {
                isLoading.postValue(true);
                long id = mealPlanDao.insert(mealPlan);
                mealPlan.setId((int) id);
                callback.onSuccess(mealPlan);
                isLoading.postValue(false);
            } catch (Exception e) {
                callback.onError("Failed to create meal plan: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void updateMealPlan(MealPlan mealPlan, MealPlanCallback callback) {
        executor.execute(() -> {
            try {
                isLoading.postValue(true);
                mealPlanDao.update(mealPlan);
                callback.onSuccess(mealPlan);
                isLoading.postValue(false);
            } catch (Exception e) {
                callback.onError("Failed to update meal plan: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void deleteMealPlan(MealPlan mealPlan, MealPlanCallback callback) {
        executor.execute(() -> {
            try {
                isLoading.postValue(true);
                mealPlanDao.delete(mealPlan);
                callback.onSuccess(mealPlan);
                isLoading.postValue(false);
            } catch (Exception e) {
                callback.onError("Failed to delete meal plan: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public LiveData<List<MealPlan>> getAllMealPlans() {
        return mealPlanDao.getAllMealPlans();
    }

    public LiveData<List<MealPlan>> getMealPlansForDate(Date date) {
        return mealPlanDao.getMealPlansForDate(date);
    }

    public void markMealAsCompleted(MealPlan mealPlan, OnMealPlanActionListener callback) {
        executor.execute(() -> {
            try {
                isLoading.postValue(true);
                // Marquer comme complété et mettre à jour
                mealPlan.setCompleted(true);
                mealPlanDao.update(mealPlan);
                callback.onSuccess(mealPlan);
                isLoading.postValue(false);
            } catch (Exception e) {
                callback.onError("Failed to mark meal as completed: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public interface MealPlanCallback {
        void onSuccess(MealPlan mealPlan);
        void onError(String error);
    }

    // Alias for backward compatibility
    public interface OnMealPlanActionListener extends MealPlanCallback {
    }
}
