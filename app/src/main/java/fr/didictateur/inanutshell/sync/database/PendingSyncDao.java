package fr.didictateur.inanutshell.sync.database;

import androidx.room.*;
import java.util.List;

/**
 * DAO pour accéder aux éléments de synchronisation en attente
 */
@Dao
public interface PendingSyncDao {
    
    @Insert
    void insert(PendingSync pendingSync);
    
    @Update
    void update(PendingSync pendingSync);
    
    @Delete
    void delete(PendingSync pendingSync);
    
    @Query("SELECT * FROM pending_sync WHERE status = :status ORDER BY timestamp ASC")
    List<PendingSync> getByStatus(PendingSync.Status status);
    
    @Query("SELECT * FROM pending_sync WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<PendingSync> getAllPending();
    
    @Query("SELECT * FROM pending_sync WHERE itemId = :itemId AND type = :type AND action = :action")
    PendingSync getByItemId(String itemId, String type, String action);
    
    @Query("DELETE FROM pending_sync WHERE itemId = :itemId AND type = :type AND action = :action")
    void deleteByItemId(String itemId, String type, String action);
    
    @Query("DELETE FROM pending_sync WHERE timestamp < :cutoffTime")
    int deleteOlderThan(long cutoffTime);
    
    @Query("SELECT COUNT(*) FROM pending_sync WHERE status = 'PENDING'")
    int getPendingCount();
    
    @Query("SELECT COUNT(*) FROM pending_sync WHERE status = 'FAILED'")
    int getFailedCount();
    
    @Query("DELETE FROM pending_sync")
    void deleteAll();
    
    @Query("SELECT * FROM pending_sync WHERE type = :type AND status = 'PENDING' ORDER BY timestamp ASC")
    List<PendingSync> getPendingByType(String type);
    
    @Query("UPDATE pending_sync SET retryCount = retryCount + 1, lastAttempt = :timestamp WHERE id = :id")
    void incrementRetryCount(long id, long timestamp);
}
