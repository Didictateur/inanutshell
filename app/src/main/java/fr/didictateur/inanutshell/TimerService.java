package fr.didictateur.inanutshell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Android pour gérer les minuteries en arrière-plan
 */
public class TimerService extends Service {
    
    private static final String CHANNEL_ID = "TimerChannel";
    private static final String CHANNEL_NAME = "Minuteries de Cuisson";
    private static final int NOTIFICATION_ID_BASE = 10000;
    
    private TimerManager timerManager;
    private Handler handler;
    private final Map<Integer, Runnable> runningTimers = new ConcurrentHashMap<>();
    private NotificationManager notificationManager;
    
    // Actions du service
    public static final String ACTION_START_TIMER = "START_TIMER";
    public static final String ACTION_PAUSE_TIMER = "PAUSE_TIMER";
    public static final String ACTION_CANCEL_TIMER = "CANCEL_TIMER";
    public static final String ACTION_UPDATE_TIMERS = "UPDATE_TIMERS";
    
    // Extras
    public static final String EXTRA_TIMER_ID = "timer_id";
    public static final String EXTRA_TIMER_NAME = "timer_name";
    public static final String EXTRA_TIMER_DURATION = "timer_duration";
    
    @Override
    public void onCreate() {
        super.onCreate();
        timerManager = new TimerManager(this);
        handler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannel();
        loadRunningTimers();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START_TIMER:
                    int timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
                    if (timerId != -1) {
                        startTimerUpdates(timerId);
                    }
                    break;
                    
                case ACTION_PAUSE_TIMER:
                    int pauseTimerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
                    if (pauseTimerId != -1) {
                        stopTimerUpdates(pauseTimerId);
                    }
                    break;
                    
                case ACTION_CANCEL_TIMER:
                    int cancelTimerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
                    if (cancelTimerId != -1) {
                        cancelTimer(cancelTimerId);
                    }
                    break;
                    
                case ACTION_UPDATE_TIMERS:
                    updateAllTimers();
                    break;
            }
        }
        
        return START_STICKY; // Le service redémarre si tué par le système
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service non lié
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Arrêter tous les timers en cours
        for (Runnable runnable : runningTimers.values()) {
            handler.removeCallbacks(runnable);
        }
        runningTimers.clear();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications pour les minuteries de cuisson");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void loadRunningTimers() {
        timerManager.getAllRunningTimers(new TimerManager.TimersCallback() {
            @Override
            public void onSuccess(List<Timer> timers) {
                for (Timer timer : timers) {
                    if (timer.isRunning()) {
                        startTimerUpdates(timer.id);
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                // Log error mais ne pas interrompre le service
            }
        });
    }
    
    private void startTimerUpdates(int timerId) {
        // Arrêter les anciennes mises à jour pour ce timer s'il y en a
        stopTimerUpdates(timerId);
        
        TimerUpdateRunnable timerRunnable = new TimerUpdateRunnable(timerId);
        runningTimers.put(timerId, timerRunnable);
        handler.post(timerRunnable);
    }
    
    private class TimerUpdateRunnable implements Runnable {
        private final int timerId;
        
        public TimerUpdateRunnable(int timerId) {
            this.timerId = timerId;
        }
        
        @Override
        public void run() {
            timerManager.getTimer(timerId, new TimerManager.TimerCallback() {
                @Override
                public void onSuccess(Timer timer) {
                    if (timer.isRunning()) {
                        timer.updateRemainingTime();
                        
                        if (timer.remainingTimeMs <= 0) {
                            // Timer terminé !
                            timer.finish();
                            timerManager.updateTimer(timer, new TimerManager.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    showTimerFinishedNotification(timer);
                                    stopTimerUpdates(timerId);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    // Log error
                                }
                            });
                        } else {
                            // Mettre à jour la notification de progression
                            updateTimerNotification(timer);
                            // Programmer la prochaine mise à jour dans 1 seconde
                            handler.postDelayed(TimerUpdateRunnable.this, 1000);
                        }
                    } else {
                        // Timer arrêté ou en pause
                        stopTimerUpdates(timerId);
                    }
                }
                
                @Override
                public void onError(String error) {
                    // Timer n'existe plus ou erreur
                    stopTimerUpdates(timerId);
                }
            });
        }
    }
    
    private void stopTimerUpdates(int timerId) {
        Runnable runnable = runningTimers.remove(timerId);
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // Supprimer la notification de progression
        notificationManager.cancel(NOTIFICATION_ID_BASE + timerId);
        
        // Arrêter le service si plus de timers actifs
        if (runningTimers.isEmpty()) {
            stopSelf();
        }
    }
    
    private void cancelTimer(int timerId) {
        timerManager.cancelTimer(timerId, new TimerManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                stopTimerUpdates(timerId);
            }
            
            @Override
            public void onError(String error) {
                // Log error
            }
        });
    }
    
    private void updateAllTimers() {
        timerManager.getAllRunningTimers(new TimerManager.TimersCallback() {
            @Override
            public void onSuccess(List<Timer> timers) {
                // Synchroniser les timers en cours
                Map<Integer, Timer> activeTimers = new HashMap<>();
                for (Timer timer : timers) {
                    activeTimers.put(timer.id, timer);
                }
                
                // Arrêter les timers qui ne sont plus actifs
                for (Integer timerId : runningTimers.keySet()) {
                    if (!activeTimers.containsKey(timerId)) {
                        stopTimerUpdates(timerId);
                    }
                }
                
                // Démarrer les nouveaux timers
                for (Timer timer : timers) {
                    if (timer.isRunning() && !runningTimers.containsKey(timer.id)) {
                        startTimerUpdates(timer.id);
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                // Log error
            }
        });
    }
    
    private void updateTimerNotification(Timer timer) {
        Intent notificationIntent = new Intent(this, TimersActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Actions dans la notification
        Intent pauseIntent = new Intent(this, TimerService.class);
        pauseIntent.setAction(ACTION_PAUSE_TIMER);
        pauseIntent.putExtra(EXTRA_TIMER_ID, timer.id);
        PendingIntent pausePendingIntent = PendingIntent.getService(
            this, timer.id * 100 + 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent cancelIntent = new Intent(this, TimerService.class);
        cancelIntent.setAction(ACTION_CANCEL_TIMER);
        cancelIntent.putExtra(EXTRA_TIMER_ID, timer.id);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
            this, timer.id * 100 + 2, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(timer.name)
            .setContentText("Temps restant: " + timer.getFormattedTime())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setProgress((int) timer.originalDurationMs, 
                       (int) (timer.originalDurationMs - timer.remainingTimeMs), false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Annuler", cancelPendingIntent)
            .setAutoCancel(false)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID_BASE + timer.id, notification);
    }
    
    private void showTimerFinishedNotification(Timer timer) {
        Intent notificationIntent = new Intent(this, TimersActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Minuterie terminée !")
            .setContentText(timer.name + " - " + timer.getFormattedOriginalDuration())
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID_BASE + timer.id + 1000, notification);
    }
    
    // Méthodes utilitaires statiques pour contrôler le service
    public static void startTimer(Context context, int timerId) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_START_TIMER);
        intent.putExtra(EXTRA_TIMER_ID, timerId);
        context.startService(intent);
    }
    
    public static void pauseTimer(Context context, int timerId) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_PAUSE_TIMER);
        intent.putExtra(EXTRA_TIMER_ID, timerId);
        context.startService(intent);
    }
    
    public static void cancelTimer(Context context, int timerId) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_CANCEL_TIMER);
        intent.putExtra(EXTRA_TIMER_ID, timerId);
        context.startService(intent);
    }
    
    public static void updateTimers(Context context) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_UPDATE_TIMERS);
        context.startService(intent);
    }
}
