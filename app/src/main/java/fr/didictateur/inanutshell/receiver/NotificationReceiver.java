package fr.didictateur.inanutshell.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// import fr.didictateur.inanutshell.ViewRecetteActivity; // TODO: À décommenter quand l'activité sera implémentée
import fr.didictateur.inanutshell.data.dao.NotificationDao;
import fr.didictateur.inanutshell.data.AppDatabase;
import fr.didictateur.inanutshell.data.model.Notification;
import fr.didictateur.inanutshell.service.NotificationService;

/**
 * Récepteur pour gérer les actions sur les notifications
 */
public class NotificationReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NotificationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notification_id", -1);
        
        if (notificationId == -1) {
            Log.e(TAG, "ID de notification invalide");
            return;
        }
        
        Log.d(TAG, "Action reçue: " + action + " pour notification: " + notificationId);
        
        NotificationService notificationService = new NotificationService(context);
        
        if (action == null) {
            // Notification programmée qui doit être affichée
            showScheduledNotification(context, notificationId);
        } else {
            switch (action) {
                case "MARK_AS_READ":
                    handleMarkAsRead(notificationService, notificationId);
                    break;
                    
                case "SNOOZE":
                    handleSnooze(notificationService, notificationId);
                    break;
                    
                case "VIEW_RECIPE":
                    handleViewRecipe(context, intent, notificationService, notificationId);
                    break;
                    
                case "STOP_TIMER":
                    handleStopTimer(context, notificationService, notificationId);
                    break;
                    
                case "REPEAT_TIMER":
                    handleRepeatTimer(context, notificationService, notificationId);
                    break;
                    
                default:
                    Log.w(TAG, "Action non reconnue: " + action);
                    break;
            }
        }
    }
    
    /**
     * Affiche une notification programmée
     */
    private void showScheduledNotification(Context context, int notificationId) {
        try {
            NotificationDao notificationDao = AppDatabase.getInstance(context).notificationDao();
            Notification notification = notificationDao.getNotificationById(notificationId);
            
            if (notification != null) {
                NotificationService notificationService = new NotificationService(context);
                
                // Marquer comme non programmée maintenant qu'elle est affichée
                notification.setScheduled(false);
                notificationDao.updateNotification(notification);
                
                // Afficher la notification
                notificationService.showNotification(notification);
                
                Log.d(TAG, "Notification programmée affichée: " + notification.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage de la notification programmée", e);
        }
    }
    
    /**
     * Marque la notification comme lue
     */
    private void handleMarkAsRead(NotificationService notificationService, int notificationId) {
        notificationService.markNotificationAsRead(notificationId);
        Log.d(TAG, "Notification marquée comme lue: " + notificationId);
    }
    
    /**
     * Reporte la notification (snooze)
     */
    private void handleSnooze(NotificationService notificationService, int notificationId) {
        // Reporter de 15 minutes par défaut
        notificationService.snoozeNotification(notificationId, 15);
        Log.d(TAG, "Notification reportée: " + notificationId);
    }
    
    /**
     * Ouvre la recette associée
     */
    private void handleViewRecipe(Context context, Intent originalIntent, 
                                NotificationService notificationService, int notificationId) {
        try {
            String actionData = originalIntent.getStringExtra("action_data");
            
            if (actionData != null && !actionData.isEmpty()) {
                // Extraire l'ID de la recette depuis actionData (format JSON)
                int recetteId = parseRecipeIdFromActionData(actionData);
                
                if (recetteId > 0) {
                    // TODO: Implémenter l'ouverture de recette quand ViewRecetteActivity sera prête
                    /*
                    Intent viewRecipeIntent = new Intent(context, ViewRecetteActivity.class);
                    viewRecipeIntent.putExtra("recette_id", recetteId);
                    viewRecipeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(viewRecipeIntent);
                    */
                    
                    // Marquer la notification comme lue
                    notificationService.markNotificationAsRead(notificationId);
                    
                    Log.d(TAG, "Recette trouvée: " + recetteId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture de la recette", e);
        }
    }
    
    /**
     * Arrête le minuteur
     */
    private void handleStopTimer(Context context, NotificationService notificationService, int notificationId) {
        try {
            // Arrêter le son/vibration du minuteur
            Intent stopTimerIntent = new Intent("STOP_COOKING_TIMER");
            stopTimerIntent.putExtra("notification_id", notificationId);
            context.sendBroadcast(stopTimerIntent);
            
            // Marquer la notification comme lue
            notificationService.markNotificationAsRead(notificationId);
            
            Log.d(TAG, "Minuteur arrêté: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'arrêt du minuteur", e);
        }
    }
    
    /**
     * Répète le minuteur
     */
    private void handleRepeatTimer(Context context, NotificationService notificationService, int notificationId) {
        try {
            // Relancer le minuteur avec la même durée
            Intent repeatTimerIntent = new Intent("REPEAT_COOKING_TIMER");
            repeatTimerIntent.putExtra("notification_id", notificationId);
            context.sendBroadcast(repeatTimerIntent);
            
            // Marquer la notification actuelle comme lue
            notificationService.markNotificationAsRead(notificationId);
            
            Log.d(TAG, "Minuteur relancé: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la répétition du minuteur", e);
        }
    }
    
    /**
     * Parse l'ID de recette depuis les données d'action JSON
     */
    private int parseRecipeIdFromActionData(String actionData) {
        try {
            // Format attendu: {"type":"recipe","id":123} ou simplement "123"
            if (actionData.startsWith("{")) {
                // Format JSON
                String[] parts = actionData.split("\"id\":");
                if (parts.length > 1) {
                    String idPart = parts[1].split("}")[0].trim();
                    return Integer.parseInt(idPart);
                }
            } else {
                // Format simple
                return Integer.parseInt(actionData.trim());
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Impossible de parser l'ID de recette: " + actionData, e);
        }
        return 0;
    }
}
