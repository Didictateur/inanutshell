package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * DAO pour les opérations sur les thèmes personnalisés
 */
@Dao
public interface ThemeDao {

    // ===================== Opérations CRUD de base =====================

    @Insert
    long insert(Theme theme);

    @Update
    void update(Theme theme);

    @Delete
    void delete(Theme theme);

    @Query("SELECT * FROM themes WHERE themeId = :themeId")
    Theme getThemeById(int themeId);

    @Query("SELECT * FROM themes WHERE themeId = :themeId")
    LiveData<Theme> getThemeByIdLive(int themeId);

    @Query("SELECT * FROM themes ORDER BY isDefault DESC, isActive DESC, themeName ASC")
    List<Theme> getAllThemes();

    @Query("SELECT * FROM themes ORDER BY isDefault DESC, isActive DESC, themeName ASC")
    LiveData<List<Theme>> getAllThemesLive();

    // ===================== Gestion des thèmes actifs =====================

    @Query("SELECT * FROM themes WHERE isActive = 1 LIMIT 1")
    Theme getActiveTheme();

    @Query("SELECT * FROM themes WHERE isActive = 1 LIMIT 1")
    LiveData<Theme> getActiveThemeLive();

    @Query("UPDATE themes SET isActive = 0")
    void deactivateAllThemes();

    @Query("UPDATE themes SET isActive = 1 WHERE themeId = :themeId")
    void activateTheme(int themeId);

    @Query("SELECT COUNT(*) FROM themes WHERE isActive = 1")
    int getActiveThemeCount();

    // ===================== Gestion des thèmes par défaut =====================

    @Query("SELECT * FROM themes WHERE isDefault = 1")
    List<Theme> getDefaultThemes();

    @Query("SELECT * FROM themes WHERE isDefault = 1")
    LiveData<List<Theme>> getDefaultThemesLive();

    @Query("UPDATE themes SET isDefault = 0 WHERE themeId != :themeId")
    void clearOtherDefaultFlags(int themeId);

    @Query("UPDATE themes SET isDefault = 1 WHERE themeId = :themeId")
    void setAsDefault(int themeId);

    // ===================== Gestion des thèmes utilisateur =====================

    @Query("SELECT * FROM themes WHERE userId = :userId ORDER BY isActive DESC, themeName ASC")
    List<Theme> getUserThemes(int userId);

    @Query("SELECT * FROM themes WHERE userId = :userId ORDER BY isActive DESC, themeName ASC")
    LiveData<List<Theme>> getUserThemesLive(int userId);

    @Query("SELECT * FROM themes WHERE userId = 0 ORDER BY themeType ASC, themeName ASC")
    List<Theme> getSystemThemes();

    @Query("SELECT * FROM themes WHERE userId = 0 ORDER BY themeType ASC, themeName ASC")
    LiveData<List<Theme>> getSystemThemesLive();

    @Query("SELECT COUNT(*) FROM themes WHERE userId = :userId")
    int getUserThemeCount(int userId);

    // ===================== Gestion des types de thèmes =====================

    @Query("SELECT * FROM themes WHERE themeType = :themeType ORDER BY themeName ASC")
    List<Theme> getThemesByType(Theme.ThemeType themeType);

    @Query("SELECT * FROM themes WHERE themeType = :themeType ORDER BY themeName ASC")
    LiveData<List<Theme>> getThemesByTypeLive(Theme.ThemeType themeType);

    @Query("SELECT * FROM themes WHERE themeType = 'CUSTOM' AND userId = :userId")
    List<Theme> getUserCustomThemes(int userId);

    @Query("SELECT * FROM themes WHERE themeType IN ('LIGHT', 'DARK', 'HIGH_CONTRAST')")
    List<Theme> getBuiltInThemes();

    // ===================== Recherche de thèmes =====================

    @Query("SELECT * FROM themes WHERE " +
           "(themeName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
           "ORDER BY isDefault DESC, isActive DESC, themeName ASC")
    List<Theme> searchThemes(String query);

    @Query("SELECT * FROM themes WHERE " +
           "userId = :userId AND " +
           "(themeName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
           "ORDER BY isActive DESC, themeName ASC")
    List<Theme> searchUserThemes(int userId, String query);

    @Query("SELECT * FROM themes WHERE themeName = :name AND userId = :userId")
    Theme getThemeByNameAndUser(String name, int userId);

    @Query("SELECT COUNT(*) > 0 FROM themes WHERE themeName = :name AND userId = :userId")
    boolean themeNameExistsForUser(String name, int userId);

    // ===================== Gestion des couleurs =====================

    @Query("UPDATE themes SET primaryColor = :color WHERE themeId = :themeId")
    void updatePrimaryColor(int themeId, String color);

    @Query("UPDATE themes SET secondaryColor = :color WHERE themeId = :themeId")
    void updateSecondaryColor(int themeId, String color);

    @Query("UPDATE themes SET backgroundColor = :color WHERE themeId = :themeId")
    void updateBackgroundColor(int themeId, String color);

    @Query("SELECT * FROM themes WHERE primaryColor = :color")
    List<Theme> getThemesByPrimaryColor(String color);

