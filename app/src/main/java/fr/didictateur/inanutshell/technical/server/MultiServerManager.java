package fr.didictateur.inanutshell.technical.server;

import android.content.Context;
import android.content.SharedPreferences;
import fr.didictateur.inanutshell.technical.logging.AppLogger;
import fr.didictateur.inanutshell.technical.network.NetworkStateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiServerManager {
    private static final String TAG = "MultiServerManager";
    private static final String PREFS_NAME = "server_config";
    private static final String KEY_CURRENT_SERVER = "current_server_url";
    
    private static MultiServerManager instance;
    private Context context;
    private List<ServerConfig> servers;
    private ServerConfig currentServer;
    private AppLogger logger;
    private NetworkStateManager networkManager;
    private ExecutorService executorService;
    private SharedPreferences preferences;
    
    public interface ServerTestCallback {
        void onServerTestComplete(ServerConfig server, boolean success, long responseTime);
        void onAllServersTestComplete(List<ServerConfig> availableServers);
    }
    
    public interface FailoverCallback {
        void onFailoverComplete(ServerConfig newServer);
        void onFailoverFailed(String error);
    }
    
    private MultiServerManager(Context context) {
        this.context = context.getApplicationContext();
        this.servers = new ArrayList<>();
        this.logger = AppLogger.getInstance(context);
        this.networkManager = NetworkStateManager.getInstance(context);
        this.executorService = Executors.newCachedThreadPool();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        initializeDefaultServers();
        loadCurrentServer();
    }
    
    public static MultiServerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MultiServerManager.class) {
                if (instance == null) {
                    instance = new MultiServerManager(context);
                }
            }
        }
        return instance;
    }
    
    private void initializeDefaultServers() {
        // Add default servers - these would be configured based on your backend infrastructure
        servers.add(new ServerConfig("Primary Server", "api.inanutshell.com", 1));
        servers.add(new ServerConfig("Secondary Server", "api2.inanutshell.com", 2));
        servers.add(new ServerConfig("Backup Server", "backup-api.inanutshell.com", 3));
    }
    
    private void loadCurrentServer() {
        String currentServerUrl = preferences.getString(KEY_CURRENT_SERVER, null);
        if (currentServerUrl != null) {
            for (ServerConfig server : servers) {
                if (currentServerUrl.equals(server.getServerUrl())) {
                    currentServer = server;
                    break;
                }
            }
        }
        
        // If no current server or server not found, use the highest priority available server
        if (currentServer == null && !servers.isEmpty()) {
            currentServer = getHighestPriorityServer();
        }
    }
    
    private void saveCurrentServer() {
        if (currentServer != null) {
            preferences.edit()
                .putString(KEY_CURRENT_SERVER, currentServer.getServerUrl())
                .apply();
        }
    }
    
    public void addServer(ServerConfig server) {
        if (server != null && !servers.contains(server)) {
            servers.add(server);
            sortServersByPriority();
        }
    }
    
    public void removeServer(ServerConfig server) {
        servers.remove(server);
        if (currentServer != null && currentServer.equals(server)) {
            currentServer = getHighestPriorityServer();
            saveCurrentServer();
        }
    }
    
    public List<ServerConfig> getServers() {
        return new ArrayList<>(servers);
    }
    
    public List<ServerConfig> getActiveServers() {
        List<ServerConfig> activeServers = new ArrayList<>();
        for (ServerConfig server : servers) {
            if (server.isActive()) {
                activeServers.add(server);
            }
        }
        return activeServers;
    }
    
    public ServerConfig getCurrentServer() {
        return currentServer;
    }
    
    public void setCurrentServer(ServerConfig server) {
        if (server != null && servers.contains(server)) {
            this.currentServer = server;
            saveCurrentServer();
            logger.logInfo(TAG, "Current server changed to: " + server.getName());
        }
    }
    
    private ServerConfig getHighestPriorityServer() {
        List<ServerConfig> activeServers = getActiveServers();
        if (activeServers.isEmpty()) return null;
        
        sortServersByPriority();
        return activeServers.get(0);
    }
    
    private void sortServersByPriority() {
        Collections.sort(servers, new Comparator<ServerConfig>() {
            @Override
            public int compare(ServerConfig s1, ServerConfig s2) {
                return Integer.compare(s1.getPriority(), s2.getPriority());
            }
        });
    }
    
    public void testAllServers(ServerTestCallback callback) {
        if (!networkManager.isConnected()) {
            logger.logWarning(TAG, "No network connection available for server testing");
            if (callback != null) {
                callback.onAllServersTestComplete(new ArrayList<ServerConfig>());
            }
            return;
        }
        
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<ServerConfig> availableServers = new ArrayList<>();
                
                for (ServerConfig server : servers) {
                    if (!server.isActive()) continue;
                    
                    long startTime = System.currentTimeMillis();
                    boolean success = testServerConnection(server);
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    server.setLastResponseTime(responseTime);
                    
                    if (success) {
                        availableServers.add(server);
                    }
                    
                    if (callback != null) {
                        callback.onServerTestComplete(server, success, responseTime);
                    }
                    
                    logger.logInfo(TAG, String.format("Server %s test: %s (%.2fs)", 
                        server.getName(), success ? "SUCCESS" : "FAILED", responseTime / 1000.0));
                }
                
                if (callback != null) {
                    callback.onAllServersTestComplete(availableServers);
                }
            }
        });
    }
    
    public void performFailover(FailoverCallback callback) {
        logger.logInfo(TAG, "Performing server failover...");
        
        testAllServers(new ServerTestCallback() {
            @Override
            public void onServerTestComplete(ServerConfig server, boolean success, long responseTime) {
                // Individual server test complete - no action needed here
            }
            
            @Override
            public void onAllServersTestComplete(List<ServerConfig> availableServers) {
                if (availableServers.isEmpty()) {
                    logger.logError(TAG, "Failover failed: No available servers");
                    if (callback != null) {
                        callback.onFailoverFailed("No available servers");
                    }
                    return;
                }
                
                // Find the best available server (lowest response time among highest priority)
                ServerConfig bestServer = findBestServer(availableServers);
                
                if (bestServer != null && !bestServer.equals(currentServer)) {
                    setCurrentServer(bestServer);
                    logger.logInfo(TAG, "Failover complete: Switched to " + bestServer.getName());
                    
                    if (callback != null) {
                        callback.onFailoverComplete(bestServer);
                    }
                } else {
                    logger.logInfo(TAG, "Failover not needed: Current server is optimal");
                    if (callback != null) {
                        callback.onFailoverComplete(currentServer);
                    }
                }
            }
        });
    }
    
    private ServerConfig findBestServer(List<ServerConfig> availableServers) {
        if (availableServers.isEmpty()) return null;
        
        // Sort by priority first, then by response time
        Collections.sort(availableServers, new Comparator<ServerConfig>() {
            @Override
            public int compare(ServerConfig s1, ServerConfig s2) {
                int priorityComparison = Integer.compare(s1.getPriority(), s2.getPriority());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return Long.compare(s1.getLastResponseTime(), s2.getLastResponseTime());
            }
        });
        
        return availableServers.get(0);
    }
    
    private boolean testServerConnection(ServerConfig server) {
        try {
            // This is a placeholder for actual server connectivity test
            // In a real implementation, you would make an HTTP request to test the server
            
            // Simulate network delay
            Thread.sleep(100 + (long)(Math.random() * 900)); // 100-1000ms random delay
            
            // Simulate success/failure (90% success rate for simulation)
            return Math.random() > 0.1;
            
        } catch (Exception e) {
            logger.logError(TAG, "Error testing server " + server.getName(), e);
            return false;
        }
    }
    
    public boolean isCurrentServerHealthy() {
        return currentServer != null && currentServer.isActive() && currentServer.isHealthy();
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Configuration methods
    public void enableServer(String serverUrl) {
        for (ServerConfig server : servers) {
            if (serverUrl.equals(server.getServerUrl())) {
                server.setActive(true);
                break;
            }
        }
    }
    
    public void disableServer(String serverUrl) {
        for (ServerConfig server : servers) {
            if (serverUrl.equals(server.getServerUrl())) {
                server.setActive(false);
                if (currentServer != null && currentServer.equals(server)) {
                    // Need to switch to another server
                    currentServer = getHighestPriorityServer();
                    saveCurrentServer();
                }
                break;
            }
        }
    }
    
    public int getActiveServerCount() {
        return getActiveServers().size();
    }
    
    public long getAverageResponseTime() {
        List<ServerConfig> activeServers = getActiveServers();
        if (activeServers.isEmpty()) return 0;
        
        long totalTime = 0;
        int count = 0;
        for (ServerConfig server : activeServers) {
            if (server.getLastResponseTime() > 0) {
                totalTime += server.getLastResponseTime();
                count++;
            }
        }
        
        return count > 0 ? totalTime / count : 0;
    }
}
