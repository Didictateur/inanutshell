package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

/**
 * Entité Theme pour gérer les thèmes personnalisés et la personnalisation de l'interface
 */
@Entity(tableName = "themes")
@TypeConverters({Converters.class})
public class Theme {

    @PrimaryKey(autoGenerate = true)
    public int themeId;

    public String themeName;
    public String description;
    public int userId; // Propriétaire du thème (0 = thème système)
    public ThemeType themeType;
    public boolean isActive;
    public boolean isDefault; // Thème par défaut du système
    public Date dateCreated;
    public Date lastModified;

    // Couleurs principales
    public String primaryColor; // Couleur primaire (hex)
    public String primaryVariantColor; // Variante de la couleur primaire
    public String secondaryColor; // Couleur secondaire (hex)
    public String secondaryVariantColor; // Variante de la couleur secondaire
    public String backgroundColor; // Couleur de fond
    public String surfaceColor; // Couleur des surfaces
    public String errorColor; // Couleur d'erreur

    // Couleurs de texte
    public String onPrimaryColor; // Texte sur couleur primaire
    public String onSecondaryColor; // Texte sur couleur secondaire
    public String onBackgroundColor; // Texte sur fond
    public String onSurfaceColor; // Texte sur surface
    public String onErrorColor; // Texte sur erreur

    // Paramètres d'accessibilité
    public float textScale; // Échelle de texte (1.0 = normal)
    public boolean highContrast; // Contraste élevé
    public boolean boldText; // Texte en gras
    public boolean reducedMotion; // Animations réduites

    // Paramètres d'interface
    public boolean roundedCorners; // Coins arrondis
    public float cornerRadius; // Rayon des coins (dp)
    public float elevation; // Élévation par défaut
    public String fontFamily; // Police personnalisée

    /**
     * Types de thèmes disponibles
     */
    public enum ThemeType {
        LIGHT("Clair", "Thème clair standard"),
        DARK("Sombre", "Thème sombre standard"),
        AUTO("Automatique", "Suit les paramètres système"),
        CUSTOM("Personnalisé", "Thème entièrement personnalisé"),
        MATERIAL_YOU("Material You", "Thème adaptatif Android 12+"),
        HIGH_CONTRAST("Contraste élevé", "Thème pour malvoyants"),
        SEASONAL("Saisonnier", "Thème qui change selon la saison"),
        RECIPE_THEMED("Thématique recette", "Couleurs inspirées des ingrédients");

        private final String displayName;
        private final String description;

        ThemeType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Vérifie si ce type permet la personnalisation
         */
        public boolean isCustomizable() {
            return this == CUSTOM || this == RECIPE_THEMED;
        }

        /**
         * Vérifie si ce type s'adapte automatiquement
         */
        public boolean isAdaptive() {
            return this == AUTO || this == MATERIAL_YOU || this == SEASONAL;
        }
    }

    // Constructeurs
    public Theme() {
        this.dateCreated = new Date();
        this.lastModified = new Date();
        this.isActive = false;
        this.isDefault = false;
        this.themeType = ThemeType.LIGHT;
        this.textScale = 1.0f;
        this.highContrast = false;
        this.boldText = false;
        this.reducedMotion = false;
        this.roundedCorners = true;
        this.cornerRadius = 8.0f;
        this.elevation = 4.0f;
        this.fontFamily = "default";
        setDefaultLightColors();
    }

    @Ignore
    public Theme(String themeName, ThemeType themeType, int userId) {
        this();
        this.themeName = themeName;
        this.themeType = themeType;
        this.userId = userId;
        applyThemeTypeDefaults();
    }

    // Méthodes utilitaires

    /**
     * Applique les couleurs par défaut selon le type de thème
     */
    public void applyThemeTypeDefaults() {
        switch (themeType) {
            case LIGHT:
                setDefaultLightColors();
                break;
            case DARK:
                setDefaultDarkColors();
                break;
            case HIGH_CONTRAST:
                setHighContrastColors();
                this.highContrast = true;
                this.boldText = true;
                break;
            case CUSTOM:
                // Garde les couleurs actuelles
                break;
            case MATERIAL_YOU:
                setMaterialYouColors();
                break;
            case SEASONAL:
                setSeasonalColors();
                break;
            case RECIPE_THEMED:
                setRecipeThemedColors();
                break;
            default:
                setDefaultLightColors();
                break;
        }
    }

    /**
     * Définit les couleurs par défaut pour le thème clair
     */
    private void setDefaultLightColors() {
        this.primaryColor = "#6200EA"; // Purple
        this.primaryVariantColor = "#3700B3";
        this.secondaryColor = "#03DAC6"; // Teal
        this.secondaryVariantColor = "#018786";
        this.backgroundColor = "#FFFFFF";
        this.surfaceColor = "#FFFFFF";
        this.errorColor = "#B00020";
        
        this.onPrimaryColor = "#FFFFFF";
        this.onSecondaryColor = "#000000";
        this.onBackgroundColor = "#000000";
        this.onSurfaceColor = "#000000";
        this.onErrorColor = "#FFFFFF";
    }

