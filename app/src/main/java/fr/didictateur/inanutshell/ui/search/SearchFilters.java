package fr.didictateur.inanutshell.ui.search;

import java.util.List;

/**
 * Classe pour gérer les filtres de recherche
 */
public class SearchFilters {
    
    /**
     * Énumération des niveaux de difficulté
     */
    public enum DifficultyLevel {
        EASY("Facile", 1),
        MEDIUM("Moyen", 2),
        HARD("Difficile", 3);
        
        private final String displayName;
        private final int level;
        
        DifficultyLevel(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getLevel() {
            return level;
        }
        
        public static DifficultyLevel fromLevel(int level) {
            for (DifficultyLevel difficulty : values()) {
                if (difficulty.level == level) {
                    return difficulty;
                }
            }
            return null;
        }
        
        public static DifficultyLevel fromName(String name) {
            if (name == null) return null;
            for (DifficultyLevel difficulty : values()) {
                if (difficulty.name().equalsIgnoreCase(name) || 
                    difficulty.displayName.equalsIgnoreCase(name)) {
                    return difficulty;
                }
            }
            return null;
        }
    }
    
    private String textQuery = "";
    private List<String> categories = null;
    private List<String> tags = null;
    private String ingredient = "";
    private Integer maxCookTime = null;
    private Integer maxPrepTime = null;
    private boolean favoritesOnly = false;
    private DifficultyLevel maxDifficulty = null;
    
    public SearchFilters() {}
    
    // Getters
    public String getTextQuery() {
        return textQuery;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public String getIngredient() {
        return ingredient;
    }
    
    public Integer getMaxCookTime() {
        return maxCookTime;
    }
    
    public Integer getMaxPrepTime() {
        return maxPrepTime;
    }
    
    public boolean isFavoritesOnly() {
        return favoritesOnly;
    }
    
    public DifficultyLevel getMaxDifficulty() {
        return maxDifficulty;
    }

    // Setters
    public void setTextQuery(String textQuery) {
        this.textQuery = textQuery != null ? textQuery : "";
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public void setIngredient(String ingredient) {
        this.ingredient = ingredient != null ? ingredient : "";
    }
    
    public void setMaxCookTime(Integer maxCookTime) {
        this.maxCookTime = maxCookTime;
    }
    
    public void setMaxPrepTime(Integer maxPrepTime) {
        this.maxPrepTime = maxPrepTime;
    }
    
    public void setFavoritesOnly(boolean favoritesOnly) {
        this.favoritesOnly = favoritesOnly;
    }
    
    public void setMaxDifficulty(DifficultyLevel maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    // Utilitaires
    public boolean isEmpty() {
        return textQuery.isEmpty() 
            && (categories == null || categories.isEmpty())
            && (tags == null || tags.isEmpty())
            && ingredient.isEmpty()
            && maxCookTime == null
            && maxPrepTime == null
            && !favoritesOnly
            && maxDifficulty == null;
    }
    
    public boolean hasActiveFilters() {
        return !isEmpty();
    }
    
    public void clear() {
        textQuery = "";
        categories = null;
        tags = null;
        ingredient = "";
        maxCookTime = null;
        maxPrepTime = null;
        favoritesOnly = false;
        maxDifficulty = null;
    }
    
    @Override
    public String toString() {
        return "SearchFilters{" +
                "textQuery='" + textQuery + '\'' +
                ", categories=" + categories +
                ", tags=" + tags +
                ", ingredient='" + ingredient + '\'' +
                ", maxCookTime=" + maxCookTime +
                ", maxPrepTime=" + maxPrepTime +
                ", favoritesOnly=" + favoritesOnly +
                ", maxDifficulty=" + maxDifficulty +
                '}';
    }
}
