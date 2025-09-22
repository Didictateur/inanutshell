package fr.didictateur.inanutshell.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.database.AppDatabase;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.sync.model.SyncItem;
import fr.didictateur.inanutshell.sync.model.SyncStatus;
import fr.didictateur.inanutshell.sync.model.ConflictResolution;

import java.util.*;
import java.util.concurrent.*;

/**
 * Gestionnaire principal de synchronisation temps réel
 * Gère la sync multi-appareils, résolution de conflits et mode offline
 */
public class SyncManager {
    
    private static final String TAG = "SyncManager";
    private static final String PREFS_NAME = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    
    private static SyncManager instance;
    private final Context context;
    private final AppDatabase database;
    private final NetworkManager networkManager;
    private final SharedPreferences syncPrefs;
    private final ExecutorService executorService;
    
    // État de synchronisation
    private final MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>();
    private final MutableLiveData<List<SyncItem>> pendingItems = new MutableLiveData<>();
    private final Queue<SyncItem> syncQueue = new ConcurrentLinkedQueue<>();
    
    // Gestion des conflits
    private final ConflictResolver conflictResolver;
    private final OfflineSyncManager offlineManager;
    
    // Configuration
    private boolean autoSyncEnabled = true;
    private long syncInterval = 30000; // 30 secondes
    private String deviceId;
    
    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.networkManager = NetworkManager.getInstance(context);
        this.syncPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newCachedThreadPool();
        
        this.conflictResolver = new ConflictResolver();
        this.offlineManager = new OfflineSyncManager(context);
        
