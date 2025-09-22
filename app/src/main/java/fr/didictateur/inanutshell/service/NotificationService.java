package fr.didictateur.inanutshell.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.ui.main.MainActivity;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.dao.NotificationDao;
import fr.didictateur.inanutshell.data.AppDatabase;
import fr.didictateur.inanutshell.data.model.Notification;
import fr.didictateur.inanutshell.receiver.NotificationReceiver;

/**
 * Service principal pour la gestion des notifications
 */
public class NotificationService {
    
    private static final String TAG = "NotificationService";
    
    // Canaux de notifications
    private static final String CHANNEL_MEAL_REMINDERS = "meal_reminders";
    private static final String CHANNEL_RECIPE_SUGGESTIONS = "recipe_suggestions";
    private static final String CHANNEL_TIMERS = "cooking_timers";
    private static final String CHANNEL_GENERAL = "general_notifications";
    
    // IDs de base pour les notifications
    private static final int BASE_MEAL_REMINDER_ID = 1000;
    private static final int BASE_RECIPE_SUGGESTION_ID = 2000;
    private static final int BASE_TIMER_ID = 3000;
    private static final int BASE_GENERAL_ID = 4000;
    
    private Context context;
    private NotificationDao notificationDao;
    private ExecutorService executorService;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationDao = AppDatabase.getInstance(context).notificationDao();
        this.executorService = Executors.newCachedThreadPool();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        createNotificationChannels();
    }
    
    /**
     * Crée les canaux de notifications pour Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            // Canal pour les rappels de repas
            NotificationChannel mealChannel = new NotificationChannel(
                    CHANNEL_MEAL_REMINDERS,
                    context.getString(R.string.channel_meal_reminders),
                    NotificationManager.IMPORTANCE_HIGH
            );
            mealChannel.setDescription(context.getString(R.string.channel_meal_reminders_desc));
            mealChannel.enableLights(true);
            mealChannel.setLightColor(Color.GREEN);
            mealChannel.enableVibration(true);
            mealChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            
            // Canal pour les suggestions de recettes
            NotificationChannel suggestionChannel = new NotificationChannel(
                    CHANNEL_RECIPE_SUGGESTIONS,
                    context.getString(R.string.channel_recipe_suggestions),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            suggestionChannel.setDescription(context.getString(R.string.channel_recipe_suggestions_desc));
            suggestionChannel.enableLights(true);
            suggestionChannel.setLightColor(Color.BLUE);
            
            // Canal pour les minuteries
            NotificationChannel timerChannel = new NotificationChannel(
                    CHANNEL_TIMERS,
                    context.getString(R.string.channel_cooking_timers),
                    NotificationManager.IMPORTANCE_HIGH
            );
            timerChannel.setDescription(context.getString(R.string.channel_cooking_timers_desc));
            timerChannel.enableLights(true);
            timerChannel.setLightColor(Color.RED);
            timerChannel.enableVibration(true);
            timerChannel.setVibrationPattern(new long[]{0, 500, 100, 500, 100, 500});
            
            // Canal général
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    context.getString(R.string.channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription(context.getString(R.string.channel_general_desc));
            
            notificationManager.createNotificationChannel(mealChannel);
            notificationManager.createNotificationChannel(suggestionChannel);
            notificationManager.createNotificationChannel(timerChannel);
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    /**
     * Crée une notification immédiate
     */
    public void showNotification(Notification notification) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Permission de notification non accordée");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Sauvegarder en base
                long id = notificationDao.insertNotification(notification);
                notification.setNotificationId((int) id);
                
                // Créer la notification système
                android.app.Notification systemNotification = buildSystemNotification(notification);
                int notificationId = getNotificationId(notification);
                try {
                    NotificationManagerCompat.from(context).notify(notificationId, systemNotification);
                } catch (SecurityException se) {
                    Log.e(TAG, "Permission de notification refusée lors de l'affichage", se);
                }
                Log.d(TAG, "Notification affichée: " + notification.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'affichage de la notification", e);
            }
        });
    }
    
    /**
     * Programme une notification pour plus tard
     */
    public void scheduleNotification(Notification notification, Date scheduledTime) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Permission de notification non accordée");
            return;
        }
        
        executorService.execute(() -> {
            try {
                notification.setScheduled(true);
                notification.setScheduledTime(scheduledTime);
                
                // Sauvegarder en base
                long id = notificationDao.insertNotification(notification);
                notification.setNotificationId((int) id);
                
                // Programmer l'alarme
                scheduleAlarm(notification);
                
                Log.d(TAG, "Notification programmée pour: " + scheduledTime);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la programmation de notification", e);
            }
        });
    }
    
    /**
     * Programme une alarme pour déclencher la notification
     */
    private void scheduleAlarm(Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notification.getNotificationId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        long triggerTime = notification.getScheduledTime().getTime();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }
    
    /**
     * Construit la notification système Android
     */
    private android.app.Notification buildSystemNotification(Notification notification) {
        String channelId = getChannelId(notification.getType());
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notification.getNotificationId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getMessage())
                .setSmallIcon(getNotificationIcon(notification.getType()))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(getPriority(notification.getPriority()));
        
        // Style étendu pour les longs messages
        if (notification.getMessage().length() > 50) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(notification.getMessage()));
        }
        
        // Couleur selon le type
        builder.setColor(getNotificationColor(notification.getType()));
        
        // Actions selon le type
        addNotificationActions(builder, notification);
        
        return builder.build();
    }
    
    /**
     * Ajoute des actions à la notification selon son type
     */
    private void addNotificationActions(NotificationCompat.Builder builder, Notification notification) {
        switch (notification.getType()) {
            case MEAL_REMINDER:
                // Action "Vu" et "Plus tard"
                addMarkAsReadAction(builder, notification);
                addSnoozeAction(builder, notification);
                break;
                
            case RECIPE_SUGGESTION:
                // Action "Voir recette"
                addViewRecipeAction(builder, notification);
                break;
                
            case TIMER_ALERT:
                // Action "Arrêter" et "Répéter"
                addStopTimerAction(builder, notification);
                addRepeatTimerAction(builder, notification);
                break;
                
            default:
                // Action basique "Marquer comme lu"
                addMarkAsReadAction(builder, notification);
                break;
        }
    }
    
    private void addMarkAsReadAction(NotificationCompat.Builder builder, Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("MARK_AS_READ");
        intent.putExtra("notification_id", notification.getNotificationId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId() + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_check, "Vu", pendingIntent);
    }
    
    private void addSnoozeAction(NotificationCompat.Builder builder, Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("SNOOZE");
        intent.putExtra("notification_id", notification.getNotificationId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId() + 20000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_snooze, "Plus tard", pendingIntent);
    }
    
    private void addViewRecipeAction(NotificationCompat.Builder builder, Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("VIEW_RECIPE");
        intent.putExtra("notification_id", notification.getNotificationId());
        intent.putExtra("action_data", notification.getActionData());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId() + 30000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_recipe, "Voir recette", pendingIntent);
    }
    
    private void addStopTimerAction(NotificationCompat.Builder builder, Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("STOP_TIMER");
        intent.putExtra("notification_id", notification.getNotificationId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId() + 40000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_stop, "Arrêter", pendingIntent);
    }
    
    private void addRepeatTimerAction(NotificationCompat.Builder builder, Notification notification) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("REPEAT_TIMER");
        intent.putExtra("notification_id", notification.getNotificationId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.getNotificationId() + 50000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.ic_repeat, "Répéter", pendingIntent);
    }
    
    /**
     * Retourne l'ID du canal selon le type de notification
     */
    private String getChannelId(Notification.NotificationType type) {
        switch (type) {
            case MEAL_REMINDER:
            case MEAL_PLAN_REMINDER:
                return CHANNEL_MEAL_REMINDERS;
            case RECIPE_SUGGESTION:
            case NEW_RECIPE:
            case COOKING_TIP:
                return CHANNEL_RECIPE_SUGGESTIONS;
            case TIMER_ALERT:
                return CHANNEL_TIMERS;
            default:
                return CHANNEL_GENERAL;
        }
    }
    
    /**
     * Retourne l'icône selon le type de notification
     */
    private int getNotificationIcon(Notification.NotificationType type) {
        switch (type) {
            case MEAL_REMINDER:
            case MEAL_PLAN_REMINDER:
                return R.drawable.ic_restaurant;
            case RECIPE_SUGGESTION:
            case NEW_RECIPE:
                return R.drawable.ic_recipe;
            case TIMER_ALERT:
                return R.drawable.ic_timer;
            case COOKING_TIP:
                return R.drawable.ic_lightbulb;
            case SHOPPING_REMINDER:
                return R.drawable.ic_shopping_cart;
            default:
                return R.drawable.ic_notification;
        }
    }
    
    /**
     * Retourne la couleur selon le type de notification
     */
    private int getNotificationColor(Notification.NotificationType type) {
        switch (type) {
            case MEAL_REMINDER:
            case MEAL_PLAN_REMINDER:
                return ContextCompat.getColor(context, R.color.primary);
            case RECIPE_SUGGESTION:
            case NEW_RECIPE:
                return ContextCompat.getColor(context, R.color.secondary);
            case TIMER_ALERT:
                return Color.RED;
            case COOKING_TIP:
                return Color.BLUE;
            default:
                return ContextCompat.getColor(context, R.color.primary);
        }
    }
    
    /**
     * Convertit la priorité de notification
     */
    private int getPriority(Notification.NotificationPriority priority) {
        switch (priority) {
            case LOW:
                return NotificationCompat.PRIORITY_LOW;
            case HIGH:
                return NotificationCompat.PRIORITY_HIGH;
            case URGENT:
                return NotificationCompat.PRIORITY_MAX;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }
    
    /**
     * Génère un ID unique pour la notification
     */
    private int getNotificationId(Notification notification) {
        int baseId = getBaseId(notification.getType());
        return baseId + (notification.getNotificationId() % 1000);
    }
    
    private int getBaseId(Notification.NotificationType type) {
        switch (type) {
            case MEAL_REMINDER:
            case MEAL_PLAN_REMINDER:
                return BASE_MEAL_REMINDER_ID;
            case RECIPE_SUGGESTION:
            case NEW_RECIPE:
                return BASE_RECIPE_SUGGESTION_ID;
            case TIMER_ALERT:
                return BASE_TIMER_ID;
            default:
                return BASE_GENERAL_ID;
        }
    }
    
    /**
     * Vérifie si l'application a la permission d'afficher des notifications
     */
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, 
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
    
    // Méthodes publiques pour la gestion des notifications
    
    /**
     * Marque une notification comme lue
     */
    public void markNotificationAsRead(int notificationId) {
        executorService.execute(() -> {
            notificationDao.markAsRead(notificationId, true);
            // Annuler la notification système
            notificationManager.cancel(notificationId);
        });
    }
    
    /**
     * Supprime une notification
     */
    public void deleteNotification(int notificationId) {
        executorService.execute(() -> {
            notificationDao.deleteNotificationById(notificationId);
            notificationManager.cancel(notificationId);
            cancelScheduledAlarm(notificationId);
        });
    }
    
    /**
     * Annule une alarme programmée
     */
    private void cancelScheduledAlarm(int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
    }
    
    /**
     * Reporte une notification (snooze)
     */
    public void snoozeNotification(int notificationId, int minutesToSnooze) {
        executorService.execute(() -> {
            Notification notification = notificationDao.getNotificationById(notificationId);
            if (notification != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, minutesToSnooze);
                
                notification.setScheduledTime(calendar.getTime());
                notificationDao.updateNotification(notification);
                
                scheduleAlarm(notification);
                notificationManager.cancel(getNotificationId(notification));
            }
        });
    }
    
    /**
     * Nettoie les anciennes notifications
     */
    public void cleanupOldNotifications() {
        executorService.execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -7); // Supprimer celles de plus de 7 jours
            Date cutoffDate = calendar.getTime();
            
            notificationDao.deleteOldReadNotifications(cutoffDate);
        });
    }
    
    /**
     * Obtient le nombre de notifications non lues
     */
    public void getUnreadCount(UnreadCountCallback callback) {
        executorService.execute(() -> {
            int count = notificationDao.getUnreadCount();
            callback.onResult(count);
        });
    }
    
    public interface UnreadCountCallback {
        void onResult(int count);
    }
    
    /**
     * Ferme le service proprement
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