    /**
     * Définit les couleurs par défaut pour le thème sombre
     */
    private void setDefaultDarkColors() {
        this.primaryColor = "#BB86FC";
        this.primaryVariantColor = "#3700B3";
        this.secondaryColor = "#03DAC6";
        this.secondaryVariantColor = "#03DAC6";
        this.backgroundColor = "#121212";
        this.surfaceColor = "#121212";
        this.errorColor = "#CF6679";
        
        this.onPrimaryColor = "#000000";
        this.onSecondaryColor = "#000000";
        this.onBackgroundColor = "#FFFFFF";
        this.onSurfaceColor = "#FFFFFF";
        this.onErrorColor = "#000000";
    }

    /**
     * Définit les couleurs pour le thème haute contraste
     */
    private void setHighContrastColors() {
        this.primaryColor = "#0000FF"; // Bleu vif
        this.primaryVariantColor = "#000080";
        this.secondaryColor = "#FF6600"; // Orange vif
        this.secondaryVariantColor = "#CC5500";
        this.backgroundColor = "#FFFFFF";
        this.surfaceColor = "#F8F8F8";
        this.errorColor = "#FF0000"; // Rouge vif
        
        this.onPrimaryColor = "#FFFFFF";
        this.onSecondaryColor = "#FFFFFF";
        this.onBackgroundColor = "#000000";
        this.onSurfaceColor = "#000000";
        this.onErrorColor = "#FFFFFF";
    }

    /**
     * Couleurs Material You (Android 12+)
     */
    private void setMaterialYouColors() {
        // Couleurs dynamiques qui s'adaptent au wallpaper
        this.primaryColor = "#6750A4";
        this.primaryVariantColor = "#625B71";
        this.secondaryColor = "#7D5260";
        this.secondaryVariantColor = "#625B71";
        this.backgroundColor = "#FFFBFE";
        this.surfaceColor = "#FFFBFE";
        this.errorColor = "#BA1A1A";
        
        this.onPrimaryColor = "#FFFFFF";
        this.onSecondaryColor = "#FFFFFF";
        this.onBackgroundColor = "#1C1B1F";
        this.onSurfaceColor = "#1C1B1F";
        this.onErrorColor = "#FFFFFF";
    }

    /**
     * Couleurs saisonnières
     */
    private void setSeasonalColors() {
        // Détermine la saison actuelle et applique les couleurs correspondantes
        int month = new Date().getMonth();
        if (month >= 2 && month <= 4) { // Printemps
            this.primaryColor = "#4CAF50"; // Vert
            this.secondaryColor = "#FFEB3B"; // Jaune
        } else if (month >= 5 && month <= 7) { // Été
            this.primaryColor = "#FF9800"; // Orange
            this.secondaryColor = "#00BCD4"; // Cyan
        } else if (month >= 8 && month <= 10) { // Automne
            this.primaryColor = "#FF5722"; // Rouge-orange
            this.secondaryColor = "#FFC107"; // Ambre
        } else { // Hiver
            this.primaryColor = "#2196F3"; // Bleu
            this.secondaryColor = "#9C27B0"; // Violet
        }
        
        this.backgroundColor = "#FAFAFA";
        this.surfaceColor = "#FFFFFF";
        this.errorColor = "#F44336";
    }

    /**
     * Couleurs inspirées des recettes
     */
    private void setRecipeThemedColors() {
        this.primaryColor = "#D32F2F"; // Rouge tomate
        this.primaryVariantColor = "#B71C1C";
        this.secondaryColor = "#388E3C"; // Vert basilic
        this.secondaryVariantColor = "#2E7D32";
        this.backgroundColor = "#FFF8E1"; // Crème
        this.surfaceColor = "#FFFFFF";
        this.errorColor = "#E65100"; // Orange épicé
        
        this.onPrimaryColor = "#FFFFFF";
        this.onSecondaryColor = "#FFFFFF";
        this.onBackgroundColor = "#3E2723"; // Brun chocolat
        this.onSurfaceColor = "#3E2723";
        this.onErrorColor = "#FFFFFF";
    }

    /**
     * Met à jour les paramètres d'accessibilité
     */
    public void updateAccessibility(float textScale, boolean highContrast, 
                                  boolean boldText, boolean reducedMotion) {
        this.textScale = Math.max(0.8f, Math.min(2.0f, textScale)); // Limite entre 0.8x et 2.0x
        this.highContrast = highContrast;
        this.boldText = boldText;
        this.reducedMotion = reducedMotion;
        this.lastModified = new Date();
    }

    /**
     * Met à jour les paramètres d'interface
     */
    public void updateInterfaceSettings(boolean roundedCorners, float cornerRadius, 
                                      float elevation, String fontFamily) {
        this.roundedCorners = roundedCorners;
        this.cornerRadius = Math.max(0, Math.min(20, cornerRadius)); // Limite entre 0 et 20dp
        this.elevation = Math.max(0, Math.min(16, elevation)); // Limite entre 0 et 16dp
        this.fontFamily = fontFamily != null ? fontFamily : "default";
        this.lastModified = new Date();
    }