        initializeDeviceId();
        initializeSyncStatus();
    }
    
    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }
    
    /**
     * Initialise l'ID unique de l'appareil
     */
    private void initializeDeviceId() {
        deviceId = syncPrefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            syncPrefs.edit()
                .putString(KEY_DEVICE_ID, deviceId)
                .apply();
        }
        Log.d(TAG, "Device ID: " + deviceId);
    }
    
    /**
     * Initialise le statut de synchronisation
     */
    private void initializeSyncStatus() {
        long lastSync = syncPrefs.getLong(KEY_LAST_SYNC, 0);
        boolean syncEnabled = syncPrefs.getBoolean(KEY_SYNC_ENABLED, true);
        
        SyncStatus status = new SyncStatus(
            syncEnabled ? SyncStatus.State.IDLE : SyncStatus.State.DISABLED,
            lastSync,
            0,
            0,
            null
        );
        syncStatus.setValue(status);
        
        // Initialiser la liste des éléments en attente
        pendingItems.setValue(new ArrayList<>());
    }
    
    // ===== API PUBLIQUE =====
    
    /**
     * Lance une synchronisation complète
     */
    public void startFullSync() {
        Log.d(TAG, "Démarrage synchronisation complète");
        updateSyncStatus(SyncStatus.State.SYNCING, "Synchronisation en cours...");
        
        executorService.execute(() -> {
            try {
                // 1. Synchroniser les recettes
                syncRecipes();
                
                // 2. Synchroniser les meal plans
                syncMealPlans();
                
                // 3. Synchroniser les listes de courses
                syncShoppingLists();
                
                // 4. Traiter les conflits
                processConflicts();
                
                // 5. Mettre à jour le timestamp
                updateLastSyncTimestamp();
                
                updateSyncStatus(SyncStatus.State.COMPLETED, "Synchronisation terminée");
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la synchronisation", e);
                updateSyncStatus(SyncStatus.State.ERROR, "Erreur: " + e.getMessage());
            }
        });
    }
    
    /**
     * Synchronise une recette spécifique
     */
    public void syncRecipe(Recipe recipe, SyncItem.Action action) {
        SyncItem item = new SyncItem(
            recipe.getId(),
            SyncItem.Type.RECIPE,
            action,
            System.currentTimeMillis(),
            deviceId,
            recipe
        );
        
        addToSyncQueue(item);
        
        if (autoSyncEnabled) {
            processSyncQueue();
        }
    }
    
    /**
     * Active/désactive la synchronisation automatique
     */
    public void setSyncEnabled(boolean enabled) {
        syncPrefs.edit()
            .putBoolean(KEY_SYNC_ENABLED, enabled)
            .apply();
        
        autoSyncEnabled = enabled;
        
        if (enabled) {
            updateSyncStatus(SyncStatus.State.IDLE, "Synchronisation activée");
            startPeriodicSync();
        } else {
            updateSyncStatus(SyncStatus.State.DISABLED, "Synchronisation désactivée");
            stopPeriodicSync();
        }
    }
    
    /**
     * Configure l'intervalle de synchronisation automatique
     */
    public void setSyncInterval(long intervalMs) {
        this.syncInterval = intervalMs;
        if (autoSyncEnabled) {
            restartPeriodicSync();
        }
    }
    
    // ===== OBSERVABLES =====
    
    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatus;
    }
    
    public LiveData<List<SyncItem>> getPendingItems() {
        return pendingItems;
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    private void syncRecipes() {
        Log.d(TAG, "Synchronisation des recettes");
        
        networkManager.getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> serverRecipes) {
                // Comparer avec les recettes locales
                executorService.execute(() -> {
                    List<Recipe> localRecipes = database.recipeDao().getAllRecipesSync();
                    mergeRecipes(localRecipes, serverRecipes);
                });
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Impossible de synchroniser les recettes: " + error);
                // Passer en mode offline
                offlineManager.handleSyncFailure(SyncItem.Type.RECIPE);
            }
        });
    }
    
    private void syncMealPlans() {
        Log.d(TAG, "Synchronisation des meal plans");
        // TODO: Implémenter la sync des meal plans
    }
    
    private void syncShoppingLists() {
        Log.d(TAG, "Synchronisation des listes de courses");
        // TODO: Implémenter la sync des listes de courses
    }
    
    private void mergeRecipes(List<Recipe> local, List<Recipe> server) {
        for (Recipe serverRecipe : server) {
            Recipe localRecipe = findRecipeById(local, serverRecipe.getId());
            
            if (localRecipe == null) {
                // Nouvelle recette du serveur
                database.recipeDao().insert(serverRecipe);
                Log.d(TAG, "Nouvelle recette ajoutée: " + serverRecipe.getName());
            } else {
                // Vérifier s'il y a un conflit
                if (hasConflict(localRecipe, serverRecipe)) {
                    conflictResolver.addConflict(localRecipe, serverRecipe);
                } else {
                    // Prendre la version la plus récente
                    if (serverRecipe.getUpdatedAtTimestamp() > localRecipe.getUpdatedAtTimestamp()) {
                        database.recipeDao().update(serverRecipe);
                        Log.d(TAG, "Recette mise à jour: " + serverRecipe.getName());
                    }
                }
            }
        }
    }
    
    private Recipe findRecipeById(List<Recipe> recipes, String id) {
        for (Recipe recipe : recipes) {
            if (recipe.getId().equals(id)) {
                return recipe;
            }
        }
        return null;
    }
    
    private boolean hasConflict(Recipe local, Recipe server) {
        // Conflit si les deux ont été modifiées depuis la dernière sync
        long lastSync = getLastSyncTimestamp();
        return local.getUpdatedAtTimestamp() > lastSync && server.getUpdatedAtTimestamp() > lastSync;
    }
    
    private void processConflicts() {
        List<ConflictResolution> conflicts = conflictResolver.getConflicts();
        if (!conflicts.isEmpty()) {
            Log.d(TAG, "Traitement de " + conflicts.size() + " conflits");
            // Notifier l'utilisateur des conflits
            updateSyncStatus(SyncStatus.State.CONFLICTS, conflicts.size() + " conflits détectés");
        }
    }
    
    private void addToSyncQueue(SyncItem item) {
        syncQueue.offer(item);
        
        // Mettre à jour la liste observable
        List<SyncItem> current = pendingItems.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(item);
        pendingItems.postValue(current);
    }
    
    private void processSyncQueue() {
        executorService.execute(() -> {
            while (!syncQueue.isEmpty()) {
                SyncItem item = syncQueue.poll();
                processSyncItem(item);
            }
        });
    }
    
    private void processSyncItem(SyncItem item) {
        Log.d(TAG, "Traitement élément sync: " + item.getType() + " " + item.getAction());
        
        switch (item.getType()) {
            case RECIPE:
                processRecipeSync(item);
                break;
            case MEAL_PLAN:
                processMealPlanSync(item);
                break;
            case SHOPPING_LIST:
                processShoppingListSync(item);
                break;
        }
    }
    
    private void processRecipeSync(SyncItem item) {
        Recipe recipe = (Recipe) item.getData();
        
        switch (item.getAction()) {
            case CREATE:
                networkManager.createRecipe(recipe, new NetworkManager.CreateRecipeCallback() {
                    @Override
                    public void onSuccess(Recipe createdRecipe) {
                        Log.d(TAG, "Recette créée sur le serveur: " + createdRecipe.getName());
                        removePendingItem(item);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Erreur création recette: " + error);
                        offlineManager.storePendingItem(item);
                    }
                });
                break;
                
            case UPDATE:
                networkManager.updateRecipe(recipe.getId(), recipe, new NetworkManager.UpdateRecipeCallback() {
                    @Override
                    public void onSuccess(Recipe updatedRecipe) {
                        Log.d(TAG, "Recette mise à jour sur le serveur: " + updatedRecipe.getName());
                        removePendingItem(item);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Erreur mise à jour recette: " + error);
                        offlineManager.storePendingItem(item);
                    }
                });
                break;
                
            case DELETE:
                networkManager.deleteRecipe(recipe.getId(), new NetworkManager.DeleteRecipeCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Recette supprimée du serveur: " + recipe.getName());
                        removePendingItem(item);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Erreur suppression recette: " + error);
                        offlineManager.storePendingItem(item);
                    }
                });
                break;
        }
    }
    
    private void processMealPlanSync(SyncItem item) {
        // TODO: Implémenter la sync des meal plans
        Log.d(TAG, "Sync meal plan non implémentée");
    }
    
    private void processShoppingListSync(SyncItem item) {
        // TODO: Implémenter la sync des listes de courses
        Log.d(TAG, "Sync shopping list non implémentée");
    }
    
    private void removePendingItem(SyncItem item) {
        List<SyncItem> current = pendingItems.getValue();
        if (current != null) {
            current.remove(item);
            pendingItems.postValue(current);
        }
    }
    
    private void updateSyncStatus(SyncStatus.State state, String message) {
        SyncStatus current = syncStatus.getValue();
        if (current == null) {
            current = new SyncStatus(state, 0, 0, 0, message);
        } else {
            current = new SyncStatus(
                state,
                current.getLastSync(),
                current.getTotalItems(),
                current.getProcessedItems(),
                message
            );
        }
        syncStatus.postValue(current);
    }
    
    private long getLastSyncTimestamp() {
        return syncPrefs.getLong(KEY_LAST_SYNC, 0);
    }
    
    private void updateLastSyncTimestamp() {
        syncPrefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply();
    }
    
    private void startPeriodicSync() {
        // TODO: Implémenter sync périodique avec WorkManager
        Log.d(TAG, "Démarrage sync périodique");
    }
    
    private void stopPeriodicSync() {
        // TODO: Arrêter sync périodique
        Log.d(TAG, "Arrêt sync périodique");
    }
    
    private void restartPeriodicSync() {
        stopPeriodicSync();
        startPeriodicSync();
    }
}
