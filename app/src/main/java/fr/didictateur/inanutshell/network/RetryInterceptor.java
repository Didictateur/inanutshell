package fr.didictateur.inanutshell.network;

import android.util.Log;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Interceptor pour la gestion automatique des tentatives de retry en cas d'erreur réseau
 */
public class RetryInterceptor implements Interceptor {
    private static final String TAG = "RetryInterceptor";
    
    // Configuration du retry
    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    
    // Compteurs pour les statistiques
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger retriedRequests = new AtomicInteger(0);
    
    public RetryInterceptor() {
        this(3, 1000, 2.0, 10000);
    }
    
    public RetryInterceptor(int maxRetries, long baseDelayMs, double backoffMultiplier, long maxDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
    }
    
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        totalRequests.incrementAndGet();
        
        Response response = null;
        IOException lastException = null;
        
        // Tentatives avec exponential backoff
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    // Calculer le délai d'attente avec exponential backoff
                    long delay = calculateDelay(attempt);
                    Log.d(TAG, String.format("Retry attempt %d/%d for %s after %dms", 
                        attempt, maxRetries, request.url(), delay));
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted during retry delay", e);
                    }
                    
                    retriedRequests.incrementAndGet();
                }
                
                response = chain.proceed(request);
                
                // Vérifier si la réponse est un succès ou si on doit retry
                if (response.isSuccessful()) {
                    successfulRequests.incrementAndGet();
                    return response;
                } else if (!shouldRetry(response.code(), attempt)) {
                    // Erreur qui ne justifie pas un retry (4xx errors)
                    Log.w(TAG, String.format("Non-retryable error %d for %s", 
                        response.code(), request.url()));
                    return response;
                } else if (attempt < maxRetries) {
                    // Fermer la réponse avant de retry
                    response.close();
                    Log.w(TAG, String.format("Server error %d, will retry %s", 
                        response.code(), request.url()));
                } else {
                    // Dernière tentative échouée
                    Log.e(TAG, String.format("Max retries exceeded for %s, final response: %d", 
                        request.url(), response.code()));
                    return response;
                }
                
            } catch (IOException e) {
                lastException = e;
                
                if (!shouldRetryException(e) || attempt >= maxRetries) {
                    Log.e(TAG, String.format("Network error for %s, attempt %d/%d: %s", 
                        request.url(), attempt + 1, maxRetries + 1, e.getMessage()));
                    throw e;
                }
                
                Log.w(TAG, String.format("Network error for %s, attempt %d/%d: %s. Retrying...", 
                    request.url(), attempt + 1, maxRetries + 1, e.getMessage()));
            }
        }
        
        // Si on arrive ici, toutes les tentatives ont échoué
        if (lastException != null) {
            throw lastException;
        }
        
        // Ne devrait jamais arriver, mais au cas où
        return response;
    }
    
    /**
     * Calcule le délai d'attente avec exponential backoff
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attempt - 1));
        
        // Ajouter un peu de jitter pour éviter le thundering herd
        double jitter = 0.1 * delay * Math.random();
        delay += (long) jitter;
        
        return Math.min(delay, maxDelayMs);
    }
    
    /**
     * Détermine si on doit retry basé sur le code de statut HTTP
     */
    private boolean shouldRetry(int responseCode, int attempt) {
        // Retry sur les erreurs serveur (5xx) et certaines erreurs client
        switch (responseCode) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                return true;
            case 401: // Unauthorized - peut être temporaire
            case 403: // Forbidden - peut être temporaire
                return attempt == 0; // Retry une seule fois
            default:
                return false;
        }
    }
    
    /**
     * Détermine si on doit retry basé sur le type d'exception
     */
    private boolean shouldRetryException(IOException exception) {
        // Retry sur les erreurs de connexion temporaires
        if (exception instanceof SocketTimeoutException) {
            return true; // Timeout - probablement temporaire
        }
        
        if (exception instanceof UnknownHostException) {
            return true; // DNS/résolution nom - peut être temporaire
        }
        
        // Vérifier les messages d'erreur pour d'autres cas temporaires
        String message = exception.getMessage();
        if (message != null) {
            message = message.toLowerCase();
            
            // Erreurs de connexion temporaires
            if (message.contains("connection reset") ||
                message.contains("connection refused") ||
                message.contains("network is unreachable") ||
                message.contains("no route to host") ||
                message.contains("software caused connection abort")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Obtient les statistiques de retry
     */
    public RetryStats getStats() {
        return new RetryStats(
            totalRequests.get(),
            successfulRequests.get(),
            retriedRequests.get()
        );
    }
    
    /**
     * Remet à zéro les statistiques
     */
    public void resetStats() {
        totalRequests.set(0);
        successfulRequests.set(0);
        retriedRequests.set(0);
    }
    
    /**
     * Classe pour les statistiques de retry
     */
    public static class RetryStats {
        public final int totalRequests;
        public final int successfulRequests;
        public final int retriedRequests;
        public final double successRate;
        public final double retryRate;
        
        public RetryStats(int totalRequests, int successfulRequests, int retriedRequests) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.retriedRequests = retriedRequests;
            this.successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
            this.retryRate = totalRequests > 0 ? (double) retriedRequests / totalRequests : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("RetryStats{total=%d, success=%d (%.1f%%), retried=%d (%.1f%%)}", 
                totalRequests, successfulRequests, successRate * 100, 
                retriedRequests, retryRate * 100);
        }
    }
}
