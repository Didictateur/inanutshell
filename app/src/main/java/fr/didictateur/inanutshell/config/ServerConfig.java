package fr.didictateur.inanutshell.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.net.MalformedURLException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Entité pour la configuration des serveurs Mealie
 */
@Entity(tableName = "server_configs")
public class ServerConfig {
    private static final String TAG = "ServerConfig";
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;               // Nom convivial du serveur
    private String baseUrl;            // URL de base (ex: https://mealie.example.com)
    private String apiKey;             // Clé API pour l'authentification
    private String username;           // Nom d'utilisateur (optionnel)
    private boolean isDefault;         // Serveur par défaut
    private boolean isEnabled;         // Serveur activé
    private long lastConnected;        // Timestamp de dernière connexion
    private int priority;              // Priorité (plus haut = préféré)
    private boolean allowSelfSigned;   // Autoriser les certificats auto-signés
    private int timeoutSeconds;        // Timeout en secondes
    private String description;        // Description du serveur
    private ServerStatus status;       // État du serveur
    private long lastStatusCheck;      // Dernière vérification d'état
    private String version;            // Version de Mealie détectée
    private boolean syncEnabled;       // Synchronisation activée
    
    // États possibles du serveur
    public enum ServerStatus {
        UNKNOWN,      // État inconnu
        ONLINE,       // En ligne et accessible
        OFFLINE,      // Hors ligne ou inaccessible
        ERROR,        // Erreur de connexion
        UNAUTHORIZED, // Problème d'authentification
        OUTDATED      // Version trop ancienne
    }
    
    // Constructeur par défaut pour Room
    public ServerConfig() {
        this.isDefault = false;
        this.isEnabled = true;
        this.priority = 0;
        this.allowSelfSigned = false;
        this.timeoutSeconds = 30;
        this.status = ServerStatus.UNKNOWN;
        this.lastStatusCheck = 0;
        this.syncEnabled = true;
    }
    
    // Constructeur complet
    @Ignore
    public ServerConfig(String name, String baseUrl, String apiKey) {
        this();
        this.name = name;
        this.baseUrl = normalizeUrl(baseUrl);
        this.apiKey = apiKey;
    }
    
    /**
     * Normalise l'URL du serveur
     */
    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        
        url = url.trim();
        
        // Ajouter https:// si aucun protocole
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        // Supprimer le slash final
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
    
