package fr.didictateur.inanutshell.database.dao;

import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;
import fr.didictateur.inanutshell.config.ServerConfig;

/**
 * DAO pour les configurations de serveurs Mealie
 */
@Dao
public interface ServerConfigDao {
    
    // ===== INSERTION =====
    
    @Insert
    long insertServer(ServerConfig server);
    
    @Insert
    List<Long> insertServers(List<ServerConfig> servers);
    
    // ===== MISE À JOUR =====
    
    @Update
    void updateServer(ServerConfig server);
    
    @Update
    void updateServers(List<ServerConfig> servers);
    
    @Query("UPDATE server_configs SET isDefault = 0")
    void clearDefaultServers();
    
    @Query("UPDATE server_configs SET isEnabled = :enabled WHERE id = :serverId")
    void setServerEnabled(int serverId, boolean enabled);
    
    @Query("UPDATE server_configs SET isDefault = 1 WHERE id = :serverId")
    void setServerAsDefault(int serverId);
    
    @Query("UPDATE server_configs SET priority = :priority WHERE id = :serverId")
    void setServerPriority(int serverId, int priority);
    
    @Query("UPDATE server_configs SET status = :status, lastStatusCheck = :timestamp WHERE id = :serverId")
    void updateServerStatus(int serverId, ServerConfig.ServerStatus status, long timestamp);
    
    @Query("UPDATE server_configs SET lastConnected = :timestamp WHERE id = :serverId")
    void updateLastConnected(int serverId, long timestamp);
    
    @Query("UPDATE server_configs SET version = :version WHERE id = :serverId")
    void updateServerVersion(int serverId, String version);
    
    // ===== SUPPRESSION =====
    
    @Delete
    void deleteServer(ServerConfig server);
    
    @Query("DELETE FROM server_configs WHERE id = :serverId")
    void deleteServerById(int serverId);
    
    @Query("DELETE FROM server_configs WHERE isEnabled = 0")
    void deleteDisabledServers();
    
    @Query("DELETE FROM server_configs")
    void deleteAllServers();
    
    // ===== SÉLECTION SIMPLE =====
    
    @Query("SELECT * FROM server_configs WHERE id = :serverId")
    ServerConfig getServerById(int serverId);
    
    @Query("SELECT * FROM server_configs")
    List<ServerConfig> getAllServers();
    
    @Query("SELECT * FROM server_configs")
    LiveData<List<ServerConfig>> getAllServersLiveData();
    
    @Query("SELECT * FROM server_configs WHERE isEnabled = 1")
    List<ServerConfig> getEnabledServers();
    
    @Query("SELECT * FROM server_configs WHERE isEnabled = 1")
    LiveData<List<ServerConfig>> getEnabledServersLiveData();
    
    @Query("SELECT * FROM server_configs WHERE isDefault = 1 LIMIT 1")
    ServerConfig getDefaultServer();
    
    @Query("SELECT * FROM server_configs WHERE isDefault = 1 LIMIT 1")
    LiveData<ServerConfig> getDefaultServerLiveData();
    
    // ===== SÉLECTION AVEC CRITÈRES =====
    
    @Query("SELECT * FROM server_configs WHERE isEnabled = 1 ORDER BY priority DESC, lastConnected DESC")
    List<ServerConfig> getEnabledServersByPriority();
    
    @Query("SELECT * FROM server_configs WHERE status = :status")
    List<ServerConfig> getServersByStatus(ServerConfig.ServerStatus status);
    
    @Query("SELECT * FROM server_configs WHERE status = :status")
    LiveData<List<ServerConfig>> getServersByStatusLiveData(ServerConfig.ServerStatus status);
    
    @Query("SELECT * FROM server_configs WHERE isEnabled = 1 AND status = 'ONLINE'")
    List<ServerConfig> getOnlineServers();
    
    @Query("SELECT * FROM server_configs WHERE isEnabled = 1 AND status = 'ONLINE' ORDER BY priority DESC")
    List<ServerConfig> getOnlineServersByPriority();
    
    @Query("SELECT * FROM server_configs WHERE baseUrl LIKE '%' || :domain || '%'")
    List<ServerConfig> getServersByDomain(String domain);
    
    @Query("SELECT * FROM server_configs WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%'")
    List<ServerConfig> searchServers(String searchTerm);
    
    // ===== STATISTIQUES =====
    
    @Query("SELECT COUNT(*) FROM server_configs")
    int getServerCount();
    
    @Query("SELECT COUNT(*) FROM server_configs WHERE isEnabled = 1")
    int getEnabledServerCount();
    
    @Query("SELECT COUNT(*) FROM server_configs WHERE status = 'ONLINE'")
    int getOnlineServerCount();
    
    @Query("SELECT COUNT(*) FROM server_configs WHERE lastConnected > 0")
    int getConnectedServerCount();
    
    @Query("SELECT COUNT(*) FROM server_configs WHERE syncEnabled = 1")
    int getSyncEnabledServerCount();
    
    // ===== VÉRIFICATIONS =====
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE baseUrl = :baseUrl AND id != :excludeId")
    boolean existsServerWithUrl(String baseUrl, int excludeId);
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE baseUrl = :baseUrl")
    boolean existsServerWithUrl(String baseUrl);
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE name = :name AND id != :excludeId")
    boolean existsServerWithName(String name, int excludeId);
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE name = :name")
    boolean existsServerWithName(String name);
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE isDefault = 1")
    boolean hasDefaultServer();
    
