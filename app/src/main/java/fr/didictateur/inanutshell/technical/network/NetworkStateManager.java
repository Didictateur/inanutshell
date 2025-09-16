package fr.didictateur.inanutshell.technical.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class NetworkStateManager {
    private static NetworkStateManager instance;
    private Context context;
    private ConnectivityManager connectivityManager;
    private List<NetworkStateListener> listeners;
    private Handler mainHandler;
    private NetworkCallback networkCallback;
    
    public enum NetworkType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        OTHER
    }
    
    public enum NetworkState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        WEAK_SIGNAL
    }
    
    public interface NetworkStateListener {
        void onNetworkStateChanged(NetworkState state, NetworkType type);
        void onNetworkSpeedChanged(long speedKbps);
    }
    
    private NetworkStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        setupNetworkCallback();
        registerNetworkCallback();
    }
    
    public static NetworkStateManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkStateManager.class) {
                if (instance == null) {
                    instance = new NetworkStateManager(context);
                }
            }
        }
        return instance;
    }
    
    private void setupNetworkCallback() {
        networkCallback = new NetworkCallback();
    }
    
    private void registerNetworkCallback() {
        if (connectivityManager != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }
    
    public void addListener(NetworkStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }
    
    public boolean isConnected() {
        if (connectivityManager == null) return false;
        
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && 
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
    
    public NetworkType getCurrentNetworkType() {
        if (connectivityManager == null) return NetworkType.NONE;
        
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return NetworkType.NONE;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return NetworkType.NONE;
        
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.CELLULAR;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkType.ETHERNET;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return NetworkType.VPN;
        }
        
        return NetworkType.OTHER;
    }
    
    public NetworkState getCurrentNetworkState() {
        if (!isConnected()) {
            return NetworkState.DISCONNECTED;
        }
        
        // Check signal strength for cellular networks
        NetworkType type = getCurrentNetworkType();
        if (type == NetworkType.CELLULAR) {
            // This is a simplified check - in a real implementation,
            // you might want to use TelephonyManager for signal strength
            return NetworkState.CONNECTED;
        }
        
        return NetworkState.CONNECTED;
    }
    
    public boolean isWiFiConnected() {
        return getCurrentNetworkType() == NetworkType.WIFI;
    }
    
    public boolean isCellularConnected() {
        return getCurrentNetworkType() == NetworkType.CELLULAR;
    }
    
    public boolean isMeteredConnection() {
        if (connectivityManager == null) return false;
        
        return connectivityManager.isActiveNetworkMetered();
    }
    
    public long getNetworkSpeed() {
        // This is a placeholder - actual implementation would require
        // speed testing or using NetworkCapabilities.getLinkDownstreamBandwidthKbps()
        if (!isConnected()) return 0;
        
        NetworkType type = getCurrentNetworkType();
        switch (type) {
            case WIFI:
                return 10000; // Assume 10 Mbps for WiFi
            case CELLULAR:
                return 2000;  // Assume 2 Mbps for cellular
            case ETHERNET:
                return 50000; // Assume 50 Mbps for ethernet
            default:
                return 1000;  // Default speed
        }
    }
    
    private void notifyListeners(NetworkState state, NetworkType type) {
        mainHandler.post(() -> {
            for (NetworkStateListener listener : listeners) {
                listener.onNetworkStateChanged(state, type);
            }
        });
    }
    
    private void notifySpeedChanged(long speed) {
        mainHandler.post(() -> {
            for (NetworkStateListener listener : listeners) {
                listener.onNetworkSpeedChanged(speed);
            }
        });
    }
    
    public void destroy() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                // Ignore callback not registered exceptions
            }
        }
        listeners.clear();
    }
    
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            NetworkType type = getCurrentNetworkType();
            notifyListeners(NetworkState.CONNECTED, type);
            notifySpeedChanged(getNetworkSpeed());
        }
        
        @Override
        public void onLost(Network network) {
            super.onLost(network);
            notifyListeners(NetworkState.DISCONNECTED, NetworkType.NONE);
            notifySpeedChanged(0);
        }
        
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            NetworkType type = getCurrentNetworkType();
            NetworkState state = getCurrentNetworkState();
            notifyListeners(state, type);
        }
    }
}
