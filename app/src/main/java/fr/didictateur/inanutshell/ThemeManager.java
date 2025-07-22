package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ThemeManager {
    
    private Context context;
    private SharedPreferences preferences;
    
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_AUTO = "auto";
    
    public static final String ACCENT_RED = "red";
    public static final String ACCENT_BLUE = "blue";
    public static final String ACCENT_GREEN = "green";
    public static final String ACCENT_PURPLE = "purple";
    public static final String ACCENT_ORANGE = "orange";
    
    private static final String PREF_THEME = "app_theme";
    private static final String PREF_ACCENT = "accent_color";
    private static final String PREF_ANIMATION = "enable_animations";
    private static final String PREF_HIGH_CONTRAST = "high_contrast";
    
    public ThemeManager(Context context) {
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public void setTheme(String theme) {
        preferences.edit().putString(PREF_THEME, theme).apply();
    }
    
    public String getTheme() {
        return preferences.getString(PREF_THEME, THEME_AUTO);
    }
    
    public void setAccentColor(String color) {
        preferences.edit().putString(PREF_ACCENT, color).apply();
    }
    
    public String getAccentColor() {
        return preferences.getString(PREF_ACCENT, ACCENT_BLUE);
    }
    
    public void setAnimationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(PREF_ANIMATION, enabled).apply();
    }
    
    public boolean areAnimationsEnabled() {
        return preferences.getBoolean(PREF_ANIMATION, true);
    }
    
    public void setHighContrast(boolean enabled) {
        preferences.edit().putBoolean(PREF_HIGH_CONTRAST, enabled).apply();
    }
    
    public boolean isHighContrast() {
        return preferences.getBoolean(PREF_HIGH_CONTRAST, false);
    }
    
    public int getPrimaryColor() {
        String accent = getAccentColor();
        switch (accent) {
            case ACCENT_RED:
                return R.color.primary_red;
            case ACCENT_GREEN:
                return R.color.primary_green;
            case ACCENT_PURPLE:
                return R.color.primary_purple;
            case ACCENT_ORANGE:
                return R.color.primary_orange;
            case ACCENT_BLUE:
            default:
                return R.color.primary_blue;
        }
    }
    
    public int getSecondaryColor() {
        String accent = getAccentColor();
        switch (accent) {
            case ACCENT_RED:
                return R.color.secondary_red;
            case ACCENT_GREEN:
                return R.color.secondary_green;
            case ACCENT_PURPLE:
                return R.color.secondary_purple;
            case ACCENT_ORANGE:
                return R.color.secondary_orange;
            case ACCENT_BLUE:
            default:
                return R.color.secondary_blue;
        }
    }
    
    public boolean isDarkTheme() {
        String theme = getTheme();
        if (theme.equals(THEME_DARK)) {
            return true;
        } else if (theme.equals(THEME_AUTO)) {
            // Vérifier le thème système (Android 10+)
            int nightModeFlags = context.getResources().getConfiguration().uiMode 
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return false;
    }
    
    public int getBackgroundColor() {
        if (isDarkTheme()) {
            return isHighContrast() ? R.color.background_dark_high_contrast : R.color.background_dark;
        } else {
            return isHighContrast() ? R.color.background_light_high_contrast : R.color.background_light;
        }
    }
    
    public int getTextColor() {
        if (isDarkTheme()) {
            return isHighContrast() ? R.color.text_dark_high_contrast : R.color.text_dark;
        } else {
            return isHighContrast() ? R.color.text_light_high_contrast : R.color.text_light;
        }
    }
}
