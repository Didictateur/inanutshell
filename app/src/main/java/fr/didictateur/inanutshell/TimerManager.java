package fr.didictateur.inanutshell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.CountDownTimer;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;

public class TimerManager {
    private static TimerManager instance;
    private Context context;
    private List<CookingTimer> activeTimers;
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "cooking_timer_channel";
    private static final int BASE_NOTIFICATION_ID = 1000;

    public interface TimerListener {
        void onTimerTick(String timerId, long millisUntilFinished);
        void onTimerFinished(String timerId, String description);
        void onTimerCancelled(String timerId);
    }

    public static class CookingTimer {
        private String id;
        private String description;
        private long totalDuration;
        private long remainingTime;
        private CountDownTimer countDownTimer;
        private TimerListener listener;
        private boolean isPaused;
        private boolean isRunning;

        public CookingTimer(String id, String description, long durationMs, TimerListener listener) {
            this.id = id;
            this.description = description;
            this.totalDuration = durationMs;
            this.remainingTime = durationMs;
            this.listener = listener;
            this.isPaused = false;
            this.isRunning = false;
        }

        public void start() {
            if (!isRunning && !isPaused) {
                isRunning = true;
                createCountDownTimer();
                countDownTimer.start();
            }
        }

        public void pause() {
            if (isRunning && countDownTimer != null) {
                countDownTimer.cancel();
                isPaused = true;
                isRunning = false;
            }
        }

        public void resume() {
            if (isPaused) {
                isPaused = false;
                isRunning = true;
                createCountDownTimer();
                countDownTimer.start();
            }
        }

        public void stop() {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            isRunning = false;
            isPaused = false;
            if (listener != null) {
                listener.onTimerCancelled(id);
            }
        }

        private void createCountDownTimer() {
            countDownTimer = new CountDownTimer(remainingTime, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingTime = millisUntilFinished;
                    if (listener != null) {
                        listener.onTimerTick(id, millisUntilFinished);
                    }
                }

                @Override
                public void onFinish() {
                    isRunning = false;
                    isPaused = false;
                    if (listener != null) {
                        listener.onTimerFinished(id, description);
                    }
                }
            };
        }

        // Getters
        public String getId() { return id; }
        public String getDescription() { return description; }
        public long getTotalDuration() { return totalDuration; }
        public long getRemainingTime() { return remainingTime; }
        public boolean isPaused() { return isPaused; }
        public boolean isRunning() { return isRunning; }
    }

    private TimerManager(Context context) {
        this.context = context.getApplicationContext();
        this.activeTimers = new ArrayList<>();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    public static TimerManager getInstance(Context context) {
        if (instance == null) {
            instance = new TimerManager(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Timer de cuisine",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications pour les timers de cuisine");
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public String createTimer(String description, long durationMs, TimerListener listener) {
        String timerId = "timer_" + System.currentTimeMillis();
        CookingTimer timer = new CookingTimer(timerId, description, durationMs, new TimerListener() {
            @Override
            public void onTimerTick(String id, long millisUntilFinished) {
                updateNotification(id, description, millisUntilFinished);
                if (listener != null) {
                    listener.onTimerTick(id, millisUntilFinished);
                }
            }

            @Override
            public void onTimerFinished(String id, String desc) {
                showTimerFinishedNotification(id, desc);
                removeTimer(id);
                if (listener != null) {
                    listener.onTimerFinished(id, desc);
                }
            }

            @Override
            public void onTimerCancelled(String id) {
                cancelNotification(id);
                removeTimer(id);
                if (listener != null) {
                    listener.onTimerCancelled(id);
                }
            }
        });

        activeTimers.add(timer);
        return timerId;
    }

    public void startTimer(String timerId) {
        CookingTimer timer = findTimer(timerId);
        if (timer != null) {
            timer.start();
        }
    }

    public void pauseTimer(String timerId) {
        CookingTimer timer = findTimer(timerId);
        if (timer != null) {
            timer.pause();
        }
    }

    public void resumeTimer(String timerId) {
        CookingTimer timer = findTimer(timerId);
        if (timer != null) {
            timer.resume();
        }
    }

    public void stopTimer(String timerId) {
        CookingTimer timer = findTimer(timerId);
        if (timer != null) {
            timer.stop();
        }
    }

    private CookingTimer findTimer(String timerId) {
        for (CookingTimer timer : activeTimers) {
            if (timer.getId().equals(timerId)) {
                return timer;
            }
        }
        return null;
    }

    private void removeTimer(String timerId) {
        activeTimers.removeIf(timer -> timer.getId().equals(timerId));
    }

    private void updateNotification(String timerId, String description, long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        Intent intent = new Intent(context, ViewRecetteActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon)
            .setContentTitle("Timer de cuisine")
            .setContentText(description + " - " + timeText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        notificationManager.notify(getNotificationId(timerId), notification);
    }

    private void showTimerFinishedNotification(String timerId, String description) {
        Intent intent = new Intent(context, ViewRecetteActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon)
            .setContentTitle("⏰ Timer terminé !")
            .setContentText(description + " - C'est prêt !")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(new long[]{0, 500, 200, 500})
            .build();

        notificationManager.notify(getNotificationId(timerId), notification);
    }

    private void cancelNotification(String timerId) {
        notificationManager.cancel(getNotificationId(timerId));
    }

    private int getNotificationId(String timerId) {
        return BASE_NOTIFICATION_ID + Math.abs(timerId.hashCode()) % 1000;
    }

    public List<CookingTimer> getActiveTimers() {
        return new ArrayList<>(activeTimers);
    }

    public static String formatTime(long milliseconds) {
        int totalSeconds = (int) (milliseconds / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
