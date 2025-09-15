package fr.didictateur.inanutshell;

/**
 * Classe pour les résultats de comptage des thèmes par type
 */
public class ThemeTypeCount {
    public Theme.ThemeType themeType;
    public int count;
    
    public ThemeTypeCount() {}
    
    @androidx.room.Ignore
    public ThemeTypeCount(Theme.ThemeType themeType, int count) {
        this.themeType = themeType;
        this.count = count;
    }
    
    public Theme.ThemeType getThemeType() {
        return themeType;
    }
    
    public void setThemeType(Theme.ThemeType themeType) {
        this.themeType = themeType;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
}
