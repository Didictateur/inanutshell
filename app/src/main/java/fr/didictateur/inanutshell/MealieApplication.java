package fr.didictateur.inanutshell;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;

public class MealieApplication extends Application {
    
    private static MealieApplication instance;
    private SharedPreferences sharedPreferences;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialiser les préférences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Appliquer le mode sombre
        applyThemePreferences();
    }
    
    public static MealieApplication getInstance() {
        return instance;
    }
    
    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
    
    private void applyThemePreferences() {
        String themeMode = sharedPreferences.getString("theme_mode", "system");
        
        int nightMode;
        switch (themeMode) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
    
    // Méthodes utilitaires pour les préférences Mealie
    public String getMealieServerUrl() {
        return sharedPreferences.getString("mealie_server_url", "");
    }
    
    public void setMealieServerUrl(String url) {
        sharedPreferences.edit().putString("mealie_server_url", url).apply();
    }
    
    public String getMealieAuthToken() {
        return sharedPreferences.getString("mealie_auth_token", "");
    }
    
    public void setMealieAuthToken(String token) {
        sharedPreferences.edit().putString("mealie_auth_token", token).apply();
    }
    
    public boolean isConfigured() {
        return !getMealieServerUrl().isEmpty() && !getMealieAuthToken().isEmpty();
    }
    
    public void clearConfiguration() {
        sharedPreferences.edit()
            .remove("mealie_server_url")
            .remove("mealie_auth_token")
            .apply();
    }
}