    // ===================== Gestion de l'accessibilité =====================

    @Query("UPDATE themes SET textScale = :scale WHERE themeId = :themeId")
    void updateTextScale(int themeId, float scale);

    @Query("UPDATE themes SET highContrast = :highContrast WHERE themeId = :themeId")
    void updateHighContrast(int themeId, boolean highContrast);

    @Query("UPDATE themes SET boldText = :boldText WHERE themeId = :themeId")
    void updateBoldText(int themeId, boolean boldText);

    @Query("UPDATE themes SET reducedMotion = :reducedMotion WHERE themeId = :themeId")
    void updateReducedMotion(int themeId, boolean reducedMotion);

    @Query("SELECT * FROM themes WHERE highContrast = 1")
    List<Theme> getHighContrastThemes();

    @Query("SELECT * FROM themes WHERE textScale > 1.2")
    List<Theme> getLargeTextThemes();

    // ===================== Gestion de l'interface =====================

    @Query("UPDATE themes SET roundedCorners = :rounded WHERE themeId = :themeId")
    void updateRoundedCorners(int themeId, boolean rounded);

    @Query("UPDATE themes SET cornerRadius = :radius WHERE themeId = :themeId")
    void updateCornerRadius(int themeId, float radius);

    @Query("UPDATE themes SET elevation = :elevation WHERE themeId = :themeId")
    void updateElevation(int themeId, float elevation);

    @Query("UPDATE themes SET fontFamily = :fontFamily WHERE themeId = :themeId")
    void updateFontFamily(int themeId, String fontFamily);

    // ===================== Statistiques =====================

    @Query("SELECT COUNT(*) FROM themes")
    int getTotalThemeCount();

    @Query("SELECT COUNT(*) FROM themes WHERE themeType = :themeType")
    int getThemeCountByType(Theme.ThemeType themeType);

    @Query("SELECT themeType, COUNT(*) as count FROM themes GROUP BY themeType")
    List<ThemeTypeCount> getThemeCountsByType();

    @Query("SELECT COUNT(*) FROM themes WHERE dateCreated > :timestamp")
    int getRecentThemeCount(long timestamp);

    // ===================== Maintenance et nettoyage =====================

    @Query("DELETE FROM themes WHERE userId = :userId AND themeType = 'CUSTOM' AND isActive = 0")
    int deleteInactiveUserThemes(int userId);

    @Query("UPDATE themes SET lastModified = :timestamp WHERE themeId = :themeId")
    void updateLastModified(int themeId, long timestamp);

    @Query("SELECT * FROM themes WHERE lastModified < :cutoffDate AND isActive = 0 AND isDefault = 0")
    List<Theme> getUnusedThemes(long cutoffDate);

    // ===================== Import/Export =====================

    @Query("SELECT * FROM themes WHERE userId = :userId")
    List<Theme> exportUserThemes(int userId);

    @Query("SELECT * FROM themes WHERE themeType IN ('LIGHT', 'DARK', 'HIGH_CONTRAST', 'MATERIAL_YOU')")
    List<Theme> getExportableThemes();

    // ===================== Thèmes saisonniers et adaptatifs =====================

    @Query("SELECT * FROM themes WHERE themeType = 'SEASONAL'")
    List<Theme> getSeasonalThemes();

    @Query("SELECT * FROM themes WHERE themeType = 'MATERIAL_YOU'")
    List<Theme> getMaterialYouThemes();

    @Query("UPDATE themes SET primaryColor = :primaryColor, secondaryColor = :secondaryColor " +
           "WHERE themeType = 'SEASONAL'")
    void updateSeasonalColors(String primaryColor, String secondaryColor);

    // ===================== Validation et contraintes =====================

    @Query("SELECT * FROM themes WHERE " +
           "primaryColor IS NULL OR backgroundColor IS NULL OR themeName IS NULL")
    List<Theme> getInvalidThemes();

    @Query("SELECT COUNT(*) FROM themes WHERE " +
           "themeName = :name AND userId = :userId AND themeId != :excludeThemeId")
    int countDuplicateNames(String name, int userId, int excludeThemeId);

    // ===================== Requêtes avancées =====================

    @Query("SELECT t.* FROM themes t " +
           "WHERE t.userId = :userId OR t.userId = 0 " +
           "ORDER BY " +
           "CASE WHEN t.isActive THEN 0 ELSE 1 END, " +
           "CASE WHEN t.isDefault THEN 0 ELSE 1 END, " +
           "CASE WHEN t.userId = :userId THEN 0 ELSE 1 END, " +
           "t.themeName ASC")
    List<Theme> getAvailableThemesForUser(int userId);

    @Query("SELECT t.* FROM themes t " +
           "WHERE t.userId = :userId OR t.userId = 0 " +
           "ORDER BY " +
           "CASE WHEN t.isActive THEN 0 ELSE 1 END, " +
           "CASE WHEN t.isDefault THEN 0 ELSE 1 END, " +
           "CASE WHEN t.userId = :userId THEN 0 ELSE 1 END, " +
           "t.themeName ASC")
    LiveData<List<Theme>> getAvailableThemesForUserLive(int userId);
}
