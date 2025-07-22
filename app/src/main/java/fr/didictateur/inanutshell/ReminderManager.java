package fr.didictateur.inanutshell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class ReminderManager {
    
    private Context context;
    private AlarmManager alarmManager;
    
    public ReminderManager(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    public void setReminder(String recipeTitle, String reminderText, long timeInMillis) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("recipe_title", recipeTitle);
        intent.putExtra("reminder_text", reminderText);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) System.currentTimeMillis(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        }
    }
    
    public void setDailyReminder(String recipeTitle, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // Si l'heure est déjà passée aujourd'hui, programmer pour demain
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("recipe_title", recipeTitle);
        intent.putExtra("reminder_text", "Il est temps de cuisiner !");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            recipeTitle.hashCode(), // ID unique basé sur le titre
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            );
        }
    }
    
    public void cancelReminder(String recipeTitle) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            recipeTitle.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
