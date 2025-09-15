package fr.didictateur.inanutshell.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestionnaire pour surveiller l'état de la connexion réseau
 */
public class NetworkStateManager {
    private static final String TAG = "NetworkStateManager";
    
    private static NetworkStateManager instance;
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isWifi = new AtomicBoolean(false);
    private final AtomicBoolean isMetered = new AtomicBoolean(false);
    
    // LiveData pour observer les changements
    private final MutableLiveData<NetworkState> networkStateLiveData = new MutableLiveData<>();
    
    // Callbacks pour les changements d'état
    private final List<NetworkStateListener> listeners = new ArrayList<>();
    
    // Callback du système pour Android N+ (API 24+)
    private ConnectivityManager.NetworkCallback networkCallback;
    
    /**
     * Interface pour écouter les changements d'état réseau
     */
    public interface NetworkStateListener {
        void onNetworkStateChanged(NetworkState state);
    }
    
    /**
     * Énumération des états possibles du réseau
     */
    public enum ConnectionType {
        NONE,       // Pas de connexion
        WIFI,       // WiFi
        CELLULAR,   // Données mobiles
        ETHERNET,   // Ethernet
        OTHER       // Autre type de connexion
    }
    
    /**
     * Classe représentant l'état actuel du réseau
     */
    public static class NetworkState {
        public final boolean isConnected;
        public final ConnectionType connectionType;
        public final boolean isMetered;
        public final boolean isValidated;
        public final long timestamp;
        
        public NetworkState(boolean isConnected, ConnectionType connectionType, 
                          boolean isMetered, boolean isValidated) {
            this.isConnected = isConnected;
            this.connectionType = connectionType;
            this.isMetered = isMetered;
            this.isValidated = isValidated;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("NetworkState{connected=%s, type=%s, metered=%s, validated=%s}", 
                isConnected, connectionType, isMetered, isValidated);
        }
    }
    
