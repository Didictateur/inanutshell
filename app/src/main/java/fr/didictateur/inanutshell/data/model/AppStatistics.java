package fr.didictateur.inanutshell.data.model;

import java.util.List;
import java.util.Date;

/**
 * Modèle de données pour les statistiques d'utilisation de l'application
 */
public class AppStatistics {
    public int totalRecipes;
    public int favoriteRecipes;
    public int recipesViewed;
    public int recipesCreated;
    public int searchesPerformed;
    public float averageRating;
    public long totalTimeSpent; // in milliseconds
    public Date lastActivity;
    
    // Champs multi-utilisateurs
    public int totalUsers;
    public int totalComments;
    public int totalSessions;
    public int todayActiveUsers;
    public int weekActiveUsers;
    public int monthActiveUsers;
    
    // Statistiques par période
    public List<DataPoint> dailyStats;
    public List<DataPoint> weeklyStats;
    public List<DataPoint> monthlyStats;
    public long totalUsageTime;
    
    public List<CategoryStats> topCategories;
    public List<RecipeStats> topRecipes;
    public List<SearchTerm> topSearchTerms;
    public List<SearchTerm> topSearches; // Alias pour topSearchTerms
    public List<EventCount> topEvents;
    public List<UserEngagement> userEngagement;
    public TrendAnalysis trendData;
    
    public AppStatistics() {}
    
    // Classes internes pour les statistiques détaillées
    public static class CategoryStats {
        public String categoryName;
        public String category; // Alias pour categoryName
        public int count;
        public float percentage;
        public double popularity; // Popularité relative
        
        public CategoryStats(String name, int count, float percentage) {
            this.categoryName = name;
            this.category = name; // Même valeur
            this.count = count;
            this.percentage = percentage;
            this.popularity = percentage; // Même valeur en double
        }
    }
    
    public static class RecipeStats {
        public String recipeName;
        public int viewCount;
        public int views; // Alias pour viewCount
        public float rating;
        public Date lastViewed;
        
        public RecipeStats(String name, int views, float rating, Date lastViewed) {
            this.recipeName = name;
            this.viewCount = views;
            this.views = views; // Même valeur
            this.rating = rating;
            this.lastViewed = lastViewed;
        }
    }
    
    public static class SearchTerm {
        public String term;
        public int frequency;
        public Date lastUsed;
        public float successRate; // Pourcentage de succès des recherches
        
        public SearchTerm(String term, int frequency, Date lastUsed) {
            this.term = term;
            this.frequency = frequency;
            this.lastUsed = lastUsed;
            this.successRate = 0.0f;
        }
        
        public SearchTerm(String term, int frequency, Date lastUsed, float successRate) {
            this.term = term;
            this.frequency = frequency;
            this.lastUsed = lastUsed;
            this.successRate = successRate;
        }
    }
    
    public static class EventCount {
        public String eventType;
        public int count;
        public Date firstOccurrence;
        public Date lastOccurrence;
        
        public EventCount(String type, int count, Date first, Date last) {
            this.eventType = type;
            this.count = count;
            this.firstOccurrence = first;
            this.lastOccurrence = last;
        }
    }
    
    public static class UserEngagement {
        public String metric;
        public double value;
        public String period;
        public String trend; // "increasing", "decreasing", "stable"
        
        public UserEngagement(String metric, double value, String period, String trend) {
            this.metric = metric;
            this.value = value;
            this.period = period;
            this.trend = trend;
        }
    }
    
    public static class TrendAnalysis {
        public String metricName;
        public List<DataPoint> dataPoints;
        public String trendDirection;
        public double trendStrength;
        public String period;
        
        public TrendAnalysis() {
            this.dataPoints = new java.util.ArrayList<>();
        }
        
        public TrendAnalysis(String metric, List<DataPoint> points, String direction, double strength) {
            this.metricName = metric;
            this.dataPoints = points;
            this.trendDirection = direction;
            this.trendStrength = strength;
        }
        
        public void analyzeTrend() {
            if (dataPoints == null || dataPoints.size() < 2) {
                trendDirection = "insufficient_data";
                trendStrength = 0.0;
                return;
            }
            
            // Analyse simple de tendance
            double firstValue = dataPoints.get(0).value;
            double lastValue = dataPoints.get(dataPoints.size() - 1).value;
            
            if (lastValue > firstValue) {
                trendDirection = "increasing";
                trendStrength = (lastValue - firstValue) / firstValue;
            } else if (lastValue < firstValue) {
                trendDirection = "decreasing";
                trendStrength = (firstValue - lastValue) / firstValue;
            } else {
                trendDirection = "stable";
                trendStrength = 0.0;
            }
        }
        
        public static class TrendPoint {
            public Date date;
            public double value;
            public String label;
            
            public TrendPoint(Date date, double value, String label) {
                this.date = date;
                this.value = value;
                this.label = label;
            }
        }
    }
    
    public static class DataPoint {
        public Date date;
        public double value;
        public String label;
        
        public DataPoint(Date date, double value, String label) {
            this.date = date;
            this.value = value;
            this.label = label;
        }
    }
}
