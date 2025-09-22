package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire des préférences utilisateur pour les notifications
 */
public class PreferencesManager {
    
    private static final String PREF_NAME = "notification_preferences";
    
    // Clés pour les préférences de notifications
    private static final String KEY_MEAL_REMINDERS_ENABLED = "meal_reminders_enabled";
    private static final String KEY_RECIPE_SUGGESTIONS_ENABLED = "recipe_suggestions_enabled";
    private static final String KEY_COOKING_TIPS_ENABLED = "cooking_tips_enabled";
    private static final String KEY_NEW_RECIPE_NOTIFICATIONS_ENABLED = "new_recipe_notifications_enabled";
    private static final String KEY_TIMER_NOTIFICATIONS_ENABLED = "timer_notifications_enabled";
    
    // Préférences de fréquence et timing
    private static final String KEY_SUGGESTION_FREQUENCY = "suggestion_frequency";
    private static final String KEY_MORNING_REMINDER_HOUR = "morning_reminder_hour";
    private static final String KEY_MORNING_REMINDER_MINUTE = "morning_reminder_minute";
    private static final String KEY_EVENING_REMINDER_HOUR = "evening_reminder_hour";
    private static final String KEY_EVENING_REMINDER_MINUTE = "evening_reminder_minute";
    
    // Préférences avancées
    private static final String KEY_PREFERRED_NOTIFICATION_TIMES = "preferred_notification_times";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
    private static final String KEY_WEEKEND_NOTIFICATIONS = "weekend_notifications";
    
    // Statistiques et personnalisation
    private static final String KEY_FAVORITE_CATEGORIES = "favorite_categories";
    private static final String KEY_NOTIFICATION_HISTORY_DAYS = "notification_history_days";
    private static final String KEY_LAST_SUGGESTION_TIME = "last_suggestion_time";
    private static final String KEY_TOTAL_NOTIFICATIONS_SENT = "total_notifications_sent";
    
    private SharedPreferences sharedPreferences;
    private Context context;
    
    public PreferencesManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // ===============================
    // ACTIVATION/DÉSACTIVATION DES TYPES
    // ===============================
    
    public boolean isMealRemindersEnabled() {
        return sharedPreferences.getBoolean(KEY_MEAL_REMINDERS_ENABLED, true);
    }
    
