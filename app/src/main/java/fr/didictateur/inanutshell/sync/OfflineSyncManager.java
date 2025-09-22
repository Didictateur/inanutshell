package fr.didictateur.inanutshell.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.room.Room;

import fr.didictateur.inanutshell.sync.model.SyncItem;
import fr.didictateur.inanutshell.sync.database.OfflineSyncDatabase;
import fr.didictateur.inanutshell.sync.database.PendingSync;
import fr.didictateur.inanutshell.sync.database.PendingSyncDao;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire de synchronisation hors ligne
 * Stocke les modifications en attente et les synchronise dès que possible
 */
public class OfflineSyncManager {
    
    private static final String TAG = "OfflineSyncManager";
    private static final String PREFS_NAME = "offline_sync_prefs";
    private static final String KEY_ENABLED = "offline_sync_enabled";
    
    private final Context context;
    private final OfflineSyncDatabase database;
    private final PendingSyncDao pendingSyncDao;
    private final SharedPreferences prefs;
    private final ExecutorService executorService;
    
    private boolean offlineModeEnabled = true;
    
    public OfflineSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = OfflineSyncDatabase.getInstance(context);
        this.pendingSyncDao = database.pendingSyncDao();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Charger les préférences
        loadPreferences();
    }
    
    // ===== API PUBLIQUE =====
    
    /**
     * Stocke un élément de synchronisation pour traitement ultérieur
     */
    public void storePendingItem(SyncItem item) {
        if (!offlineModeEnabled) {
            Log.d(TAG, "Mode offline désactivé, élément ignoré");
            return;
        }
        
        executorService.execute(() -> {
            try {
                PendingSync pendingSync = convertToPendingSync(item);
                pendingSyncDao.insert(pendingSync);
                
                Log.d(TAG, "Élément stocké pour sync offline: " + item);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du stockage offline", e);
            }
        });
    }
    
    /**
     * Récupère tous les éléments en attente de synchronisation
     */
    public void getPendingItems(PendingItemsCallback callback) {
        executorService.execute(() -> {
            try {
                List<PendingSync> pendingSyncs = pendingSyncDao.getAllPending();
                List<SyncItem> syncItems = new ArrayList<>();
                
                for (PendingSync pending : pendingSyncs) {
                    SyncItem item = convertToSyncItem(pending);
                    if (item != null) {
                        syncItems.add(item);
                    }
                }
                
                callback.onSuccess(syncItems);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la récupération des éléments pending", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Marque un élément comme synchronisé avec succès
     */
    public void markAsSynced(SyncItem item) {
        executorService.execute(() -> {
            try {
                pendingSyncDao.deleteByItemId(item.getId(), item.getType().name(), item.getAction().name());
                Log.d(TAG, "Élément marqué comme synchronisé: " + item);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la suppression de l'élément pending", e);
            }
        });
    }
    
    /**
     * Incrémente le compteur de tentatives pour un élément
     */
    public void incrementRetryCount(SyncItem item) {
        executorService.execute(() -> {
            try {
                PendingSync pending = pendingSyncDao.getByItemId(item.getId(), item.getType().name(), item.getAction().name());
                if (pending != null) {
                    pending.retryCount++;
                    pending.lastAttempt = System.currentTimeMillis();
                    
                    if (pending.retryCount >= 5) {
                        // Après 5 tentatives, marquer comme échec permanent
                        pending.status = PendingSync.Status.FAILED;
                        Log.w(TAG, "Élément marqué comme échec permanent: " + item);
                    }
                    
                    pendingSyncDao.update(pending);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'incrémentation retry count", e);
            }
        });
    }
    
    /**
     * Gère un échec de synchronisation
     */
    public void handleSyncFailure(SyncItem.Type type) {
        Log.d(TAG, "Gestion d'un échec de synchronisation pour: " + type);
        
        executorService.execute(() -> {
            try {
                // Marquer tous les éléments en cours comme en attente
                List<PendingSync> inProgress = pendingSyncDao.getByStatus(PendingSync.Status.SYNCING);
                for (PendingSync pending : inProgress) {
                    if (pending.type.equals(type.name())) {
                        pending.status = PendingSync.Status.PENDING;
                        pendingSyncDao.update(pending);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la gestion d'échec de sync", e);
            }
        });
    }
    
    /**
     * Nettoie les anciens éléments de synchronisation
     */
    public void cleanupOldItems() {
        executorService.execute(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7 jours
                int deleted = pendingSyncDao.deleteOlderThan(cutoffTime);
                
                if (deleted > 0) {
                    Log.d(TAG, "Nettoyage: " + deleted + " anciens éléments supprimés");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du nettoyage", e);
            }
        });
    }
    
    /**
     * Active/désactive le mode offline
     */
    public void setOfflineModeEnabled(boolean enabled) {
        this.offlineModeEnabled = enabled;
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply();
        
        Log.d(TAG, "Mode offline " + (enabled ? "activé" : "désactivé"));
    }
    
    /**
     * Vérifie si le mode offline est activé
     */
    public boolean isOfflineModeEnabled() {
        return offlineModeEnabled;
    }
    
    /**
     * Obtient le nombre d'éléments en attente
     */
    public void getPendingCount(CountCallback callback) {
        executorService.execute(() -> {
            try {
                int count = pendingSyncDao.getPendingCount();
                callback.onCount(count);
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du comptage pending", e);
                callback.onCount(0);
            }
        });
    }
    
    // ===== INTERFACES DE CALLBACK =====
    
    public interface PendingItemsCallback {
        void onSuccess(List<SyncItem> items);
        void onError(String error);
    }
    
    public interface CountCallback {
        void onCount(int count);
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    private void loadPreferences() {
        offlineModeEnabled = prefs.getBoolean(KEY_ENABLED, true);
    }
    
    private PendingSync convertToPendingSync(SyncItem item) {
        PendingSync pending = new PendingSync();
        pending.itemId = item.getId();
        pending.type = item.getType().name();
        pending.action = item.getAction().name();
        pending.data = serializeData(item.getData());
        pending.deviceId = item.getDeviceId();
        pending.timestamp = item.getTimestamp();
        pending.status = PendingSync.Status.PENDING;
        pending.retryCount = item.getRetryCount();
        pending.lastAttempt = 0;
        
        return pending;
    }
    
    private SyncItem convertToSyncItem(PendingSync pending) {
        try {
            SyncItem.Type type = SyncItem.Type.valueOf(pending.type);
            SyncItem.Action action = SyncItem.Action.valueOf(pending.action);
            Object data = deserializeData(pending.data, type);
            
            SyncItem item = new SyncItem(
                pending.itemId,
                type,
                action,
                pending.timestamp,
                pending.deviceId,
                data
            );
            
            // Restaurer le retry count
            for (int i = 0; i < pending.retryCount; i++) {
                item.incrementRetryCount();
            }
            
            return item;
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion PendingSync -> SyncItem", e);
            return null;
        }
    }
    
    private String serializeData(Object data) {
        // Pour simplifier, on utilise toString()
        // Dans une vraie implémentation, utiliser JSON ou autre format
        if (data == null) return "";
        return data.toString();
    }
    
    private Object deserializeData(String data, SyncItem.Type type) {
        // Pour simplifier, on retourne la string
        // Dans une vraie implémentation, désérialiser depuis JSON
        if (data == null || data.isEmpty()) return null;
        
        switch (type) {
            case RECIPE:
                // TODO: Désérialiser la recette depuis JSON
                return data;
            case MEAL_PLAN:
            case SHOPPING_LIST:
            case USER_PROFILE:
            default:
                return data;
        }
    }
}
