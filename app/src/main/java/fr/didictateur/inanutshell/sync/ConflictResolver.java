package fr.didictateur.inanutshell.sync;

import android.util.Log;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.sync.model.ConflictResolution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de résolution de conflits de synchronisation
 */
public class ConflictResolver {
    
    private static final String TAG = "ConflictResolver";
    
    private final Map<String, ConflictResolution> activeConflicts = new ConcurrentHashMap<>();
    
    /**
     * Ajoute un conflit à résoudre
     */
    public void addConflict(Object localVersion, Object serverVersion) {
        String id = generateConflictId(localVersion);
        ConflictResolution.Type type = determineConflictType(localVersion);
        
        ConflictResolution conflict = new ConflictResolution(id, type, localVersion, serverVersion);
        activeConflicts.put(id, conflict);
        
        Log.d(TAG, "Nouveau conflit ajouté: " + conflict.getConflictDescription());
        
        // Tenter une résolution automatique
        attemptAutoResolution(conflict);
    }
    
    /**
     * Obtient tous les conflits en attente
     */
    public List<ConflictResolution> getConflicts() {
        return new ArrayList<>(activeConflicts.values());
    }
    
    /**
     * Obtient un conflit spécifique par ID
     */
    public ConflictResolution getConflict(String id) {
        return activeConflicts.get(id);
    }
    
    /**
     * Résout un conflit manuellement
     */
    public void resolveConflict(String conflictId, ConflictResolution.Strategy strategy, Object customVersion) {
        ConflictResolution conflict = activeConflicts.get(conflictId);
        if (conflict == null) {
            Log.w(TAG, "Conflit introuvable: " + conflictId);
            return;
        }
        
        switch (strategy) {
            case USE_LOCAL:
                conflict.resolveWithLocal();
                break;
            case USE_SERVER:
                conflict.resolveWithServer();
                break;
            case MERGE:
                Object merged = performMerge(conflict.getLocalVersion(), conflict.getServerVersion());
                conflict.resolveWithMerge(merged);
                break;
            case ASK_USER:
                if (customVersion != null) {
                    conflict.resolveWithCustom(customVersion);
                }
                break;
        }
        
        if (conflict.isResolved()) {
            Log.d(TAG, "Conflit résolu: " + conflictId + " avec stratégie " + strategy);
            // Garder le conflit résolu pour traitement ultérieur
        }
    }
    
    /**
     * Supprime un conflit résolu
     */
    public void removeConflict(String conflictId) {
        ConflictResolution removed = activeConflicts.remove(conflictId);
        if (removed != null) {
            Log.d(TAG, "Conflit supprimé: " + conflictId);
        }
    }
    
