package fr.didictateur.inanutshell.sync.model;

/**
 * Représente l'état actuel de la synchronisation
 */
public class SyncStatus {
    
    public enum State {
        IDLE,           // En attente
        SYNCING,        // Synchronisation en cours
        COMPLETED,      // Synchronisation terminée
        ERROR,          // Erreur
        CONFLICTS,      // Conflits détectés
        DISABLED        // Synchronisation désactivée
    }
    
    private final State state;
    private final long lastSync;
    private final int totalItems;
    private final int processedItems;
    private final String message;
    
    public SyncStatus(State state, long lastSync, int totalItems, int processedItems, String message) {
        this.state = state;
        this.lastSync = lastSync;
        this.totalItems = totalItems;
        this.processedItems = processedItems;
        this.message = message;
    }
    
    // Getters
    public State getState() { return state; }
    public long getLastSync() { return lastSync; }
    public int getTotalItems() { return totalItems; }
    public int getProcessedItems() { return processedItems; }
    public String getMessage() { return message; }
    
    // Méthodes utilitaires
    public boolean isInProgress() {
        return state == State.SYNCING;
    }
    
    public boolean hasError() {
        return state == State.ERROR;
    }
    
    public boolean hasConflicts() {
        return state == State.CONFLICTS;
    }
    
    public int getProgress() {
        if (totalItems == 0) return 0;
        return (processedItems * 100) / totalItems;
    }
    
    @Override
    public String toString() {
        return String.format("SyncStatus{state=%s, lastSync=%d, progress=%d/%d, message='%s'}", 
                           state, lastSync, processedItems, totalItems, message);
    }
}
