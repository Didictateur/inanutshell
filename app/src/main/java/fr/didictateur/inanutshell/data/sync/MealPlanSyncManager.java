package fr.didictateur.inanutshell.data.sync;

import android.content.Context;
import android.util.Log;

import fr.didictateur.inanutshell.data.api.MealieApiService;
import fr.didictateur.inanutshell.data.model.MealieMealPlan;
import fr.didictateur.inanutshell.data.meal.MealPlan;
import fr.didictateur.inanutshell.data.meal.MealPlanDao;
import fr.didictateur.inanutshell.data.response.MealPlanListResponse;
import fr.didictateur.inanutshell.data.network.NetworkManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire de synchronisation entre les meal plans locaux et l'API Mealie
 */
public class MealPlanSyncManager {
    
    private static final String TAG = "MealPlanSync";
    private static MealPlanSyncManager instance;
    
    private final Context context;
    private final MealPlanDao mealPlanDao;
    private final NetworkManager networkManager;
    private final ExecutorService executor;
    
    // Format de date pour l'API Mealie
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    
    private MealPlanSyncManager(Context context, MealPlanDao mealPlanDao) {
        this.context = context.getApplicationContext();
        this.mealPlanDao = mealPlanDao;
        this.networkManager = NetworkManager.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized MealPlanSyncManager getInstance(Context context, MealPlanDao mealPlanDao) {
        if (instance == null) {
            instance = new MealPlanSyncManager(context, mealPlanDao);
        }
        return instance;
    }
    
    /**
     * Interface pour les callbacks de synchronisation
     */
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int current, int total);
    }
    
    /**
     * Synchronise tous les meal plans avec le serveur Mealie
     */
    public void syncAllMealPlans(SyncCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Récupérer les meal plans du serveur
                downloadMealPlansFromServer(new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000), // 30 jours passés
                                          new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000), // 30 jours futurs
                                          new SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // 2. Upload les meal plans locaux non synchronisés
                        uploadLocalMealPlans(callback);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Erreur download: " + error);
                    }
                    
                    @Override
                    public void onProgress(int current, int total) {
                        callback.onProgress(current, total * 2); // 50% pour download
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur sync meal plans", e);
                callback.onError("Erreur de synchronisation: " + e.getMessage());
            }
        });
    }
    
    /**
     * Télécharge les meal plans depuis le serveur
     */
    private void downloadMealPlansFromServer(Date startDate, Date endDate, SyncCallback callback) {
        String authHeader = networkManager.getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        MealieApiService apiService = networkManager.getApiService();
        String start = dateFormat.format(startDate);
        String end = dateFormat.format(endDate);
        
        Call<MealPlanListResponse> call = apiService.getMealPlans(authHeader, 1, 1000, start, end);
        
        call.enqueue(new Callback<MealPlanListResponse>() {
            @Override
            public void onResponse(Call<MealPlanListResponse> call, Response<MealPlanListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MealieMealPlan> serverMealPlans = response.body().getItems();
                    Log.d(TAG, "Téléchargé " + serverMealPlans.size() + " meal plans du serveur");
                    
                    // Convertir et sauvegarder les meal plans en local
                    executor.execute(() -> {
                        try {
                            int processed = 0;
                            for (MealieMealPlan serverPlan : serverMealPlans) {
                                MealPlan localPlan = convertMealieToLocal(serverPlan);
                                if (localPlan != null) {
                                    // Vérifier si existe déjà en local
                                    MealPlan existing = mealPlanDao.getMealPlanByServerIdSync(serverPlan.getId());
                                    if (existing != null) {
                                        // Mettre à jour
                                        localPlan.id = existing.id;
                                        mealPlanDao.updateSync(localPlan);
                                    } else {
                                        // Insérer nouveau
                                        mealPlanDao.insertSync(localPlan);
                                    }
                                }
                                processed++;
                                final int current = processed;
                                callback.onProgress(current, serverMealPlans.size());
                            }
                            callback.onSuccess("Téléchargé " + processed + " meal plans");
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur sauvegarde meal plans", e);
                            callback.onError("Erreur sauvegarde: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Erreur API meal plans: " + response.code());
                    callback.onError("Erreur serveur: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<MealPlanListResponse> call, Throwable t) {
                Log.e(TAG, "Erreur réseau meal plans", t);
                callback.onError("Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Upload les meal plans locaux vers le serveur
     */
    private void uploadLocalMealPlans(SyncCallback callback) {
        executor.execute(() -> {
            try {
                // Récupérer les meal plans locaux non synchronisés
                List<MealPlan> localPlans = mealPlanDao.getUnsyncedMealPlansSync();
                Log.d(TAG, "Upload " + localPlans.size() + " meal plans locaux");
                
                if (localPlans.isEmpty()) {
                    callback.onSuccess("Synchronisation terminée - aucun meal plan à uploader");
                    return;
                }
                
                String authHeader = networkManager.getAuthHeader();
                MealieApiService apiService = networkManager.getApiService();
                
                int processed = 0;
                int errors = 0;
                
                for (MealPlan localPlan : localPlans) {
                    try {
                        MealieMealPlan mealiePlan = convertLocalToMealie(localPlan);
                        
                        // Upload vers le serveur
                        Call<MealieMealPlan> call = apiService.createMealPlan(authHeader, mealiePlan);
                        Response<MealieMealPlan> response = call.execute();
                        
                        if (response.isSuccessful() && response.body() != null) {
                            // Marquer comme synchronisé
                            localPlan.serverId = response.body().getId();
                            localPlan.isSynced = true;
                            localPlan.lastSyncDate = new Date();
                            mealPlanDao.updateSync(localPlan);
                            Log.d(TAG, "Meal plan uploadé: " + localPlan.recipeName);
                        } else {
                            Log.e(TAG, "Erreur upload meal plan: " + response.code());
                            errors++;
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur upload meal plan individuel", e);
                        errors++;
                    }
                    
                    processed++;
                    callback.onProgress(localPlans.size() + processed, localPlans.size() * 2);
                }
                
                if (errors == 0) {
                    callback.onSuccess("Synchronisation terminée avec succès - " + processed + " meal plans uploadés");
                } else {
                    callback.onError("Synchronisation terminée avec " + errors + " erreurs sur " + processed + " meal plans");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur upload meal plans", e);
                callback.onError("Erreur upload: " + e.getMessage());
            }
        });
    }
    
    /**
     * Convertit un MealieMealPlan en MealPlan local
     */
    private MealPlan convertMealieToLocal(MealieMealPlan mealiePlan) {
        try {
            MealPlan localPlan = new MealPlan();
            
            localPlan.serverId = mealiePlan.getId();
            localPlan.recipeId = mealiePlan.getRecipeId();
            localPlan.recipeName = mealiePlan.getTitle();
            localPlan.mealDate = dateFormat.parse(mealiePlan.getDate());
            localPlan.mealType = convertMealieTypeToLocal(mealiePlan.getEntryType());
            localPlan.notes = mealiePlan.getText();
            localPlan.servings = 1; // Default, Mealie n'a pas cette info
            localPlan.isSynced = true;
            localPlan.lastSyncDate = new Date();
            
            return localPlan;
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur conversion Mealie -> Local", e);
            return null;
        }
    }
    
    /**
     * Convertit un MealPlan local en MealieMealPlan
     */
    private MealieMealPlan convertLocalToMealie(MealPlan localPlan) {
        MealieMealPlan mealiePlan = new MealieMealPlan();
        
        mealiePlan.setDate(dateFormat.format(localPlan.mealDate));
        mealiePlan.setEntryType(convertLocalTypeToMealie(localPlan.mealType));
        mealiePlan.setTitle(localPlan.recipeName);
        mealiePlan.setText(localPlan.notes);
        mealiePlan.setRecipeId(localPlan.recipeId);
        
        return mealiePlan;
    }
    
    /**
     * Convertit le type de repas Mealie vers local
     */
    private MealPlan.MealType convertMealieTypeToLocal(String mealieType) {
        if (mealieType == null) return MealPlan.MealType.LUNCH;
        
        switch (mealieType.toLowerCase()) {
            case "breakfast": return MealPlan.MealType.BREAKFAST;
            case "lunch": return MealPlan.MealType.LUNCH;
            case "dinner": return MealPlan.MealType.DINNER;
            case "side":
            case "snack": return MealPlan.MealType.SNACK;
            default: return MealPlan.MealType.LUNCH;
        }
    }
    
    /**
     * Convertit le type de repas local vers Mealie
     */
    private String convertLocalTypeToMealie(MealPlan.MealType localType) {
        switch (localType) {
            case BREAKFAST: return "breakfast";
            case LUNCH: return "lunch";
            case DINNER: return "dinner";
            case SNACK: return "side";
            default: return "lunch";
        }
    }
    
    /**
     * Synchronise un meal plan spécifique
     */
    public void syncSingleMealPlan(MealPlan mealPlan, SyncCallback callback) {
        if (mealPlan.isSynced && mealPlan.serverId != null) {
            // Meal plan déjà synchronisé, mettre à jour sur le serveur
            updateMealPlanOnServer(mealPlan, callback);
        } else {
            // Nouveau meal plan, créer sur le serveur
            createMealPlanOnServer(mealPlan, callback);
        }
    }
    
    private void createMealPlanOnServer(MealPlan mealPlan, SyncCallback callback) {
        String authHeader = networkManager.getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        MealieMealPlan mealiePlan = convertLocalToMealie(mealPlan);
        MealieApiService apiService = networkManager.getApiService();
        
        Call<MealieMealPlan> call = apiService.createMealPlan(authHeader, mealiePlan);
        call.enqueue(new Callback<MealieMealPlan>() {
            @Override
            public void onResponse(Call<MealieMealPlan> call, Response<MealieMealPlan> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Marquer comme synchronisé
                    executor.execute(() -> {
                        mealPlan.serverId = response.body().getId();
                        mealPlan.isSynced = true;
                        mealPlan.lastSyncDate = new Date();
                        mealPlanDao.updateSync(mealPlan);
                    });
                    callback.onSuccess("Meal plan créé sur le serveur");
                } else {
                    callback.onError("Erreur création serveur: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<MealieMealPlan> call, Throwable t) {
                callback.onError("Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    private void updateMealPlanOnServer(MealPlan mealPlan, SyncCallback callback) {
        String authHeader = networkManager.getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        MealieMealPlan mealiePlan = convertLocalToMealie(mealPlan);
        MealieApiService apiService = networkManager.getApiService();
        
        Call<MealieMealPlan> call = apiService.updateMealPlan(authHeader, mealPlan.serverId, mealiePlan);
        call.enqueue(new Callback<MealieMealPlan>() {
            @Override
            public void onResponse(Call<MealieMealPlan> call, Response<MealieMealPlan> response) {
                if (response.isSuccessful()) {
                    executor.execute(() -> {
                        mealPlan.lastSyncDate = new Date();
                        mealPlanDao.updateSync(mealPlan);
                    });
                    callback.onSuccess("Meal plan mis à jour sur le serveur");
                } else {
                    callback.onError("Erreur mise à jour serveur: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<MealieMealPlan> call, Throwable t) {
                callback.onError("Erreur réseau: " + t.getMessage());
            }
        });
    }
}
