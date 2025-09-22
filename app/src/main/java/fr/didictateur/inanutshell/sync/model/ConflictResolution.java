package fr.didictateur.inanutshell.sync.model;

import fr.didictateur.inanutshell.data.model.Recipe;

/**
 * Représente un conflit de synchronisation entre deux versions d'un élément
 */
public class ConflictResolution {
    
    public enum Strategy {
        USE_LOCAL,      // Utiliser la version locale
        USE_SERVER,     // Utiliser la version serveur
        MERGE,          // Fusionner les deux versions
        ASK_USER        // Demander à l'utilisateur
    }
    
    public enum Type {
        RECIPE_CONFLICT,
        MEAL_PLAN_CONFLICT,
        SHOPPING_LIST_CONFLICT
    }
    
    private final String id;
    private final Type type;
    private final Object localVersion;
    private final Object serverVersion;
    private Strategy strategy;
    private Object resolvedVersion;
    private boolean resolved;
    private long timestamp;
    
    public ConflictResolution(String id, Type type, Object localVersion, Object serverVersion) {
        this.id = id;
        this.type = type;
        this.localVersion = localVersion;
        this.serverVersion = serverVersion;
        this.strategy = Strategy.ASK_USER; // Par défaut, demander à l'utilisateur
        this.resolved = false;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public String getId() { return id; }
    public Type getType() { return type; }
    public Object getLocalVersion() { return localVersion; }
    public Object getServerVersion() { return serverVersion; }
    public Strategy getStrategy() { return strategy; }
    public Object getResolvedVersion() { return resolvedVersion; }
    public boolean isResolved() { return resolved; }
    public long getTimestamp() { return timestamp; }
    
    // Méthodes de résolution
    public void resolveWithLocal() {
        this.strategy = Strategy.USE_LOCAL;
        this.resolvedVersion = localVersion;
        this.resolved = true;
    }
    
    public void resolveWithServer() {
        this.strategy = Strategy.USE_SERVER;
        this.resolvedVersion = serverVersion;
        this.resolved = true;
    }
    
    public void resolveWithMerge(Object mergedVersion) {
        this.strategy = Strategy.MERGE;
        this.resolvedVersion = mergedVersion;
        this.resolved = true;
    }
    
    public void resolveWithCustom(Object customVersion) {
        this.strategy = Strategy.ASK_USER;
        this.resolvedVersion = customVersion;
        this.resolved = true;
    }
    
    // Méthodes utilitaires pour les recettes
    public Recipe getLocalRecipe() {
        return type == Type.RECIPE_CONFLICT ? (Recipe) localVersion : null;
    }
    
    public Recipe getServerRecipe() {
        return type == Type.RECIPE_CONFLICT ? (Recipe) serverVersion : null;
    }
    
    public String getConflictDescription() {
        switch (type) {
            case RECIPE_CONFLICT:
                Recipe local = getLocalRecipe();
                Recipe server = getServerRecipe();
                if (local != null && server != null) {
                    return String.format("Conflit sur la recette '%s':\n" +
                                       "Version locale: modifiée le %tc\n" +
                                       "Version serveur: modifiée le %tc",
                                       local.getName(), 
                                       local.getUpdatedAt(), 
                                       server.getUpdatedAt());
                }
                break;
            case MEAL_PLAN_CONFLICT:
                return "Conflit sur un plan de repas";
            case SHOPPING_LIST_CONFLICT:
                return "Conflit sur une liste de courses";
        }
        return "Conflit de synchronisation";
    }
    
    @Override
    public String toString() {
        return String.format("ConflictResolution{id='%s', type=%s, strategy=%s, resolved=%b}", 
                           id, type, strategy, resolved);
    }
}
