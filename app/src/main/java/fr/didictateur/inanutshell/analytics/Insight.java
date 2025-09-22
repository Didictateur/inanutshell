package fr.didictateur.inanutshell.analytics;

/**
 * Classe pour représenter un insight automatique
 */
public class Insight {
    public String title;
    public String description;
    public String type; // "positive", "negative", "neutral", "warning"
    
    public Insight(String title, String description, String type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }
}