    @Query("SELECT COUNT(*) > 0 FROM server_configs WHERE isEnabled = 1")
    boolean hasEnabledServers();
    
    // ===== NETTOYAGE ET MAINTENANCE =====
    
    @Query("UPDATE server_configs SET status = 'UNKNOWN', lastStatusCheck = 0 WHERE lastStatusCheck < :cutoffTime")
    void resetOldStatusChecks(long cutoffTime);
    
    @Query("SELECT * FROM server_configs WHERE lastStatusCheck > 0 AND lastStatusCheck < :cutoffTime")
    List<ServerConfig> getServersWithOldStatus(long cutoffTime);
    
    @Query("UPDATE server_configs SET lastConnected = 0 WHERE lastConnected < :cutoffTime")
    void clearOldConnections(long cutoffTime);
    
    // ===== SYNCHRONISATION =====
    
    @Query("SELECT * FROM server_configs WHERE syncEnabled = 1 AND isEnabled = 1")
    List<ServerConfig> getSyncEnabledServers();
    
    @Query("SELECT * FROM server_configs WHERE syncEnabled = 1 AND isEnabled = 1 AND status = 'ONLINE'")
    List<ServerConfig> getAvailableSyncServers();
    
    @Query("UPDATE server_configs SET syncEnabled = :enabled WHERE id = :serverId")
    void setSyncEnabled(int serverId, boolean enabled);
    
    // ===== PRIORITÉ ET ORDRE =====
    
    @Query("SELECT MAX(priority) FROM server_configs")
    int getMaxPriority();
    
    @Query("SELECT MIN(priority) FROM server_configs")
    int getMinPriority();
    
    @Query("UPDATE server_configs SET priority = priority + 1 WHERE priority >= :priority")
    void incrementPrioritiesFrom(int priority);
    
    @Query("UPDATE server_configs SET priority = priority - 1 WHERE priority > :priority")
    void decrementPrioritiesAfter(int priority);
    
    // ===== REQUÊTES COMPLEXES =====
    
    /**
     * Obtient le meilleur serveur disponible selon les critères :
     * 1. Serveur par défaut s'il est en ligne
     * 2. Serveur en ligne avec la plus haute priorité
     * 3. Serveur activé avec la plus haute priorité
     */
    @Query("SELECT * FROM server_configs " +
           "WHERE isEnabled = 1 " +
           "ORDER BY " +
           "CASE WHEN isDefault = 1 AND status = 'ONLINE' THEN 0 ELSE 1 END, " +
           "CASE WHEN status = 'ONLINE' THEN 0 ELSE 1 END, " +
           "priority DESC, " +
           "lastConnected DESC " +
           "LIMIT 1")
    ServerConfig getBestAvailableServer();
    
    /**
     * Obtient les serveurs de secours (excluant le serveur actuel)
     */
    @Query("SELECT * FROM server_configs " +
           "WHERE isEnabled = 1 AND id != :currentServerId " +
           "ORDER BY " +
           "CASE WHEN status = 'ONLINE' THEN 0 ELSE 1 END, " +
           "priority DESC, " +
           "lastConnected DESC")
    List<ServerConfig> getFallbackServers(int currentServerId);
    
    /**
     * Obtient les statistiques des serveurs par statut
     */
    @Query("SELECT status, COUNT(*) as count " +
           "FROM server_configs " +
           "WHERE isEnabled = 1 " +
           "GROUP BY status")
    List<ServerStatusCount> getServerStatusCounts();
    
    /**
     * Obtient les serveurs récemment connectés
     */
    @Query("SELECT * FROM server_configs " +
           "WHERE lastConnected > :sinceTimestamp " +
           "ORDER BY lastConnected DESC")
    List<ServerConfig> getRecentlyConnectedServers(long sinceTimestamp);
    
    /**
     * Obtient les serveurs nécessitant une vérification de statut
     */
    @Query("SELECT * FROM server_configs " +
           "WHERE isEnabled = 1 " +
           "AND (lastStatusCheck = 0 OR lastStatusCheck < :cutoffTime) " +
           "ORDER BY priority DESC")
    List<ServerConfig> getServersNeedingStatusCheck(long cutoffTime);
    
    // ===== UTILITAIRES =====
    
    /**
     * Classe pour les statistiques de statut
     */
    class ServerStatusCount {
        public ServerConfig.ServerStatus status;
        public int count;
    }
    
    /**
     * Réinitialise la priorité de tous les serveurs de manière séquentielle
     */
    @Query("SELECT id FROM server_configs ORDER BY priority DESC, name ASC")
    List<Integer> getServerIdsOrderedByPriority();
    
    /**
     * Met à jour la priorité d'un serveur spécifique
     */
    @Query("UPDATE server_configs SET priority = :newPriority WHERE id = :serverId")
    void updateServerPriority(int serverId, int newPriority);
    
    /**
     * Obtient la prochaine priorité disponible
     */
    default int getNextPriority() {
        int maxPriority = getMaxPriority();
        return maxPriority + 1;
    }
    
    /**
     * Réorganise les priorités pour qu'elles soient séquentielles
     */
    default void normalizePriorities() {
        List<Integer> serverIds = getServerIdsOrderedByPriority();
        for (int i = 0; i < serverIds.size(); i++) {
            updateServerPriority(serverIds.get(i), serverIds.size() - i);
        }
    }
}