    /**
     * Supprime tous les conflits résolus
     */
    public void clearResolvedConflicts() {
        Iterator<Map.Entry<String, ConflictResolution>> iterator = activeConflicts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ConflictResolution> entry = iterator.next();
            if (entry.getValue().isResolved()) {
                iterator.remove();
                Log.d(TAG, "Conflit résolu supprimé: " + entry.getKey());
            }
        }
    }
    
    /**
     * Vérifie s'il y a des conflits en attente
     */
    public boolean hasUnresolvedConflicts() {
        return activeConflicts.values().stream()
            .anyMatch(conflict -> !conflict.isResolved());
    }
    
    /**
     * Compte le nombre de conflits non résolus
     */
    public int getUnresolvedConflictCount() {
        return (int) activeConflicts.values().stream()
            .filter(conflict -> !conflict.isResolved())
            .count();
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    private String generateConflictId(Object item) {
        if (item instanceof Recipe) {
            return "recipe_" + ((Recipe) item).getId();
        }
        // Ajouter d'autres types selon besoin
        return "unknown_" + item.hashCode();
    }
    
    private ConflictResolution.Type determineConflictType(Object item) {
        if (item instanceof Recipe) {
            return ConflictResolution.Type.RECIPE_CONFLICT;
        }
        // Ajouter d'autres types selon besoin
        return ConflictResolution.Type.RECIPE_CONFLICT;
    }
    
    /**
     * Tente une résolution automatique basée sur des règles prédéfinies
     */
    private void attemptAutoResolution(ConflictResolution conflict) {
        switch (conflict.getType()) {
            case RECIPE_CONFLICT:
                attemptRecipeAutoResolution(conflict);
                break;
            case MEAL_PLAN_CONFLICT:
            case SHOPPING_LIST_CONFLICT:
                // Pas de résolution auto pour l'instant
                break;
        }
    }
    
    private void attemptRecipeAutoResolution(ConflictResolution conflict) {
        Recipe local = conflict.getLocalRecipe();
        Recipe server = conflict.getServerRecipe();
        
        if (local == null || server == null) return;
        
        // Règle 1: Si une version est beaucoup plus récente (> 1 heure)
        long timeDiff = Math.abs(local.getUpdatedAtTimestamp() - server.getUpdatedAtTimestamp());
        if (timeDiff > 3600000) { // 1 heure
            if (local.getUpdatedAtTimestamp() > server.getUpdatedAtTimestamp()) {
                conflict.resolveWithLocal();
                Log.d(TAG, "Auto-résolution: version locale plus récente");
            } else {
                conflict.resolveWithServer();
                Log.d(TAG, "Auto-résolution: version serveur plus récente");
            }
            return;
        }
        
        // Règle 2: Si seule une version a changé les ingrédients
        boolean localIngredientsChanged = hasIngredientsChanged(local);
        boolean serverIngredientsChanged = hasIngredientsChanged(server);
        
        if (localIngredientsChanged && !serverIngredientsChanged) {
            conflict.resolveWithLocal();
            Log.d(TAG, "Auto-résolution: ingrédients modifiés localement");
            return;
        } else if (serverIngredientsChanged && !localIngredientsChanged) {
            conflict.resolveWithServer();
            Log.d(TAG, "Auto-résolution: ingrédients modifiés sur le serveur");
            return;
        }
        
        // Règle 3: Tentative de fusion automatique pour les changements mineurs
        if (canAutoMerge(local, server)) {
            Recipe merged = performRecipeMerge(local, server);
            conflict.resolveWithMerge(merged);
            Log.d(TAG, "Auto-résolution: fusion automatique réussie");
        }
        
        // Sinon, laisser pour résolution manuelle
    }
    
    private boolean hasIngredientsChanged(Recipe recipe) {
        // Logique pour détecter si les ingrédients ont changé
        // Pour simplifier, on considère que tout changement récent affecte les ingrédients
        return recipe.getUpdatedAtTimestamp() > System.currentTimeMillis() - 86400000; // 24h
    }
    
    private boolean canAutoMerge(Recipe local, Recipe server) {
        // Vérifier si les changements sont compatibles pour une fusion automatique
        
        // Si les noms sont différents, pas de fusion auto
        if (!Objects.equals(local.getName(), server.getName())) {
            return false;
        }
        
        // Si les temps de cuisson sont très différents, pas de fusion auto
        if (Math.abs(local.getCookingTime() - server.getCookingTime()) > 30) {
            return false;
        }
        
        // Autres vérifications selon les besoins
        return true;
    }
    
    private Recipe performRecipeMerge(Recipe local, Recipe server) {
        // Créer une nouvelle recette fusionnée
        Recipe merged = new Recipe();
        
        // Garder l'ID de la version locale
        merged.setId(local.getId());
        
        // Nom: prendre le plus récent
        if (local.getUpdatedAtTimestamp() > server.getUpdatedAtTimestamp()) {
            merged.setName(local.getName());
        } else {
            merged.setName(server.getName());
        }
        
        // Description: fusionner si possible
        String mergedDescription = mergeDescriptions(local.getDescription(), server.getDescription());
        merged.setDescription(mergedDescription);
        
        // Temps de cuisson: prendre la moyenne si proche, sinon le plus récent
        int cookingTimeDiff = Math.abs(local.getCookingTime() - server.getCookingTime());
        if (cookingTimeDiff <= 15) {
            merged.setCookingTime((local.getCookingTime() + server.getCookingTime()) / 2);
        } else {
            merged.setCookingTime(local.getUpdatedAtTimestamp() > server.getUpdatedAtTimestamp() ? 
                                local.getCookingTime() : server.getCookingTime());
        }
        
        // Ingrédients: fusionner les listes
        // TODO: Implémenter la fusion des ingrédients
        
        // Instructions: prendre les plus récentes
        if (local.getUpdatedAtTimestamp() > server.getUpdatedAtTimestamp()) {
            merged.setInstructions(local.getInstructions());
        } else {
            merged.setInstructions(server.getInstructions());
        }
        
        // Mettre à jour le timestamp
        merged.updateTimestamp();
        
        return merged;
    }
    
    private String mergeDescriptions(String local, String server) {
        if (Objects.equals(local, server)) {
            return local;
        }
        
        if (local == null || local.trim().isEmpty()) {
            return server;
        }
        
        if (server == null || server.trim().isEmpty()) {
            return local;
        }
        
        // Si les descriptions sont différentes, les combiner
        return local + "\n\n[Fusion serveur]: " + server;
    }
    
    private Object performMerge(Object localVersion, Object serverVersion) {
        if (localVersion instanceof Recipe && serverVersion instanceof Recipe) {
            return performRecipeMerge((Recipe) localVersion, (Recipe) serverVersion);
        }
        
        // Pour d'autres types, retourner la version locale par défaut
        return localVersion;
    }
}
