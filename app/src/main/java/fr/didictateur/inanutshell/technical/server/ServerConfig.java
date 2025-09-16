package fr.didictateur.inanutshell.technical.server;

public class ServerConfig {
    private String serverUrl;
    private String name;
    private int priority;
    private boolean active;
    private long lastResponseTime;
    private boolean ssl;
    private int port;
    private String apiKey;
    
    public ServerConfig() {
        this.active = true;
        this.ssl = true;
        this.port = 443;
        this.priority = 0;
    }
    
    public ServerConfig(String name, String serverUrl) {
        this();
        this.name = name;
        this.serverUrl = serverUrl;
    }
    
    public ServerConfig(String name, String serverUrl, int priority) {
        this(name, serverUrl);
        this.priority = priority;
    }
    
    // Getters and Setters
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public long getLastResponseTime() { return lastResponseTime; }
    public void setLastResponseTime(long lastResponseTime) { this.lastResponseTime = lastResponseTime; }
    
    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    // Utility methods
    public String getFullUrl() {
        if (serverUrl == null) return null;
        
        if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
            return serverUrl;
        }
        
        String protocol = ssl ? "https://" : "http://";
        return protocol + serverUrl + (port != (ssl ? 443 : 80) ? ":" + port : "");
    }
    
    public boolean isHealthy() {
        return active && lastResponseTime < 5000; // Less than 5 seconds response time
    }
    
    @Override
    public String toString() {
        return name != null ? name : serverUrl;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ServerConfig that = (ServerConfig) obj;
        return serverUrl != null ? serverUrl.equals(that.serverUrl) : that.serverUrl == null;
    }
    
    @Override
    public int hashCode() {
        return serverUrl != null ? serverUrl.hashCode() : 0;
    }
}
