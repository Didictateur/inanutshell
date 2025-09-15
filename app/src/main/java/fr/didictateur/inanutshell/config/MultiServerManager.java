package fr.didictateur.inanutshell.config;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import fr.didictateur.inanutshell.AppDatabase;
import fr.didictateur.inanutshell.database.dao.ServerConfigDao;
import fr.didictateur.inanutshell.network.NetworkStateManager;
import fr.didictateur.inanutshell.performance.PerformanceManager;
import fr.didictateur.inanutshell.logging.AppLogger;

/**
 * Gestionnaire pour la configuration et le basculement automatique entre serveurs Mealie
 */
public class MultiServerManager {
    private static final String TAG = "MultiServerManager";
    private static MultiServerManager instance;
    
    private final Context context;
    private final AppDatabase database;
    private final ServerConfigDao serverDao;
    private final ExecutorService executor;
    private final OkHttpClient httpClient;
    private final PerformanceManager performanceManager;
    private final AppLogger logger;
    private final NetworkStateManager networkManager;
    
    // État actuel
    private final AtomicReference<ServerConfig> currentServer;
    private final MutableLiveData<ServerConfig> currentServerLiveData;
    private final MutableLiveData<List<ServerConfig>> serversLiveData;
    private final MutableLiveData<ConnectionStatus> connectionStatusLiveData;
    
    // Cache et surveillance
    private final ConcurrentHashMap<Integer, Future<?>> statusCheckTasks;
    private final ConcurrentHashMap<Integer, Long> lastConnectionAttempt;
    
    // Configuration
    private static final int HEALTH_CHECK_INTERVAL_MS = 30000; // 30 secondes
    private static final int CONNECTION_TIMEOUT_MS = 10000;    // 10 secondes
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int FALLBACK_DELAY_MS = 5000;         // 5 secondes
    
    /**
     * État de la connexion multi-serveurs
     */
    public enum ConnectionStatus {
        DISCONNECTED,     // Aucune connexion
        CONNECTING,       // Connexion en cours
        CONNECTED,        // Connecté au serveur principal
        FALLBACK,         // Connecté à un serveur de secours
        SWITCHING,        // Basculement en cours
        ERROR             // Erreur de connexion
    }
    
