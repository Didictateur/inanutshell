package fr.didictateur.inanutshell.network;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import retrofit2.HttpException;

/**
 * Gestionnaire centralisé pour la gestion et l'affichage des erreurs réseau
 */
public class ErrorHandler {
    
    private final Context context;
    
    public ErrorHandler(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Énumération des types d'erreurs possibles
     */
    public enum ErrorType {
        NETWORK_UNAVAILABLE,    // Pas de connexion réseau
        TIMEOUT,               // Timeout de connexion
        SERVER_ERROR,          // Erreur serveur (5xx)
        CLIENT_ERROR,          // Erreur client (4xx)
        AUTHENTICATION_ERROR,  // Erreur d'authentification (401/403)
        NOT_FOUND,            // Ressource non trouvée (404)
        RATE_LIMITED,         // Trop de requêtes (429)
        UNKNOWN_HOST,         // Serveur introuvable
        PARSING_ERROR,        // Erreur de parsing JSON
        UNKNOWN               // Erreur inconnue
    }
    
    /**
     * Classe représentant une erreur avec des informations détaillées
     */
    public static class ErrorInfo {
        public final ErrorType type;
        public final String message;
        public final String userMessage;
        public final boolean isRetryable;
        public final Throwable cause;
        public final long timestamp;
        
        public ErrorInfo(ErrorType type, String message, String userMessage, 
                        boolean isRetryable, Throwable cause) {
            this.type = type;
            this.message = message;
            this.userMessage = userMessage;
            this.isRetryable = isRetryable;
            this.cause = cause;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("ErrorInfo{type=%s, message='%s', retryable=%s}", 
                type, message, isRetryable);
        }
    }
    
    /**
     * Interface pour les callbacks d'erreur
     */
    public interface ErrorCallback {
        void onError(ErrorInfo errorInfo);
    }
    
    /**
     * Analyse une exception et retourne les informations d'erreur correspondantes
     */
    public ErrorInfo analyzeError(Throwable throwable) {
        if (throwable instanceof HttpException) {
            return analyzeHttpException((HttpException) throwable);
        } else if (throwable instanceof IOException) {
            return analyzeIOException((IOException) throwable);
        } else if (throwable instanceof TimeoutException || 
                   throwable instanceof SocketTimeoutException) {
            return new ErrorInfo(
                ErrorType.TIMEOUT,
                "Timeout de connexion",
                "La connexion a pris trop de temps. Vérifiez votre réseau et réessayez.",
                true,
                throwable
            );
        } else {
            return new ErrorInfo(
                ErrorType.UNKNOWN,
                throwable.getMessage() != null ? throwable.getMessage() : "Erreur inconnue",
                "Une erreur inattendue s'est produite. Veuillez réessayer.",
                true,
                throwable
            );
        }
    }
    
    /**
     * Analyse une HttpException (erreur HTTP de Retrofit)
     */
    private ErrorInfo analyzeHttpException(HttpException httpException) {
        int code = httpException.code();
        String message = httpException.message();
        
        switch (code) {
            case 400:
                return new ErrorInfo(
                    ErrorType.CLIENT_ERROR,
                    "Requête invalide (400)",
                    "Les données envoyées sont incorrectes.",
                    false,
                    httpException
                );
                
            case 401:
                return new ErrorInfo(
                    ErrorType.AUTHENTICATION_ERROR,
                    "Non autorisé (401)", 
                    "Vos identifiants sont incorrects ou ont expiré. Reconnectez-vous.",
                    false,
                    httpException
                );
                
            case 403:
                return new ErrorInfo(
                    ErrorType.AUTHENTICATION_ERROR,
                    "Accès interdit (403)",
                    "Vous n'avez pas les permissions nécessaires pour cette action.",
                    false,
                    httpException
                );
                
            case 404:
                return new ErrorInfo(
                    ErrorType.NOT_FOUND,
                    "Ressource non trouvée (404)",
                    "L'élément demandé n'existe pas ou a été supprimé.",
                    false,
                    httpException
                );
                
            case 408:
                return new ErrorInfo(
                    ErrorType.TIMEOUT,
                    "Timeout de requête (408)",
                    "La requête a pris trop de temps. Réessayez.",
                    true,
                    httpException
                );
                
            case 429:
                return new ErrorInfo(
                    ErrorType.RATE_LIMITED,
                    "Trop de requêtes (429)",
                    "Vous faites trop de requêtes. Attendez un moment avant de réessayer.",
                    true,
                    httpException
                );
                
            case 500:
                return new ErrorInfo(
                    ErrorType.SERVER_ERROR,
                    "Erreur serveur interne (500)",
                    "Le serveur rencontre un problème. Réessayez plus tard.",
                    true,
                    httpException
                );
                
            case 502:
                return new ErrorInfo(
                    ErrorType.SERVER_ERROR,
                    "Passerelle défaillante (502)",
                    "Le serveur est temporairement indisponible. Réessayez.",
                    true,
                    httpException
                );
                
            case 503:
                return new ErrorInfo(
                    ErrorType.SERVER_ERROR,
                    "Service indisponible (503)",
                    "Le service est en maintenance. Réessayez plus tard.",
                    true,
                    httpException
                );
                
            case 504:
                return new ErrorInfo(
                    ErrorType.TIMEOUT,
                    "Timeout de passerelle (504)",
                    "Le serveur met trop de temps à répondre. Réessayez.",
                    true,
                    httpException
                );
                
            default:
                if (code >= 400 && code < 500) {
                    return new ErrorInfo(
                        ErrorType.CLIENT_ERROR,
                        String.format("Erreur client (%d): %s", code, message),
                        "Il y a un problème avec votre demande.",
                        false,
                        httpException
                    );
                } else if (code >= 500) {
                    return new ErrorInfo(
                        ErrorType.SERVER_ERROR,
                        String.format("Erreur serveur (%d): %s", code, message),
                        "Le serveur rencontre un problème. Réessayez plus tard.",
                        true,
                        httpException
                    );
                } else {
                    return new ErrorInfo(
                        ErrorType.UNKNOWN,
                        String.format("Code HTTP inattendu (%d): %s", code, message),
                        "Une erreur inattendue s'est produite.",
                        true,
                        httpException
                    );
                }
        }
    }
    
    /**
     * Analyse une IOException (erreur de réseau/IO)
     */
    private ErrorInfo analyzeIOException(IOException ioException) {
        String message = ioException.getMessage();
        
        if (ioException instanceof UnknownHostException) {
            return new ErrorInfo(
                ErrorType.UNKNOWN_HOST,
                "Serveur introuvable",
                "Impossible de joindre le serveur. Vérifiez l'URL et votre connexion.",
                true,
                ioException
            );
        }
        
        if (ioException instanceof SocketTimeoutException) {
            return new ErrorInfo(
                ErrorType.TIMEOUT,
                "Timeout de connexion",
                "La connexion a pris trop de temps. Vérifiez votre réseau.",
                true,
                ioException
            );
        }
        
        if (message != null) {
            message = message.toLowerCase();
            
            if (message.contains("network is unreachable") || 
                message.contains("no route to host")) {
                return new ErrorInfo(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "Réseau indisponible",
                    "Pas de connexion réseau. Vérifiez votre WiFi ou vos données mobiles.",
                    true,
                    ioException
                );
            }
            
            if (message.contains("connection refused")) {
                return new ErrorInfo(
                    ErrorType.SERVER_ERROR,
                    "Connexion refusée",
                    "Le serveur refuse la connexion. Vérifiez l'URL du serveur.",
                    true,
                    ioException
                );
            }
            
            if (message.contains("ssl") || message.contains("certificate")) {
                return new ErrorInfo(
                    ErrorType.CLIENT_ERROR,
                    "Erreur SSL/Certificat",
                    "Problème de sécurité de connexion. Vérifiez la configuration du serveur.",
                    false,
                    ioException
                );
            }
        }
        
        return new ErrorInfo(
            ErrorType.NETWORK_UNAVAILABLE,
            "Erreur réseau: " + ioException.getMessage(),
            "Problème de connexion réseau. Vérifiez votre connexion et réessayez.",
            true,
            ioException
        );
    }
    
    /**
     * Affiche un message d'erreur à l'utilisateur
     */
    public void showErrorToUser(ErrorInfo errorInfo) {
        Toast.makeText(context, errorInfo.userMessage, 
            errorInfo.isRetryable ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }
    
    /**
     * Affiche un message d'erreur personnalisé
     */
    public void showErrorToUser(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Affiche un message d'erreur depuis les ressources
     */
    public void showErrorToUser(@StringRes int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Gère une erreur avec callback optionnel
     */
    public void handleError(Throwable throwable, ErrorCallback callback) {
        ErrorInfo errorInfo = analyzeError(throwable);
        
        if (callback != null) {
            callback.onError(errorInfo);
        } else {
            // Affichage par défaut si pas de callback
            showErrorToUser(errorInfo);
        }
    }
    
    /**
     * Vérifie si une erreur justifie un retry automatique
     */
    public boolean shouldRetry(ErrorInfo errorInfo) {
        return errorInfo.isRetryable && (
            errorInfo.type == ErrorType.TIMEOUT ||
            errorInfo.type == ErrorType.SERVER_ERROR ||
            errorInfo.type == ErrorType.NETWORK_UNAVAILABLE ||
            errorInfo.type == ErrorType.RATE_LIMITED
        );
    }
    
    /**
     * Obtient un délai d'attente approprié avant retry
     */
    public long getRetryDelay(ErrorInfo errorInfo, int attemptNumber) {
        long baseDelay;
        
        switch (errorInfo.type) {
            case RATE_LIMITED:
                baseDelay = 5000; // 5 secondes pour rate limiting
                break;
            case TIMEOUT:
                baseDelay = 2000; // 2 secondes pour timeout
                break;
            case SERVER_ERROR:
                baseDelay = 3000; // 3 secondes pour erreur serveur
                break;
            case NETWORK_UNAVAILABLE:
                baseDelay = 1000; // 1 seconde pour réseau
                break;
            default:
                baseDelay = 1000;
        }
        
        // Exponential backoff avec jitter
        long delay = (long) (baseDelay * Math.pow(2, attemptNumber - 1));
        delay += (long) (delay * 0.1 * Math.random()); // Jitter 10%
        
        return Math.min(delay, 30000); // Max 30 secondes
    }
}