    /**
     * Active ce thème
     */
    public void activate() {
        this.isActive = true;
        this.lastModified = new Date();
    }

    /**
     * Désactive ce thème
     */
    public void deactivate() {
        this.isActive = false;
        this.lastModified = new Date();
    }

    /**
     * Vérifie si le thème est valide
     */
    public boolean isValid() {
        return themeName != null && !themeName.trim().isEmpty() &&
               primaryColor != null && backgroundColor != null;
    }

    /**
     * Crée une copie personnalisée de ce thème
     */
    public Theme createCustomCopy(String newName, int newUserId) {
        Theme copy = new Theme();
        copy.themeName = newName;
        copy.description = "Copie personnalisée de " + this.themeName;
        copy.userId = newUserId;
        copy.themeType = ThemeType.CUSTOM;
        
        // Copier toutes les couleurs
        copy.primaryColor = this.primaryColor;
        copy.primaryVariantColor = this.primaryVariantColor;
        copy.secondaryColor = this.secondaryColor;
        copy.secondaryVariantColor = this.secondaryVariantColor;
        copy.backgroundColor = this.backgroundColor;
        copy.surfaceColor = this.surfaceColor;
        copy.errorColor = this.errorColor;
        copy.onPrimaryColor = this.onPrimaryColor;
        copy.onSecondaryColor = this.onSecondaryColor;
        copy.onBackgroundColor = this.onBackgroundColor;
        copy.onSurfaceColor = this.onSurfaceColor;
        copy.onErrorColor = this.onErrorColor;
        
        // Copier les paramètres
        copy.textScale = this.textScale;
        copy.highContrast = this.highContrast;
        copy.boldText = this.boldText;
        copy.reducedMotion = this.reducedMotion;
        copy.roundedCorners = this.roundedCorners;
        copy.cornerRadius = this.cornerRadius;
        copy.elevation = this.elevation;
        copy.fontFamily = this.fontFamily;
        
        return copy;
    }

    /**
     * Obtient un nom d'affichage complet
     */
    public String getDisplayName() {
        String display = themeName;
        if (isDefault) display += " (Par défaut)";
        if (isActive) display += " (Actif)";
        return display;
    }

    @Override
    public String toString() {
        return "Theme{" +
                "themeId=" + themeId +
                ", themeName='" + themeName + '\'' +
                ", themeType=" + themeType +
                ", userId=" + userId +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                '}';
    }

    // ===================== GETTERS ET SETTERS =====================

    public int getThemeId() {
        return themeId;
    }

    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ThemeType getThemeType() {
        return themeType;
    }

    public void setThemeType(ThemeType themeType) {
        this.themeType = themeType;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModified(long timestamp) {
        this.lastModified = new Date(timestamp);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getPrimaryVariantColor() {
        return primaryVariantColor;
    }

    public void setPrimaryVariantColor(String primaryVariantColor) {
        this.primaryVariantColor = primaryVariantColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getSecondaryVariantColor() {
        return secondaryVariantColor;
    }

    public void setSecondaryVariantColor(String secondaryVariantColor) {
        this.secondaryVariantColor = secondaryVariantColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getSurfaceColor() {
        return surfaceColor;
    }

    public void setSurfaceColor(String surfaceColor) {
        this.surfaceColor = surfaceColor;
    }

    public String getErrorColor() {
        return errorColor;
    }

    public void setErrorColor(String errorColor) {
        this.errorColor = errorColor;
    }

    public String getOnPrimaryColor() {
        return onPrimaryColor;
    }

    public void setOnPrimaryColor(String onPrimaryColor) {
        this.onPrimaryColor = onPrimaryColor;
    }

    public String getOnSecondaryColor() {
        return onSecondaryColor;
    }

    public void setOnSecondaryColor(String onSecondaryColor) {
        this.onSecondaryColor = onSecondaryColor;
    }

    public String getOnBackgroundColor() {
        return onBackgroundColor;
    }

    public void setOnBackgroundColor(String onBackgroundColor) {
        this.onBackgroundColor = onBackgroundColor;
    }

    public String getOnSurfaceColor() {
        return onSurfaceColor;
    }

    public void setOnSurfaceColor(String onSurfaceColor) {
        this.onSurfaceColor = onSurfaceColor;
    }

    public String getOnErrorColor() {
        return onErrorColor;
    }

    public void setOnErrorColor(String onErrorColor) {
        this.onErrorColor = onErrorColor;
    }

    public float getTextScale() {
        return textScale;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    public boolean isHighContrast() {
        return highContrast;
    }

    public void setHighContrast(boolean highContrast) {
        this.highContrast = highContrast;
    }

    public boolean isBoldText() {
        return boldText;
    }

    public void setBoldText(boolean boldText) {
        this.boldText = boldText;
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
    }

    public boolean isRoundedCorners() {
        return roundedCorners;
    }

    public void setRoundedCorners(boolean roundedCorners) {
        this.roundedCorners = roundedCorners;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }
}
