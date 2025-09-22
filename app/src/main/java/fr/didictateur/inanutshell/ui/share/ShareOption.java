package fr.didictateur.inanutshell.ui.share;

/**
 * Classe représentant une option de partage
 */
public class ShareOption {
    
    public enum Type {
        TEXT, IMAGE, QR_CODE, PUBLIC_LINK, WHATSAPP, FACEBOOK, TWITTER, INSTAGRAM
    }
    
    private String title;
    private String description;
    private int iconResId;
    private Type type;
    
    public ShareOption(String title, String description, int iconResId, Type type) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.type = type;
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public Type getType() { return type; }
    
    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public void setType(Type type) { this.type = type; }
}
