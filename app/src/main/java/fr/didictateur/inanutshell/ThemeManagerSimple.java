package fr.didictateur.inanutshell;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire simplifié pour les thèmes personnalisés
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_ACTIVE_THEME_ID = "active_theme_id";

    private static ThemeManager instance;
    private final Context context;
    private final ThemeDao themeDao;
    private final SharedPreferences prefs;
    private final ExecutorService executor;

    // État actuel
    private Theme currentTheme;
    private final MutableLiveData<Theme> currentThemeLiveData = new MutableLiveData<>();

    /**
     * Interface pour les écouteurs de changement de thème
     */
    public interface OnThemeChangedListener {
        void onThemeChanged(Theme newTheme);
        void onThemeApplying();
        void onThemeApplied();
    }

    // ===================== Construction et initialisation =====================

    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.themeDao = AppDatabase.getInstance(context).themeDao();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newFixedThreadPool(2);
        
        initializeDefaultThemes();
        loadActiveTheme();
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    // ===================== Gestion des thèmes =====================

    /**
     * Obtient le thème actuellement actif
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Obtient un LiveData du thème actuel
     */
    public LiveData<Theme> getCurrentThemeLive() {
        return currentThemeLiveData;
    }

    /**
     * Applique un nouveau thème
     */
    public void applyTheme(int themeId) {
        executor.execute(() -> {
            try {
                Theme theme = themeDao.getThemeById(themeId);
                if (theme != null) {
                    // Désactiver tous les autres thèmes
                    themeDao.deactivateAllThemes();
                    
                    // Activer le nouveau thème
                    themeDao.activateTheme(themeId);
                    theme.setActive(true);
                    
                    // Sauvegarder la préférence
                    prefs.edit().putInt(KEY_ACTIVE_THEME_ID, themeId).apply();
                    
                    // Mettre à jour le thème actuel
                    currentTheme = theme;
                    
                    // Notifier les changements sur le thread principal
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        currentThemeLiveData.setValue(theme);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'application du thème", e);
            }
        });
    }

    /**
     * Crée un nouveau thème personnalisé
     */
    public void createCustomTheme(String name, String description, int userId) {
        executor.execute(() -> {
            Theme customTheme = new Theme(name, Theme.ThemeType.CUSTOM, userId);
            customTheme.setDescription(description);
            customTheme.setActive(false);
            customTheme.setDefault(false);
            
            long themeId = themeDao.insert(customTheme);
            customTheme.setThemeId((int) themeId);
        });
    }

    /**
     * Met à jour un thème existant
     */
    public void updateTheme(Theme theme) {
        executor.execute(() -> {
            theme.setLastModified(System.currentTimeMillis());
            themeDao.update(theme);
            
            // Si c'est le thème actuel, notifier le changement
            if (currentTheme != null && currentTheme.getThemeId() == theme.getThemeId()) {
                currentTheme = theme;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    currentThemeLiveData.setValue(theme);
                });
            }
        });
    }

    /**
     * Supprime un thème
     */
    public void deleteTheme(int themeId) {
        executor.execute(() -> {
            Theme theme = themeDao.getThemeById(themeId);
            if (theme != null && !theme.isDefault()) {
                // Si c'est le thème actuel, basculer vers le thème par défaut
                if (currentTheme != null && currentTheme.getThemeId() == themeId) {
                    List<Theme> defaultThemes = themeDao.getDefaultThemes();
                    if (!defaultThemes.isEmpty()) {
                        applyTheme(defaultThemes.get(0).getThemeId());
                    }
                }
                
                themeDao.delete(theme);
            }
        });
    }

    // ===================== Gestion des couleurs =====================

    /**
     * Met à jour la couleur primaire du thème actuel
     */
    public void updateCurrentThemePrimaryColor(String color) {
        if (currentTheme != null) {
            currentTheme.setPrimaryColor(color);
            updateTheme(currentTheme);
        }
    }

    /**
     * Met à jour la couleur secondaire du thème actuel
     */
    public void updateCurrentThemeSecondaryColor(String color) {
        if (currentTheme != null) {
            currentTheme.setSecondaryColor(color);
            updateTheme(currentTheme);
        }
    }

    /**
     * Met à jour la couleur de fond du thème actuel
     */
    public void updateCurrentThemeBackgroundColor(String color) {
        if (currentTheme != null) {
            currentTheme.setBackgroundColor(color);
            updateTheme(currentTheme);
        }
    }

    // ===================== Gestion de l'accessibilité =====================

    /**
     * Met à jour l'échelle de texte
     */
    public void updateTextScale(float scale) {
        if (currentTheme != null) {
            currentTheme.setTextScale(scale);
            updateTheme(currentTheme);
        }
    }

    /**
     * Active ou désactive le contraste élevé
     */
    public void setHighContrast(boolean enabled) {
        if (currentTheme != null) {
            currentTheme.setHighContrast(enabled);
            updateTheme(currentTheme);
        }
    }

    /**
     * Active ou désactive le texte en gras
     */
    public void setBoldText(boolean enabled) {
        if (currentTheme != null) {
            currentTheme.setBoldText(enabled);
            updateTheme(currentTheme);
        }
    }

    /**
     * Active ou désactive la réduction des animations
     */
    public void setReducedMotion(boolean enabled) {
        if (currentTheme != null) {
            currentTheme.setReducedMotion(enabled);
            updateTheme(currentTheme);
        }
    }

    // ===================== Utilitaires =====================

    /**
     * Obtient tous les thèmes disponibles
     */
    public LiveData<List<Theme>> getAllThemes() {
        return themeDao.getAllThemesLive();
    }

    /**
     * Obtient le thème par défaut
     */
    public Theme getDefaultTheme() {
        List<Theme> defaultThemes = themeDao.getDefaultThemes();
        return defaultThemes.isEmpty() ? null : defaultThemes.get(0);
    }

    // ===================== Méthodes privées =====================

    private void loadActiveTheme() {
        executor.execute(() -> {
            Theme activeTheme = themeDao.getActiveTheme();
            if (activeTheme != null) {
                currentTheme = activeTheme;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    currentThemeLiveData.setValue(activeTheme);
                });
            } else {
                // Créer et appliquer un thème par défaut
                initializeDefaultThemes();
            }
        });
    }

    private void initializeDefaultThemes() {
        executor.execute(() -> {
            // Vérifier si des thèmes par défaut existent
            List<Theme> defaultThemes = themeDao.getDefaultThemes();
            if (defaultThemes.isEmpty()) {
                createDefaultThemes();
            }
        });
    }

    private void createDefaultThemes() {
        // Thème clair par défaut
        Theme lightTheme = new Theme("Clair", Theme.ThemeType.LIGHT, 0);
        lightTheme.setDescription("Thème clair par défaut");
        lightTheme.setPrimaryColor("#2196F3");
        lightTheme.setSecondaryColor("#03DAC6");
        lightTheme.setBackgroundColor("#FFFFFF");
        lightTheme.setDefault(true);
        lightTheme.setActive(true);
        long lightId = themeDao.insert(lightTheme);
        
        // Thème sombre
        Theme darkTheme = new Theme("Sombre", Theme.ThemeType.DARK, 0);
        darkTheme.setDescription("Thème sombre par défaut");
        darkTheme.setPrimaryColor("#BB86FC");
        darkTheme.setSecondaryColor("#03DAC6");
        darkTheme.setBackgroundColor("#121212");
        themeDao.insert(darkTheme);
        
        // Définir le thème clair comme actuel
        lightTheme.setThemeId((int) lightId);
        currentTheme = lightTheme;
        prefs.edit().putInt(KEY_ACTIVE_THEME_ID, (int) lightId).apply();
        
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            currentThemeLiveData.setValue(lightTheme);
        });
    }

    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        executor.shutdown();
    }
}
