package fr.didictateur.inanutshell.sync.model;

/**
 * Représente un élément dans la file d'attente de synchronisation
 */
public class SyncItem {
    
    public enum Type {
        RECIPE,
        MEAL_PLAN,
        SHOPPING_LIST,
        USER_PROFILE
    }
    
    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
    
    private final String id;
    private final Type type;
    private final Action action;
    private final long timestamp;
    private final String deviceId;
    private final Object data;
    private int retryCount;
    
    public SyncItem(String id, Type type, Action action, long timestamp, String deviceId, Object data) {
        this.id = id;
        this.type = type;
        this.action = action;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.data = data;
        this.retryCount = 0;
    }
    
    // Getters
    public String getId() { return id; }
    public Type getType() { return type; }
    public Action getAction() { return action; }
    public long getTimestamp() { return timestamp; }
    public String getDeviceId() { return deviceId; }
    public Object getData() { return data; }
    public int getRetryCount() { return retryCount; }
    
    // Méthodes
    public void incrementRetryCount() {
        retryCount++;
    }
    
    public boolean shouldRetry() {
        return retryCount < 3; // Maximum 3 tentatives
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SyncItem syncItem = (SyncItem) obj;
        return id.equals(syncItem.id) && 
               type == syncItem.type && 
               action == syncItem.action;
    }
    
    @Override
    public int hashCode() {
        return id.hashCode() + type.hashCode() + action.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("SyncItem{id='%s', type=%s, action=%s, timestamp=%d, retries=%d}", 
                           id, type, action, timestamp, retryCount);
    }
}
