package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import fr.didictateur.inanutshell.data.model.Tool;
import fr.didictateur.inanutshell.data.network.NetworkManager;

/**
 * Gestionnaire de cache pour les outils et ustensiles
 */
public class ToolsManager {
    
    private static final String PREFS_NAME = "tools_cache";
    private static final String KEY_TOOLS = "cached_tools";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 heures
    
    private static ToolsManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<Tool> cachedTools;
    
    private ToolsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadFromCache();
    }
    
    public static synchronized ToolsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ToolsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Récupère les outils avec cache intelligent
     */
    public void getTools(ToolsCallback callback) {
        // Si le cache est valide, l'utiliser
        if (isCacheValid()) {
            callback.onSuccess(new ArrayList<>(cachedTools));
            return;
        }
        
        // Sinon, récupérer depuis le serveur
        refreshTools(callback);
    }
    
    /**
     * Force la récupération depuis le serveur
     */
    public void refreshTools(ToolsCallback callback) {
        NetworkManager.getInstance().getTools(new NetworkManager.ToolsCallback() {
            @Override
            public void onSuccess(List<Tool> tools) {
                // Mettre à jour le cache
                cachedTools = new ArrayList<>(tools);
                saveToCache();
                callback.onSuccess(tools);
            }
            
            @Override
            public void onError(String error) {
                // En cas d'erreur, utiliser le cache s'il existe
                if (cachedTools != null && !cachedTools.isEmpty()) {
                    callback.onSuccess(new ArrayList<>(cachedTools));
                } else {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Recherche un outil par nom
     */
    public Tool findToolByName(String name) {
        if (cachedTools == null || name == null) {
            return null;
        }
        
        for (Tool tool : cachedTools) {
            if (name.equalsIgnoreCase(tool.getName())) {
                return tool;
            }
        }
        return null;
    }
    
    /**
     * Recherche des outils par nom partiel
     */
    public List<Tool> searchTools(String query) {
        List<Tool> results = new ArrayList<>();
        if (cachedTools == null || query == null || query.trim().isEmpty()) {
            return results;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        for (Tool tool : cachedTools) {
            if (tool.getName().toLowerCase().contains(lowerQuery)) {
                results.add(tool);
            }
        }
        return results;
    }
    
    /**
     * Vérifie si le cache est valide
     */
    private boolean isCacheValid() {
        if (cachedTools == null || cachedTools.isEmpty()) {
            return false;
        }
        
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        return (System.currentTimeMillis() - lastUpdate) < CACHE_DURATION;
    }
    
    /**
     * Charge les outils depuis le cache
     */
    private void loadFromCache() {
        String json = prefs.getString(KEY_TOOLS, null);
        if (json != null) {
            Type listType = new TypeToken<List<Tool>>(){}.getType();
            cachedTools = gson.fromJson(json, listType);
        } else {
            cachedTools = new ArrayList<>();
        }
    }
    
    /**
     * Sauvegarde les outils dans le cache
     */
    private void saveToCache() {
        String json = gson.toJson(cachedTools);
        prefs.edit()
             .putString(KEY_TOOLS, json)
             .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
             .apply();
    }
    
    /**
     * Vide le cache
     */
    public void clearCache() {
        cachedTools.clear();
        prefs.edit().clear().apply();
    }
    
    /**
     * Interface pour les callbacks
     */
    public interface ToolsCallback {
        void onSuccess(List<Tool> tools);
        void onError(String error);
    }
}