    private MultiServerManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.serverDao = database.serverConfigDao();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "MultiServer");
            thread.setDaemon(true);
            return thread;
        });
        
        // Client HTTP avec timeout personnalisé
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(CONNECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(CONNECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();
        
        this.performanceManager = PerformanceManager.getInstance();
        this.logger = AppLogger.getInstance(context);
        this.networkManager = NetworkStateManager.getInstance(context);
        
        // État
        this.currentServer = new AtomicReference<>();
        this.currentServerLiveData = new MutableLiveData<>();
        this.serversLiveData = new MutableLiveData<>();
        this.connectionStatusLiveData = new MutableLiveData<>(ConnectionStatus.DISCONNECTED);
        
        // Cache
        this.statusCheckTasks = new ConcurrentHashMap<>();
        this.lastConnectionAttempt = new ConcurrentHashMap<>();
        
        // Initialisation
        initializeServers();
        startHealthCheckScheduler();
        observeNetworkChanges();
    }
    
    public static synchronized MultiServerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MultiServerManager(context);
        }
        return instance;
    }
    
    /**
     * Initialise les serveurs depuis la base de données
     */
    private void initializeServers() {
        executor.submit(() -> {
            try {
                List<ServerConfig> servers = serverDao.getAllServers();
                serversLiveData.postValue(servers);
                
                // Chercher le serveur par défaut
                ServerConfig defaultServer = null;
                for (ServerConfig server : servers) {
                    if (server.isDefault() && server.isEnabled()) {
                        defaultServer = server;
                        break;
                    }
                }
                
                // Si pas de serveur par défaut, prendre le premier activé
                if (defaultServer == null) {
                    for (ServerConfig server : servers) {
                        if (server.isEnabled()) {
                            defaultServer = server;
                            break;
                        }
                    }
                }
                
                if (defaultServer != null) {
                    setCurrentServer(defaultServer);
                    logger.logInfo(TAG, "Default server initialized: " + defaultServer.getName());
                } else {
                    logger.logWarning(TAG, "No enabled servers found");
                }
                
            } catch (Exception e) {
                logger.logError(TAG, "Error initializing servers", e);
            }
        });
    }
    
    /**
     * Observe les changements de réseau pour reconnecter si nécessaire
     */
    private void observeNetworkChanges() {
        networkManager.getNetworkStateLiveData().observeForever(networkState -> {
            if (networkState.isConnected) {
                logger.logInfo(TAG, "Network connected - checking server status");
                checkCurrentServerStatus();
            } else {
                logger.logInfo(TAG, "Network disconnected");
                connectionStatusLiveData.postValue(ConnectionStatus.DISCONNECTED);
            }
        });
    }
    
    /**
     * Ajoute un nouveau serveur
     */
    public void addServer(ServerConfig server, ServerCallback callback) {
        performanceManager.executeWithCache(
            "add_server_" + server.getName(),
            new PerformanceManager.PerformanceTask<ServerConfig>() {
                @Override
                public ServerConfig execute() throws Exception {
                    // Valider la configuration
                    ServerConfig.ValidationResult validation = server.validate();
                    if (!validation.isValid()) {
                        throw new IllegalArgumentException(validation.getErrorMessage());
                    }
                    
                    // Vérifier si c'est le premier serveur (le rendre par défaut)
                    List<ServerConfig> existingServers = serverDao.getAllServers();
                    if (existingServers.isEmpty()) {
                        server.setDefault(true);
                    }
                    
                    // Sauvegarder
                    long id = serverDao.insertServer(server);
                    server.setId((int) id);
                    
                    // Tester la connexion
                    testServerConnection(server);
                    
                    // Mettre à jour la liste
                    refreshServersList();
                    
                    logger.logInfo(TAG, "Server added: " + server.getName());
                    return server;
                }
                
                @Override
                public PerformanceManager.TaskType getType() {
                    return PerformanceManager.TaskType.IO;
                }
                
                @Override
                public boolean isCacheable() {
                    return false;
                }
            },
            new PerformanceManager.PerformanceCallback<ServerConfig>() {
                @Override
                public void onSuccess(ServerConfig result) {
                    callback.onSuccess(result);
                }
                
                @Override
                public void onError(Exception error) {
                    callback.onError(error);
                }
            }
        );
    }
    
    /**
     * Met à jour un serveur existant
     */
    public void updateServer(ServerConfig server, ServerCallback callback) {
        executor.submit(() -> {
            try {
                // Valider la configuration
                ServerConfig.ValidationResult validation = server.validate();
                if (!validation.isValid()) {
                    throw new IllegalArgumentException(validation.getErrorMessage());
                }
                
                // Mettre à jour en base
                serverDao.updateServer(server);
                
                // Tester la connexion
                testServerConnection(server);
                
                // Si c'est le serveur actuel, le recharger
                ServerConfig current = currentServer.get();
                if (current != null && current.getId() == server.getId()) {
                    setCurrentServer(server);
                }
                
                // Mettre à jour la liste
                refreshServersList();
                
                logger.logInfo(TAG, "Server updated: " + server.getName());
                callback.onSuccess(server);
                
            } catch (Exception e) {
                logger.logError(TAG, "Error updating server", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Supprime un serveur
     */
    public void deleteServer(int serverId, SimpleCallback callback) {
        executor.submit(() -> {
            try {
                ServerConfig serverToDelete = serverDao.getServerById(serverId);
                if (serverToDelete == null) {
                    throw new IllegalArgumentException("Serveur non trouvé");
                }
                
                // Ne pas permettre la suppression s'il n'y a qu'un serveur
                List<ServerConfig> allServers = serverDao.getAllServers();
                if (allServers.size() <= 1) {
                    throw new IllegalStateException("Impossible de supprimer le dernier serveur");
                }
                
                // Si c'est le serveur par défaut, en choisir un autre
                if (serverToDelete.isDefault()) {
                    for (ServerConfig server : allServers) {
                        if (server.getId() != serverId && server.isEnabled()) {
                            server.setDefault(true);
                            serverDao.updateServer(server);
                            break;
                        }
                    }
                }
                
                // Si c'est le serveur actuel, basculer vers un autre
                ServerConfig current = currentServer.get();
                if (current != null && current.getId() == serverId) {
                    switchToNextAvailableServer();
                }
                
                // Supprimer
                serverDao.deleteServer(serverToDelete);
                
                // Mettre à jour la liste
                refreshServersList();
                
                logger.logInfo(TAG, "Server deleted: " + serverToDelete.getName());
                callback.onSuccess();
                
            } catch (Exception e) {
                logger.logError(TAG, "Error deleting server", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Définit le serveur par défaut
     */
    public void setDefaultServer(int serverId, SimpleCallback callback) {
        executor.submit(() -> {
            try {
                // Retirer le flag par défaut des autres serveurs
                serverDao.clearDefaultServers();
                
                // Définir le nouveau serveur par défaut
                ServerConfig server = serverDao.getServerById(serverId);
                if (server != null) {
                    server.setDefault(true);
                    serverDao.updateServer(server);
                    
                    // Basculer vers ce serveur
                    switchToServer(server);
                    
                    logger.logInfo(TAG, "Default server set to: " + server.getName());
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalArgumentException("Serveur non trouvé"));
                }
                
            } catch (Exception e) {
                logger.logError(TAG, "Error setting default server", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Bascule vers un serveur spécifique
     */
    public void switchToServer(ServerConfig server) {
        if (server == null || !server.isEnabled()) {
            logger.logWarning(TAG, "Cannot switch to disabled or null server");
            return;
        }
        
        connectionStatusLiveData.postValue(ConnectionStatus.SWITCHING);
        logger.logInfo(TAG, "Switching to server: " + server.getName());
        
        executor.submit(() -> {
            try {
                // Tester la connexion au nouveau serveur
                boolean connected = testServerConnection(server);
                
                if (connected) {
                    setCurrentServer(server);
                    connectionStatusLiveData.postValue(ConnectionStatus.CONNECTED);
                    logger.logInfo(TAG, "Successfully switched to: " + server.getName());
                } else {
                    connectionStatusLiveData.postValue(ConnectionStatus.ERROR);
                    logger.logError(TAG, "Failed to connect to server: " + server.getName());
                }
                
            } catch (Exception e) {
                connectionStatusLiveData.postValue(ConnectionStatus.ERROR);
                logger.logError(TAG, "Error switching server", e);
            }
        });
    }
    
    /**
     * Bascule automatiquement vers le prochain serveur disponible
     */
    public void switchToNextAvailableServer() {
        executor.submit(() -> {
            try {
                List<ServerConfig> servers = serverDao.getEnabledServersByPriority();
                
                for (ServerConfig server : servers) {
                    ServerConfig current = currentServer.get();
                    
                    // Ignorer le serveur actuel s'il y en a un
                    if (current != null && server.getId() == current.getId()) {
                        continue;
                    }
                    
                    // Tester la connexion
                    if (testServerConnection(server)) {
                        setCurrentServer(server);
                        connectionStatusLiveData.postValue(ConnectionStatus.FALLBACK);
                        logger.logInfo(TAG, "Switched to fallback server: " + server.getName());
                        return;
                    }
                }
                
                // Aucun serveur disponible
                setCurrentServer(null);
                connectionStatusLiveData.postValue(ConnectionStatus.DISCONNECTED);
                logger.logWarning(TAG, "No available servers found");
                
            } catch (Exception e) {
                logger.logError(TAG, "Error switching to next server", e);
                connectionStatusLiveData.postValue(ConnectionStatus.ERROR);
            }
        });
    }
    
    /**
     * Teste la connexion à un serveur
     */
    private boolean testServerConnection(ServerConfig server) {
        if (server == null) return false;
        
        long now = System.currentTimeMillis();
        Long lastAttempt = lastConnectionAttempt.get(server.getId());
        
        // Éviter les tentatives trop fréquentes
        if (lastAttempt != null && now - lastAttempt < FALLBACK_DELAY_MS) {
            return false;
        }
        
        lastConnectionAttempt.put(server.getId(), now);
        
        try {
            String healthUrl = server.getApiUrl() + "/app/about";
            Request request = new Request.Builder()
                .url(healthUrl)
                .header("Authorization", "Bearer " + server.getApiKey())
                .build();
            
            logger.logDebug(TAG, "Testing connection to: " + server.getName());
            
            try (Response response = httpClient.newCall(request).execute()) {
                boolean isHealthy = response.isSuccessful();
                
                if (isHealthy) {
                    server.updateStatus(ServerConfig.ServerStatus.ONLINE);
                    
                    // Extraire la version si possible
                    if (response.body() != null) {
                        String body = response.body().string();
                        // Analyser la réponse pour extraire la version
                        extractVersionFromResponse(server, body);
                    }
                } else {
                    if (response.code() == 401 || response.code() == 403) {
                        server.updateStatus(ServerConfig.ServerStatus.UNAUTHORIZED);
                    } else {
                        server.updateStatus(ServerConfig.ServerStatus.ERROR);
                    }
                }
                
                // Mettre à jour en base
                serverDao.updateServer(server);
                
                logger.logDebug(TAG, "Server " + server.getName() + " status: " + server.getStatus() + 
                    " (HTTP " + response.code() + ")");
                
                return isHealthy;
            }
            
        } catch (IOException e) {
            server.updateStatus(ServerConfig.ServerStatus.OFFLINE);
            serverDao.updateServer(server);
            logger.logDebug(TAG, "Server " + server.getName() + " is offline: " + e.getMessage());
            return false;
        } catch (Exception e) {
            server.updateStatus(ServerConfig.ServerStatus.ERROR);
            serverDao.updateServer(server);
            logger.logError(TAG, "Error testing server " + server.getName(), e);
            return false;
        }
    }
    
    /**
     * Extrait la version de Mealie depuis la réponse
     */
    private void extractVersionFromResponse(ServerConfig server, String response) {
        try {
            // Parse JSON pour extraire la version
            if (response.contains("\"version\"")) {
                String version = response.replaceAll(".*\"version\":\\s*\"([^\"]+)\".*", "$1");
                if (!version.equals(response)) { // Si la regex a fonctionné
                    server.setVersion(version);
                    logger.logDebug(TAG, "Detected Mealie version: " + version + " on " + server.getName());
                }
            }
        } catch (Exception e) {
            logger.logDebug(TAG, "Could not extract version from response: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie le statut du serveur actuel
     */
    private void checkCurrentServerStatus() {
        ServerConfig current = currentServer.get();
        if (current == null) {
            switchToNextAvailableServer();
            return;
        }
        
        executor.submit(() -> {
            boolean isHealthy = testServerConnection(current);
            
            if (!isHealthy) {
                logger.logWarning(TAG, "Current server unhealthy, switching to fallback");
                switchToNextAvailableServer();
            } else {
                // Serveur OK, mais vérifier s'il y a un meilleur serveur disponible
                checkForBetterServer();
            }
        });
    }
    
    /**
     * Vérifie s'il y a un serveur de priorité plus élevée disponible
     */
    private void checkForBetterServer() {
        ServerConfig current = currentServer.get();
        if (current == null) return;
        
        executor.submit(() -> {
            try {
                List<ServerConfig> servers = serverDao.getEnabledServersByPriority();
                
                for (ServerConfig server : servers) {
                    // Ignorer le serveur actuel et les serveurs de priorité inférieure
                    if (server.getId() == current.getId() || 
                        server.getPriority() <= current.getPriority()) {
                        continue;
                    }
                    
                    // Tester le serveur de priorité plus élevée
                    if (testServerConnection(server)) {
                        logger.logInfo(TAG, "Found better server, switching from " + 
                            current.getName() + " to " + server.getName());
                        setCurrentServer(server);
                        connectionStatusLiveData.postValue(ConnectionStatus.CONNECTED);
                        break;
                    }
                }
                
            } catch (Exception e) {
                logger.logError(TAG, "Error checking for better server", e);
            }
        });
    }
    
    /**
     * Démarre le planificateur de vérifications périodiques
     */
    private void startHealthCheckScheduler() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEALTH_CHECK_INTERVAL_MS);
                    
                    if (networkManager.getCurrentNetworkState().isConnected) {
                        checkCurrentServerStatus();
                        
                        // Vérifier périodiquement tous les serveurs
                        refreshServersStatus();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.logError(TAG, "Error in health check scheduler", e);
                }
            }
        });
    }
    
    /**
     * Rafraîchit le statut de tous les serveurs
     */
    private void refreshServersStatus() {
        executor.submit(() -> {
            try {
                List<ServerConfig> servers = serverDao.getAllServers();
                
                for (ServerConfig server : servers) {
                    if (server.isEnabled() && !server.isStatusFresh()) {
                        // Annuler la tâche précédente s'il y en a une
                        Future<?> existingTask = statusCheckTasks.get(server.getId());
                        if (existingTask != null && !existingTask.isDone()) {
                            existingTask.cancel(true);
                        }
                        
                        // Démarrer une nouvelle vérification
                        Future<?> task = executor.submit(() -> testServerConnection(server));
                        statusCheckTasks.put(server.getId(), task);
                    }
                }
                
                // Mettre à jour la liste après les vérifications
                performanceManager.executeOnMainThreadDelayed(this::refreshServersList, 2000);
                
            } catch (Exception e) {
                logger.logError(TAG, "Error refreshing servers status", e);
            }
        });
    }
    
    /**
     * Rafraîchit la liste des serveurs
     */
    private void refreshServersList() {
        executor.submit(() -> {
            try {
                List<ServerConfig> servers = serverDao.getAllServers();
                serversLiveData.postValue(servers);
            } catch (Exception e) {
                logger.logError(TAG, "Error refreshing servers list", e);
            }
        });
    }
    
    /**
     * Définit le serveur actuel
     */
    private void setCurrentServer(ServerConfig server) {
        currentServer.set(server);
        currentServerLiveData.postValue(server);
        
        if (server != null) {
            server.setLastConnected(System.currentTimeMillis());
            executor.submit(() -> serverDao.updateServer(server));
        }
    }
    
    // Getters pour LiveData
    public LiveData<ServerConfig> getCurrentServerLiveData() {
        return currentServerLiveData;
    }
    
    public LiveData<List<ServerConfig>> getServersLiveData() {
        return serversLiveData;
    }
    
    public LiveData<ConnectionStatus> getConnectionStatusLiveData() {
        return connectionStatusLiveData;
    }
    
    public ServerConfig getCurrentServer() {
        return currentServer.get();
    }
    
    /**
     * Obtient les statistiques de connexion
     */
    public ConnectionStats getConnectionStats() {
        try {
            List<ServerConfig> servers = serverDao.getAllServers();
            
            int totalServers = servers.size();
            int onlineServers = 0;
            int enabledServers = 0;
            long totalConnections = 0;
            
            for (ServerConfig server : servers) {
                if (server.isEnabled()) {
                    enabledServers++;
                }
                if (server.getStatus() == ServerConfig.ServerStatus.ONLINE) {
                    onlineServers++;
                }
                if (server.getLastConnected() > 0) {
                    totalConnections++;
                }
            }
            
            return new ConnectionStats(
                totalServers,
                enabledServers,
                onlineServers,
                totalConnections,
                currentServer.get() != null
            );
            
        } catch (Exception e) {
            logger.logError(TAG, "Error getting connection stats", e);
            return new ConnectionStats(0, 0, 0, 0, false);
        }
    }
    
    /**
     * Force la reconnexion
     */
    public void forceReconnect() {
        logger.logInfo(TAG, "Force reconnecting...");
        connectionStatusLiveData.postValue(ConnectionStatus.CONNECTING);
        
        // Vider le cache des tentatives de connexion
        lastConnectionAttempt.clear();
        
        // Reconnecter
        checkCurrentServerStatus();
    }
    
    /**
     * Libère les ressources
     */
    public void shutdown() {
        // Annuler toutes les tâches en cours
        for (Future<?> task : statusCheckTasks.values()) {
            task.cancel(true);
        }
        statusCheckTasks.clear();
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.logInfo(TAG, "MultiServerManager shutdown completed");
    }
    
    // Interfaces de callback
    public interface ServerCallback {
        void onSuccess(ServerConfig server);
        void onError(Exception error);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception error);
    }
    
    /**
     * Statistiques de connexion
     */
    public static class ConnectionStats {
        public final int totalServers;
        public final int enabledServers;
        public final int onlineServers;
        public final long totalConnections;
        public final boolean hasActiveConnection;
        
        public ConnectionStats(int totalServers, int enabledServers, int onlineServers, 
                             long totalConnections, boolean hasActiveConnection) {
            this.totalServers = totalServers;
            this.enabledServers = enabledServers;
            this.onlineServers = onlineServers;
            this.totalConnections = totalConnections;
            this.hasActiveConnection = hasActiveConnection;
        }
        
        @Override
        public String toString() {
            return String.format("ConnectionStats{total=%d, enabled=%d, online=%d, connected=%s}",
                totalServers, enabledServers, onlineServers, hasActiveConnection);
        }
    }
}
