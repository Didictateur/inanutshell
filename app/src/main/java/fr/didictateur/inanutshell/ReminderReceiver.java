package fr.didictateur.inanutshell;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "recipe_reminders";
    private static final String CHANNEL_NAME = "Rappels de Recettes";
    private static final String CHANNEL_DESCRIPTION = "Notifications pour les rappels de recettes";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String recipeTitle = intent.getStringExtra("recipe_title");
        String reminderText = intent.getStringExtra("reminder_text");
        
        createNotificationChannel(context);
        showNotification(context, recipeTitle, reminderText);
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void showNotification(Context context, String recipeTitle, String reminderText) {
        // Intent pour ouvrir l'app quand on clique sur la notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon)
            .setContentTitle("Rappel de recette")
            .setContentText(recipeTitle + ": " + reminderText)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(recipeTitle + ": " + reminderText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
