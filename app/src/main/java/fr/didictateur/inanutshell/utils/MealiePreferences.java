package fr.didictateur.inanutshell.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MealiePreferences {
    
    private static final String PREFS_NAME = "mealie_preferences";
    
    // Keys - Synchronized with MealieApplication
    private static final String KEY_SERVER_URL = "mealie_server_url";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TOKEN = "mealie_auth_token";
    private static final String KEY_IS_SETUP_COMPLETE = "is_setup_complete";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LAST_SYNC = "last_sync";
    
    // Theme modes
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    
    private final SharedPreferences prefs;
    
    public MealiePreferences(Context context) {
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    // Server configuration
    public void setMealieCredentials(String serverUrl, String email, String token) {
        prefs.edit()
                .putString(KEY_SERVER_URL, serverUrl)
                .putString(KEY_EMAIL, email)
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_IS_SETUP_COMPLETE, true)
                .apply();
        
        // Also update MealieApplication for consistency
        fr.didictateur.inanutshell.MealieApplication.getInstance().setMealieServerUrl(serverUrl);
        fr.didictateur.inanutshell.MealieApplication.getInstance().setMealieAuthToken(token);
    }
    
    public String getMealieServerUrl() {
        return prefs.getString(KEY_SERVER_URL, "");
    }
    
    public String getMealieEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }
    
    public String getMealieToken() {
        return prefs.getString(KEY_TOKEN, "");
    }
    
    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_IS_SETUP_COMPLETE, false);
    }
    
    public void clearMealieCredentials() {
        prefs.edit()
                .remove(KEY_SERVER_URL)
                .remove(KEY_EMAIL)
                .remove(KEY_TOKEN)
                .putBoolean(KEY_IS_SETUP_COMPLETE, false)
                .apply();
    }
    
    // Theme preferences
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }
    
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }
    
    // Sync preferences
    public void setLastSyncTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
    }
    
    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }
    
    // Utility methods
    public boolean hasValidCredentials() {
        return !getMealieServerUrl().isEmpty() && 
               !getMealieEmail().isEmpty() && 
               !getMealieToken().isEmpty();
    }
    
    public void clearAllData() {
        prefs.edit().clear().apply();
    }
}