    private NetworkStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        initializeNetworkMonitoring();
        updateNetworkState();
    }
    
    public static synchronized NetworkStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkStateManager(context);
        }
        return instance;
    }
    
    /**
     * Initialise la surveillance du réseau selon la version d'Android
     */
    private void initializeNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android N+ : Utiliser NetworkCallback
            initializeModernNetworkMonitoring();
        } else {
            // Android plus ancien : Utiliser BroadcastReceiver (deprecated mais nécessaire)
            initializeLegacyNetworkMonitoring();
        }
    }
    
    /**
     * Surveillance moderne pour Android N+ (API 24+)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeModernNetworkMonitoring() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network available: " + network);
                updateNetworkState();
            }
            
            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "Network lost: " + network);
                updateNetworkState();
            }
            
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                super.onCapabilitiesChanged(network, capabilities);
                Log.d(TAG, "Network capabilities changed: " + network);
                updateNetworkState();
            }
            
            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.d(TAG, "Network unavailable");
                updateNetworkState();
            }
        };
        
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }
    
    /**
     * Surveillance legacy pour Android < N
     */
    private void initializeLegacyNetworkMonitoring() {
        // TODO: Implémenter BroadcastReceiver pour CONNECTIVITY_ACTION
        // Nécessaire pour les anciennes versions d'Android
        Log.w(TAG, "Legacy network monitoring not implemented yet");
    }
    
    /**
     * Met à jour l'état actuel du réseau
     */
    public void updateNetworkState() {
        NetworkState state = getCurrentNetworkState();
        
        // Mettre à jour les variables atomiques
        isConnected.set(state.isConnected);
        isWifi.set(state.connectionType == ConnectionType.WIFI);
        isMetered.set(state.isMetered);
        
        // Notifier via LiveData
        networkStateLiveData.postValue(state);
        
        // Notifier les listeners
        synchronized (listeners) {
            for (NetworkStateListener listener : listeners) {
                try {
                    listener.onNetworkStateChanged(state);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying network state listener", e);
                }
            }
        }
        
        Log.d(TAG, "Network state updated: " + state);
    }
    
    /**
     * Obtient l'état actuel du réseau
     */
    public NetworkState getCurrentNetworkState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getCurrentNetworkStateModern();
        } else {
            return getCurrentNetworkStateLegacy();
        }
    }
    
    /**
     * Obtient le LiveData pour observer les changements d'état réseau
     */
    public LiveData<NetworkState> getNetworkStateLiveData() {
        return networkStateLiveData;
    }
    
    /**
     * Obtient l'état du réseau pour Android M+ (API 23+)
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private NetworkState getCurrentNetworkStateModern() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return new NetworkState(false, ConnectionType.NONE, false, false);
        }
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return new NetworkState(false, ConnectionType.NONE, false, false);
        }
        
        // Déterminer le type de connexion
        ConnectionType connectionType = ConnectionType.OTHER;
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connectionType = ConnectionType.WIFI;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            connectionType = ConnectionType.CELLULAR;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            connectionType = ConnectionType.ETHERNET;
        }
        
        // Vérifier si la connexion est mesurée
        boolean isMetered = connectivityManager.isActiveNetworkMetered();
        
        // Vérifier si la connexion est validée (a accès Internet)
        boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        
        return new NetworkState(true, connectionType, isMetered, isValidated);
    }
    
    /**
     * Obtient l'état du réseau pour Android < M (legacy)
     */
    @SuppressWarnings("deprecation")
    private NetworkState getCurrentNetworkStateLegacy() {
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            return new NetworkState(false, ConnectionType.NONE, false, false);
        }
        
        // Déterminer le type de connexion
        ConnectionType connectionType = ConnectionType.OTHER;
        int networkType = activeNetworkInfo.getType();
        if (networkType == ConnectivityManager.TYPE_WIFI) {
            connectionType = ConnectionType.WIFI;
        } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
            connectionType = ConnectionType.CELLULAR;
        } else if (networkType == ConnectivityManager.TYPE_ETHERNET) {
            connectionType = ConnectionType.ETHERNET;
        }
        
        // Pour les anciennes versions, on considère que mobile = metered
        boolean isMetered = (networkType == ConnectivityManager.TYPE_MOBILE);
        
        // Pour les anciennes versions, on considère que connecté = validé
        boolean isValidated = activeNetworkInfo.isConnected();
        
        return new NetworkState(true, connectionType, isMetered, isValidated);
    }
    
    // ===================== API publique =====================
    
    /**
     * Vérifie si le réseau est connecté
     */
    public boolean isConnected() {
        return isConnected.get();
    }
    
    /**
     * Vérifie si la connexion est en WiFi
     */
    public boolean isWifi() {
        return isWifi.get();
    }
    
    /**
     * Vérifie si la connexion est mesurée (données mobiles)
     */
    public boolean isMetered() {
        return isMetered.get();
    }
    
    /**
     * Obtient le LiveData de l'état du réseau
     */
    public LiveData<NetworkState> getNetworkStateLive() {
        return networkStateLiveData;
    }
    
    /**
     * Ajoute un listener pour les changements d'état
     */
    public void addListener(NetworkStateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Supprime un listener
     */
    public void removeListener(NetworkStateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Vérifie si on peut faire des opérations coûteuses en réseau
     */
    public boolean canDoExpensiveOperations() {
        NetworkState state = getCurrentNetworkState();
        return state.isConnected && !state.isMetered;
    }
    
    /**
     * Obtient une description textuelle de l'état du réseau
     */
    public String getNetworkDescription() {
        NetworkState state = getCurrentNetworkState();
        if (!state.isConnected) {
            return "Pas de connexion";
        }
        
        String description = "";
        switch (state.connectionType) {
            case WIFI:
                description = "WiFi";
                break;
            case CELLULAR:
                description = "Données mobiles";
                break;
            case ETHERNET:
                description = "Ethernet";
                break;
            case OTHER:
                description = "Autre connexion";
                break;
        }
        
        if (state.isMetered) {
            description += " (limitée)";
        }
        
        if (!state.isValidated) {
            description += " (sans Internet)";
        }
        
        return description;
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
        
        synchronized (listeners) {
            listeners.clear();
        }
    }
}
