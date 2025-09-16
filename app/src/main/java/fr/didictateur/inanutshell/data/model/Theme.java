package fr.didictateur.inanutshell.data.model;

import java.util.Date;
import java.util.Map;

public class Theme {
    public enum ThemeType {
        LIGHT,
        DARK,
        AUTO,
        CUSTOM
    }
    
    private long id;
    private String name;
    private String description;
    private ThemeType type;
    private Map<String, String> colors;
    private boolean isDefault;
    private boolean isActive;
    private Date createdDate;
    private Date modifiedDate;
    private String author;
    private double version;
    private boolean systemTheme;
    
    // Color properties
    private int primaryColor;
    private int secondaryColor;
    private int backgroundColor;
    private int textColor;
    private int accentColor;
    
    // Constructors
    public Theme() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.isDefault = false;
        this.isActive = false;
        this.version = 1.0;
        this.systemTheme = false;
    }
    
    public Theme(String name, ThemeType type) {
        this();
        this.name = name;
        this.type = type;
    }
    
    public Theme(String name, String description, ThemeType type) {
        this(name, type);
        this.description = description;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedDate = new Date();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.modifiedDate = new Date();
    }
    
    public ThemeType getType() { return type; }
    public void setType(ThemeType type) { 
        this.type = type;
        this.modifiedDate = new Date();
    }
    
    public Map<String, String> getColors() { return colors; }
    public void setColors(Map<String, String> colors) { 
        this.colors = colors;
        this.modifiedDate = new Date();
    }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public double getVersion() { return version; }
    public void setVersion(double version) { this.version = version; }
    
    public boolean isSystemTheme() { return systemTheme; }
    public void setSystemTheme(boolean systemTheme) { this.systemTheme = systemTheme; }
    
    public int getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(int primaryColor) { 
        this.primaryColor = primaryColor;
        this.modifiedDate = new Date();
    }
    
    public int getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(int secondaryColor) { 
        this.secondaryColor = secondaryColor;
        this.modifiedDate = new Date();
    }
    
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) { 
        this.backgroundColor = backgroundColor;
        this.modifiedDate = new Date();
    }
    
    public int getTextColor() { return textColor; }
    public void setTextColor(int textColor) { 
        this.textColor = textColor;
        this.modifiedDate = new Date();
    }
    
    public int getAccentColor() { return accentColor; }
    public void setAccentColor(int accentColor) { 
        this.accentColor = accentColor;
        this.modifiedDate = new Date();
    }
    
    // Utility methods
    public String getTypeDisplayName() {
        if (type == null) return "Non défini";
        
        switch (type) {
            case LIGHT: return "Clair";
            case DARK: return "Sombre";
            case AUTO: return "Automatique";
            case CUSTOM: return "Personnalisé";
            default: return "Non défini";
        }
    }
    
    public boolean isLightTheme() {
        return type == ThemeType.LIGHT;
    }
    
    public boolean isDarkTheme() {
        return type == ThemeType.DARK;
    }
    
    public boolean isAutoTheme() {
        return type == ThemeType.AUTO;
    }
    
    public boolean isCustomTheme() {
        return type == ThemeType.CUSTOM;
    }
    
    public void addColor(String key, String value) {
        if (colors != null) {
            colors.put(key, value);
            this.modifiedDate = new Date();
        }
    }
    
    public String getColor(String key) {
        return colors != null ? colors.get(key) : null;
    }
    
    public void removeColor(String key) {
        if (colors != null) {
            colors.remove(key);
            this.modifiedDate = new Date();
        }
    }
    
    public boolean hasColor(String key) {
        return colors != null && colors.containsKey(key);
    }
    
    public int getColorCount() {
        return colors != null ? colors.size() : 0;
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Thème sans nom";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Theme theme = (Theme) obj;
        return id == theme.id;
    }
    
    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
