package fr.didictateur.inanutshell.ui.sync;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import fr.didictateur.inanutshell.sync.SyncManager;
import fr.didictateur.inanutshell.sync.ConflictResolver;
import fr.didictateur.inanutshell.sync.OfflineSyncManager;
import fr.didictateur.inanutshell.sync.model.ConflictResolution;
import fr.didictateur.inanutshell.sync.model.SyncItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel pour l'activité de synchronisation
 */
public class SyncViewModel extends AndroidViewModel {
    
    private final SyncManager syncManager;
    private final ConflictResolver conflictResolver;
    private final OfflineSyncManager offlineManager;
    private final ExecutorService executorService;
    
    private final MutableLiveData<List<ConflictResolution>> conflicts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public SyncViewModel(@NonNull Application application) {
        super(application);
        
        this.syncManager = SyncManager.getInstance(application);
        this.conflictResolver = new ConflictResolver();
        this.offlineManager = new OfflineSyncManager(application);
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialiser les données
        isLoading.setValue(false);
        refreshConflicts();
    }
    
    // ===== OBSERVABLES =====
    
    public LiveData<List<ConflictResolution>> getConflicts() {
        return conflicts;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // ===== ACTIONS PUBLIQUES =====
    
    /**
     * Rafraîchit toutes les données
     */
    public void refreshData() {
        refreshConflicts();
    }
    
    /**
     * Démarre une synchronisation complète
     */
    public void startFullSync() {
        syncManager.startFullSync();
    }
    
    /**
     * Active/désactive la synchronisation automatique
     */
    public void setAutoSyncEnabled(boolean enabled) {
        syncManager.setSyncEnabled(enabled);
    }
    
    /**
     * Résout un conflit avec la stratégie spécifiée
     */
    public void resolveConflict(String conflictId, ConflictResolution.Strategy strategy, Object customVersion) {
        executorService.execute(() -> {
            try {
                conflictResolver.resolveConflict(conflictId, strategy, customVersion);
                
                // Rafraîchir la liste des conflits
                refreshConflicts();
                
            } catch (Exception e) {
                error.postValue("Erreur lors de la résolution du conflit: " + e.getMessage());
            }
        });
    }
    
    /**
     * Supprime un conflit résolu
     */
    public void removeResolvedConflict(String conflictId) {
        executorService.execute(() -> {
            conflictResolver.removeConflict(conflictId);
            refreshConflicts();
        });
    }
    
    /**
     * Nettoie tous les conflits résolus
     */
    public void clearResolvedConflicts() {
        executorService.execute(() -> {
            conflictResolver.clearResolvedConflicts();
            refreshConflicts();
        });
    }
    
    /**
     * Relance la synchronisation des éléments en attente
     */
    public void retryPendingItems() {
        isLoading.setValue(true);
        
        offlineManager.getPendingItems(new OfflineSyncManager.PendingItemsCallback() {
            @Override
            public void onSuccess(List<SyncItem> items) {
                // Relancer chaque élément dans la file de sync
                for (SyncItem item : items) {
                    if (item.shouldRetry()) {
                        syncManager.syncRecipe(
                            (fr.didictateur.inanutshell.data.model.Recipe) item.getData(),
                            item.getAction()
                        );
                    }
                }
                isLoading.postValue(false);
            }
            
            @Override
            public void onError(String errorMsg) {
                error.postValue("Impossible de relancer les éléments: " + errorMsg);
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Nettoie les éléments terminés
     */
    public void clearCompletedItems() {
        executorService.execute(() -> {
            try {
                offlineManager.cleanupOldItems();
                // Les observables des pending items se mettront à jour automatiquement
                
            } catch (Exception e) {
                error.postValue("Erreur lors du nettoyage: " + e.getMessage());
            }
        });
    }
    
    /**
     * Supprime un élément pending spécifique
     */
    public void removePendingItem(SyncItem item) {
        executorService.execute(() -> {
            try {
                offlineManager.markAsSynced(item);
                // L'observable se mettra à jour automatiquement
                
            } catch (Exception e) {
                error.postValue("Erreur lors de la suppression: " + e.getMessage());
            }
        });
    }
    
    /**
     * Force la synchronisation d'un élément spécifique
     */
    public void forceSyncItem(SyncItem item) {
        if (item.getData() instanceof fr.didictateur.inanutshell.data.model.Recipe) {
            syncManager.syncRecipe(
                (fr.didictateur.inanutshell.data.model.Recipe) item.getData(),
                item.getAction()
            );
        }
    }
    
    /**
     * Obtient les statistiques de synchronisation
     */
    public void getSyncStats(SyncStatsCallback callback) {
        executorService.execute(() -> {
            try {
                offlineManager.getPendingCount(new OfflineSyncManager.CountCallback() {
                    @Override
                    public void onCount(int pendingCount) {
                        int conflictCount = conflictResolver.getUnresolvedConflictCount();
                        
                        SyncStats stats = new SyncStats(pendingCount, conflictCount);
                        callback.onStats(stats);
                    }
                });
                
            } catch (Exception e) {
                callback.onError("Erreur lors de la récupération des stats: " + e.getMessage());
            }
        });
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    private void refreshConflicts() {
        executorService.execute(() -> {
            try {
                List<ConflictResolution> currentConflicts = conflictResolver.getConflicts();
                conflicts.postValue(currentConflicts);
                
            } catch (Exception e) {
                error.postValue("Erreur lors du rafraîchissement des conflits: " + e.getMessage());
            }
        });
    }
    
    // ===== CLASSES UTILITAIRES =====
    
    public static class SyncStats {
        public final int pendingCount;
        public final int conflictCount;
        
        public SyncStats(int pendingCount, int conflictCount) {
            this.pendingCount = pendingCount;
            this.conflictCount = conflictCount;
        }
    }
    
    public interface SyncStatsCallback {
        void onStats(SyncStats stats);
        void onError(String error);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