    public void setMealRemindersEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_MEAL_REMINDERS_ENABLED, enabled).apply();
    }
    
    public boolean isRecipeSuggestionsEnabled() {
        return sharedPreferences.getBoolean(KEY_RECIPE_SUGGESTIONS_ENABLED, true);
    }
    
    public void setRecipeSuggestionsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_RECIPE_SUGGESTIONS_ENABLED, enabled).apply();
    }
    
    public boolean isCookingTipsEnabled() {
        return sharedPreferences.getBoolean(KEY_COOKING_TIPS_ENABLED, true);
    }
    
    public void setCookingTipsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_COOKING_TIPS_ENABLED, enabled).apply();
    }
    
    public boolean isNewRecipeNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NEW_RECIPE_NOTIFICATIONS_ENABLED, true);
    }
    
    public void setNewRecipeNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NEW_RECIPE_NOTIFICATIONS_ENABLED, enabled).apply();
    }
    
    public boolean isTimerNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_TIMER_NOTIFICATIONS_ENABLED, true);
    }
    
    public void setTimerNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_TIMER_NOTIFICATIONS_ENABLED, enabled).apply();
    }
    
    // ===============================
    // FRÉQUENCE ET HORAIRES
    // ===============================
    
    /**
     * Obtient la fréquence de suggestions (en jours)
     * 1 = quotidien, 2 = tous les 2 jours, etc.
     */
    public int getSuggestionFrequency() {
        return sharedPreferences.getInt(KEY_SUGGESTION_FREQUENCY, 1);
    }
    
    public void setSuggestionFrequency(int days) {
        sharedPreferences.edit().putInt(KEY_SUGGESTION_FREQUENCY, days).apply();
    }
    
    /**
     * Horaire des rappels matinaux
     */
    public int getMorningReminderHour() {
        return sharedPreferences.getInt(KEY_MORNING_REMINDER_HOUR, 8);
    }
    
    public int getMorningReminderMinute() {
        return sharedPreferences.getInt(KEY_MORNING_REMINDER_MINUTE, 0);
    }
    
    public void setMorningReminderTime(int hour, int minute) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_MORNING_REMINDER_HOUR, hour);
        editor.putInt(KEY_MORNING_REMINDER_MINUTE, minute);
        editor.apply();
    }
    
    /**
     * Horaire des rappels du soir
     */
    public int getEveningReminderHour() {
        return sharedPreferences.getInt(KEY_EVENING_REMINDER_HOUR, 18);
    }
    
    public int getEveningReminderMinute() {
        return sharedPreferences.getInt(KEY_EVENING_REMINDER_MINUTE, 30);
    }
    
    public void setEveningReminderTime(int hour, int minute) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_EVENING_REMINDER_HOUR, hour);
        editor.putInt(KEY_EVENING_REMINDER_MINUTE, minute);
        editor.apply();
    }
    
    // ===============================
    // MOMENTS PRÉFÉRÉS
    // ===============================
    
    /**
     * Obtient les moments préférés pour les notifications
     * 0=Matin, 1=Midi, 2=Après-midi, 3=Soir
     */
    public List<Integer> getPreferredNotificationTimes() {
        Set<String> timesSet = sharedPreferences.getStringSet(KEY_PREFERRED_NOTIFICATION_TIMES, 
                new HashSet<String>() {{ add("0"); add("3"); }});
        
        List<Integer> times = new ArrayList<>();
        for (String timeStr : timesSet) {
            try {
                times.add(Integer.parseInt(timeStr));
            } catch (NumberFormatException e) {
                // Ignorer les valeurs invalides
            }
        }
        return times;
    }
    
    public void setPreferredNotificationTimes(List<Integer> times) {
        Set<String> timesSet = new HashSet<>();
        for (Integer time : times) {
            timesSet.add(String.valueOf(time));
        }
        sharedPreferences.edit().putStringSet(KEY_PREFERRED_NOTIFICATION_TIMES, timesSet).apply();
    }
    
    // ===============================
    // HEURES SILENCIEUSES
    // ===============================
    
    public boolean isQuietHoursEnabled() {
        return sharedPreferences.getBoolean(KEY_QUIET_HOURS_ENABLED, false);
    }
    
    public void setQuietHoursEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply();
    }
    
    public int getQuietHoursStart() {
        return sharedPreferences.getInt(KEY_QUIET_HOURS_START, 22); // 22h par défaut
    }
    
    public void setQuietHoursStart(int hour) {
        sharedPreferences.edit().putInt(KEY_QUIET_HOURS_START, hour).apply();
    }
    
    public int getQuietHoursEnd() {
        return sharedPreferences.getInt(KEY_QUIET_HOURS_END, 7); // 7h par défaut
    }
    
    public void setQuietHoursEnd(int hour) {
        sharedPreferences.edit().putInt(KEY_QUIET_HOURS_END, hour).apply();
    }
    
    /**
     * Vérifie si nous sommes actuellement en heures silencieuses
     */
    public boolean isInQuietHours() {
        if (!isQuietHoursEnabled()) {
            return false;
        }
        
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        int startHour = getQuietHoursStart();
        int endHour = getQuietHoursEnd();
        
        if (startHour <= endHour) {
            // Même jour (ex: 9h à 17h)
            return currentHour >= startHour && currentHour < endHour;
        } else {
            // Cheval sur deux jours (ex: 22h à 7h)
            return currentHour >= startHour || currentHour < endHour;
        }
    }
    
    // ===============================
    // NOTIFICATIONS WEEKEND
    // ===============================
    
    public boolean areWeekendNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_WEEKEND_NOTIFICATIONS, true);
    }
    
    public void setWeekendNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_WEEKEND_NOTIFICATIONS, enabled).apply();
    }
    
    /**
     * Vérifie si les notifications sont autorisées aujourd'hui
     */
    public boolean areNotificationsAllowedToday() {
        if (areWeekendNotificationsEnabled()) {
            return true;
        }
        
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        
        // Désactivé le weekend (samedi=7, dimanche=1)
        return dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY;
    }
    
    // ===============================
    // CATÉGORIES FAVORITES
    // ===============================
    
    public Set<String> getFavoriteCategories() {
        return sharedPreferences.getStringSet(KEY_FAVORITE_CATEGORIES, new HashSet<>());
    }
    
    public void setFavoriteCategories(Set<String> categories) {
        sharedPreferences.edit().putStringSet(KEY_FAVORITE_CATEGORIES, categories).apply();
    }
    
    public void addFavoriteCategory(String category) {
        Set<String> categories = new HashSet<>(getFavoriteCategories());
        categories.add(category);
        setFavoriteCategories(categories);
    }
    
    public void removeFavoriteCategory(String category) {
        Set<String> categories = new HashSet<>(getFavoriteCategories());
        categories.remove(category);
        setFavoriteCategories(categories);
    }
    
    // ===============================
    // HISTORIQUE ET STATISTIQUES
    // ===============================
    
    public int getNotificationHistoryDays() {
        return sharedPreferences.getInt(KEY_NOTIFICATION_HISTORY_DAYS, 30);
    }
    
    public void setNotificationHistoryDays(int days) {
        sharedPreferences.edit().putInt(KEY_NOTIFICATION_HISTORY_DAYS, days).apply();
    }
    
    public long getLastSuggestionTime() {
        return sharedPreferences.getLong(KEY_LAST_SUGGESTION_TIME, 0);
    }
    
    public void setLastSuggestionTime(long timestamp) {
        sharedPreferences.edit().putLong(KEY_LAST_SUGGESTION_TIME, timestamp).apply();
    }
    
    public int getTotalNotificationsSent() {
        return sharedPreferences.getInt(KEY_TOTAL_NOTIFICATIONS_SENT, 0);
    }
    
    public void incrementNotificationsSent() {
        int current = getTotalNotificationsSent();
        sharedPreferences.edit().putInt(KEY_TOTAL_NOTIFICATIONS_SENT, current + 1).apply();
    }
    
    // ===============================
    // MÉTHODES UTILITAIRES
    // ===============================
    
    /**
     * Vérifie si c'est le moment d'envoyer une suggestion
     */
    public boolean shouldSendSuggestion() {
        if (!isRecipeSuggestionsEnabled() || isInQuietHours() || !areNotificationsAllowedToday()) {
            return false;
        }
        
        long lastSuggestion = getLastSuggestionTime();
        long currentTime = System.currentTimeMillis();
        int frequency = getSuggestionFrequency();
        
        long daysSinceLastSuggestion = (currentTime - lastSuggestion) / (1000 * 60 * 60 * 24);
        
        return daysSinceLastSuggestion >= frequency;
    }
    
    /**
     * Réinitialise tous les paramètres aux valeurs par défaut
     */
    public void resetToDefaults() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * Exporte les préférences sous forme de chaîne JSON (pour sauvegarde/restauration)
     */
    public String exportPreferences() {
        // Implementation simplifiée - dans un vrai projet, utiliser Gson ou similar
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"meal_reminders\":").append(isMealRemindersEnabled()).append(",");
        sb.append("\"recipe_suggestions\":").append(isRecipeSuggestionsEnabled()).append(",");
        sb.append("\"cooking_tips\":").append(isCookingTipsEnabled()).append(",");
        sb.append("\"frequency\":").append(getSuggestionFrequency());
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Obtient un résumé textuel des paramètres actuels
     */
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (isMealRemindersEnabled()) summary.append("Rappels repas ✓ ");
        if (isRecipeSuggestionsEnabled()) summary.append("Suggestions ✓ ");
        if (isCookingTipsEnabled()) summary.append("Conseils ✓ ");
        
        summary.append("\nFréquence: ");
        int freq = getSuggestionFrequency();
        if (freq == 1) {
            summary.append("Quotidienne");
        } else {
            summary.append("Tous les ").append(freq).append(" jours");
        }
        
        return summary.toString();
    }
}
