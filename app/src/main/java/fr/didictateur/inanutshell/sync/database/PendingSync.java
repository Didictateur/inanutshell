package fr.didictateur.inanutshell.sync.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entité représentant un élément en attente de synchronisation
 */
@Entity(tableName = "pending_sync")
public class PendingSync {
    
    public enum Status {
        PENDING,    // En attente
        SYNCING,    // En cours de synchronisation
        FAILED      // Échec permanent
    }
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String itemId;           // ID de l'élément à synchroniser
    public String type;             // Type d'élément (RECIPE, MEAL_PLAN, etc.)
    public String action;           // Action à effectuer (CREATE, UPDATE, DELETE)
    public String data;             // Données sérialisées de l'élément
    public String deviceId;         // ID de l'appareil source
    public long timestamp;          // Timestamp de création
    public Status status;           // Statut actuel
    public int retryCount;          // Nombre de tentatives
    public long lastAttempt;        // Timestamp de la dernière tentative
    
    public PendingSync() {
        this.status = Status.PENDING;
        this.retryCount = 0;
        this.lastAttempt = 0;
    }
}