    /**
     * Valide la configuration du serveur
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        
        if (name == null || name.trim().isEmpty()) {
            errors.add("Le nom du serveur est requis");
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            errors.add("L'URL du serveur est requise");
        } else {
            try {
                URL url = new URL(baseUrl);
                String protocol = url.getProtocol();
                if (!"http".equals(protocol) && !"https".equals(protocol)) {
                    errors.add("Seuls les protocoles HTTP et HTTPS sont supportés");
                }
                
                String host = url.getHost();
                if (host == null || host.trim().isEmpty()) {
                    errors.add("L'URL doit contenir un nom d'hôte valide");
                }
                
                int port = url.getPort();
                if (port != -1 && (port < 1 || port > 65535)) {
                    errors.add("Le port doit être entre 1 et 65535");
                }
                
            } catch (MalformedURLException e) {
                errors.add("URL invalide: " + e.getMessage());
            }
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            errors.add("La clé API est requise");
        } else if (apiKey.length() < 10) {
            errors.add("La clé API semble trop courte");
        }
        
        if (timeoutSeconds < 5 || timeoutSeconds > 300) {
            errors.add("Le timeout doit être entre 5 et 300 secondes");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Obtient l'URL complète de l'API
     */
    public String getApiUrl() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "";
        }
        
        String apiPath = "/api";
        if (baseUrl.contains("/api")) {
            return baseUrl;
        }
        
        return baseUrl + apiPath;
    }
    
    /**
     * Obtient l'URL pour les médias
     */
    public String getMediaUrl() {
        return baseUrl + "/api/media";
    }
    
    /**
     * Vérifie si le serveur utilise HTTPS
     */
    public boolean isSecure() {
        return baseUrl != null && baseUrl.startsWith("https://");
    }
    
    /**
     * Vérifie si le serveur est local
     */
    public boolean isLocal() {
        if (baseUrl == null) return false;
        
        return baseUrl.contains("localhost") || 
               baseUrl.contains("127.0.0.1") || 
               baseUrl.contains("192.168.") ||
               baseUrl.contains("10.") ||
               baseUrl.contains("172.16.") ||
               baseUrl.contains("172.17.") ||
               baseUrl.contains("172.18.") ||
               baseUrl.contains("172.19.") ||
               baseUrl.contains("172.2") ||
               baseUrl.contains("172.30") ||
               baseUrl.contains("172.31");
    }
    
    /**
     * Met à jour le statut de connexion
     */
    public void updateStatus(ServerStatus newStatus) {
        this.status = newStatus;
        this.lastStatusCheck = System.currentTimeMillis();
        
        if (newStatus == ServerStatus.ONLINE) {
            this.lastConnected = System.currentTimeMillis();
        }
        
        Log.d(TAG, "Server " + name + " status updated to: " + newStatus);
    }
    
    /**
     * Vérifie si le statut est récent
     */
    public boolean isStatusFresh() {
        long maxAge = 5 * 60 * 1000; // 5 minutes
        return System.currentTimeMillis() - lastStatusCheck < maxAge;
    }
    
    /**
     * Crée une copie de test de la configuration
     */
    public ServerConfig createTestCopy() {
        ServerConfig copy = new ServerConfig();
        copy.name = this.name + " (Test)";
        copy.baseUrl = this.baseUrl;
        copy.apiKey = this.apiKey;
        copy.username = this.username;
        copy.allowSelfSigned = this.allowSelfSigned;
        copy.timeoutSeconds = Math.min(this.timeoutSeconds, 10); // Timeout réduit pour les tests
        copy.description = "Configuration de test pour " + this.name;
        return copy;
    }
    
    /**
     * Compare deux configurations pour détecter les changements
     */
    public boolean isEquivalentTo(ServerConfig other) {
        if (other == null) return false;
        
        return java.util.Objects.equals(this.baseUrl, other.baseUrl) &&
               java.util.Objects.equals(this.apiKey, other.apiKey) &&
               java.util.Objects.equals(this.username, other.username) &&
               this.allowSelfSigned == other.allowSelfSigned &&
               this.timeoutSeconds == other.timeoutSeconds;
    }
    
    @Override
    public String toString() {
        return String.format("ServerConfig{name='%s', url='%s', status=%s, enabled=%s, default=%s}", 
            name, baseUrl, status, isEnabled, isDefault);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ServerConfig that = (ServerConfig) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = normalizeUrl(baseUrl); }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }
    
    public long getLastConnected() { return lastConnected; }
    public void setLastConnected(long lastConnected) { this.lastConnected = lastConnected; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isAllowSelfSigned() { return allowSelfSigned; }
    public void setAllowSelfSigned(boolean allowSelfSigned) { this.allowSelfSigned = allowSelfSigned; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ServerStatus getStatus() { return status; }
    public void setStatus(ServerStatus status) { this.status = status; }
    
    public long getLastStatusCheck() { return lastStatusCheck; }
    public void setLastStatusCheck(long lastStatusCheck) { this.lastStatusCheck = lastStatusCheck; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public boolean isSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }
    
    /**
     * Résultat de validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        
        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
    
    /**
     * Builder pour créer facilement des configurations
     */
    public static class Builder {
        private final ServerConfig config;
        
        public Builder(String name, String baseUrl) {
            config = new ServerConfig();
            config.setName(name);
            config.setBaseUrl(baseUrl);
        }
        
        public Builder apiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }
        
        public Builder username(String username) {
            config.setUsername(username);
            return this;
        }
        
        public Builder description(String description) {
            config.setDescription(description);
            return this;
        }
        
        public Builder timeout(int seconds) {
            config.setTimeoutSeconds(seconds);
            return this;
        }
        
        public Builder priority(int priority) {
            config.setPriority(priority);
            return this;
        }
        
        public Builder allowSelfSigned(boolean allow) {
            config.setAllowSelfSigned(allow);
            return this;
        }
        
        public Builder setDefault(boolean isDefault) {
            config.setDefault(isDefault);
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }
        
        public Builder syncEnabled(boolean enabled) {
            config.setSyncEnabled(enabled);
            return this;
        }
        
        public ServerConfig build() {
            ValidationResult validation = config.validate();
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Configuration invalide: " + validation.getErrorMessage());
            }
            return config;
        }
    }
}
