package fr.didictateur.inanutshell.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.model.AppStatistics;

import fr.didictateur.inanutshell.data.model.UsageStats;
import fr.didictateur.inanutshell.data.model.Recipe;

public class AnalyticsManager {
    private static AnalyticsManager instance;
    private Context context;
    private SharedPreferences analyticsPrefs;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private String currentSessionId;
    private long sessionStartTime;
    private boolean analyticsEnabled = true;
    
    // Cache des statistiques
    private AppStatistics cachedStats;
    private long lastStatsUpdate = 0;
    private static final long STATS_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    // Listeners pour les mises à jour
    private List<AnalyticsListener> listeners = new ArrayList<>();
    
    public interface AnalyticsListener {
        void onStatsUpdated(AppStatistics stats);
        void onEventTracked(String eventType);
    }
    
    private AnalyticsManager(Context context) {
        this.context = context.getApplicationContext();
        this.analyticsPrefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        loadConfiguration();
        startNewSession();
    }
    
    public static synchronized AnalyticsManager getInstance(Context context) {
        if (instance == null) {
            instance = new AnalyticsManager(context);
        }
        return instance;
    }
    
    // Gestion des sessions
    public void startNewSession() {
        currentSessionId = UUID.randomUUID().toString();
        sessionStartTime = System.currentTimeMillis();
        
        if (analyticsEnabled) {
            trackEvent(UsageStats.EVENT_APP_OPEN, "{\"session_id\":\"" + currentSessionId + "\"}");
        }
    }
    
    public void endSession() {
        if (analyticsEnabled && currentSessionId != null) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            String data = "{\"session_id\":\"" + currentSessionId + "\",\"duration\":" + sessionDuration + "}";
            trackEvent(UsageStats.EVENT_APP_CLOSE, data);
        }
    }
    
    // Suivi des événements
    public void trackEvent(String eventType, String eventData) {
        if (!analyticsEnabled) return;
        
        executorService.execute(() -> {
            try {
                String userId = getCurrentUserId();
                UsageStats stats = new UsageStats(userId, eventType, eventData, currentSessionId);
                
                // TODO: Sauvegarder en base de données avec Room
                saveUsageStatToDatabase(stats);
                
                // Notifier les listeners
                mainHandler.post(() -> {
                    for (AnalyticsListener listener : listeners) {
                        listener.onEventTracked(eventType);
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    // Méthodes de suivi spécialisées
    public void trackRecipeView(String recipeId, String recipeName, String category) {
        String data = String.format(java.util.Locale.ROOT, "{\"recipe_id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\"}", 
            recipeId, recipeName, category);
        trackEvent(UsageStats.EVENT_RECIPE_VIEW, data);
    }
    
    public void trackRecipeCreate(String recipeId, String category, int ingredientCount) {
        String data = String.format(java.util.Locale.ROOT, "{\"recipe_id\":\"%s\",\"category\":\"%s\",\"ingredients\":%d}", 
            recipeId, category, ingredientCount);
        trackEvent(UsageStats.EVENT_RECIPE_CREATE, data);
    }
    
    public void trackSearch(String query, int resultCount, boolean isAdvanced) {
        String eventType = isAdvanced ? UsageStats.EVENT_SEARCH_ADVANCED : UsageStats.EVENT_SEARCH_QUERY;
    String data = String.format(java.util.Locale.ROOT, "{\"query\":\"%s\",\"results\":%d}", query, resultCount);
        trackEvent(eventType, data);
    }
    
    public void trackNutritionCalculation(String recipeId, double calories) {
    String data = String.format(java.util.Locale.ROOT, "{\"recipe_id\":\"%s\",\"calories\":%.1f}", recipeId, calories);
        trackEvent(UsageStats.EVENT_NUTRITION_CALCULATE, data);
    }
    
    public void trackUserInteraction(String interactionType, String targetId) {
    String data = String.format(java.util.Locale.ROOT, "{\"type\":\"%s\",\"target\":\"%s\"}", interactionType, targetId);
        trackEvent(UsageStats.EVENT_USER_FOLLOW, data);
    }
    
    // Génération des statistiques
    public void getStatistics(AnalyticsListener listener) {
        // Vérifier le cache
        long now = System.currentTimeMillis();
        if (cachedStats != null && (now - lastStatsUpdate) < STATS_CACHE_DURATION) {
            listener.onStatsUpdated(cachedStats);
            return;
        }
        
        executorService.execute(() -> {
            try {
                AppStatistics stats = generateStatistics();
                
                // Mettre à jour le cache
                cachedStats = stats;
                lastStatsUpdate = now;
                
                // Notifier le listener
                mainHandler.post(() -> listener.onStatsUpdated(stats));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private AppStatistics generateStatistics() {
        AppStatistics stats = new AppStatistics();
        
        // Statistiques générales
        stats.totalUsers = getTotalUsersCount();
        stats.totalRecipes = getTotalRecipesCount();
        stats.totalComments = getTotalCommentsCount();
        stats.totalSessions = getTotalSessionsCount();
        stats.totalUsageTime = getTotalUsageTime();
        
        // Utilisateurs actifs
        stats.todayActiveUsers = getActiveUsersCount(1);
        stats.weekActiveUsers = getActiveUsersCount(7);
        stats.monthActiveUsers = getActiveUsersCount(30);
        
        // Statistiques par période
        stats.dailyStats = convertMapToDataPoints(getDailyStats(30)); // 30 derniers jours
        stats.weeklyStats = convertMapToDataPoints(getWeeklyStats(12)); // 12 dernières semaines
        stats.monthlyStats = convertMapToDataPoints(getMonthlyStats(12)); // 12 derniers mois
        
        // Top événements
        stats.topEvents = getTopEvents(10);
        stats.topRecipes = getTopRecipes(10);
        stats.topCategories = getTopCategories();
        stats.topSearches = getTopSearchTerms(10);
        
        return stats;
    }
    
    // Analyses avancées
    public void generateEngagementReport(EngagementReportListener listener) {
        executorService.execute(() -> {
            try {
                List<AppStatistics.UserEngagement> engagementList = generateUserEngagementAnalysis();
                
                mainHandler.post(() -> listener.onEngagementReportReady(engagementList));
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> listener.onEngagementReportError(e.getMessage()));
            }
        });
    }
    
    public void generateTrendAnalysis(String metric, String period, TrendAnalysisListener listener) {
        executorService.execute(() -> {
            try {
                AppStatistics.TrendAnalysis analysis = generateTrendData(metric, period);
                
                mainHandler.post(() -> listener.onTrendAnalysisReady(analysis));
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> listener.onTrendAnalysisError(e.getMessage()));
            }
        });
    }
    
    public void generateInsights(InsightsListener listener) {
        executorService.execute(() -> {
            try {
                List<Insight> insights = generateAutomaticInsights();
                
                mainHandler.post(() -> listener.onInsightsReady(insights));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    // Génération d'insights automatiques
    private List<Insight> generateAutomaticInsights() {
        List<Insight> insights = new ArrayList<>();
        
        // Analyser les tendances d'utilisation
        AppStatistics stats = generateStatistics();
        
        // Insight sur les recettes populaires
        if (!stats.topRecipes.isEmpty()) {
            AppStatistics.RecipeStats topRecipe = stats.topRecipes.get(0);
            insights.add(new Insight(
                "Recette la plus populaire",
                String.format(java.util.Locale.ROOT, "'%s' est votre recette la plus consultée avec %d vues", 
                    topRecipe.recipeName, topRecipe.views),
                "info"
            ));
        }
        
        // Insight sur les catégories
        if (!stats.topCategories.isEmpty()) {
            AppStatistics.CategoryStats topCategory = stats.topCategories.get(0);
            insights.add(new Insight(
                "Catégorie préférée",
                String.format(java.util.Locale.ROOT, "'%s' représente %.1f%% de vos consultations", 
                    topCategory.category, topCategory.popularity),
                "trending"
            ));
        }
        
        // Insight sur l'engagement
        if (stats.weekActiveUsers > 0) {
            double engagementRate = (double) stats.weekActiveUsers / stats.totalUsers * 100;
            String level = engagementRate > 70 ? "excellent" : engagementRate > 50 ? "bon" : "modéré";
            insights.add(new Insight(
                "Taux d'engagement",
                String.format(java.util.Locale.ROOT, "%.1f%% de vos utilisateurs sont actifs cette semaine (%s)", 
                    engagementRate, level),
                engagementRate > 70 ? "positive" : "neutral"
            ));
        }
        
        // Insight sur les recherches
        if (!stats.topSearches.isEmpty()) {
            AppStatistics.SearchTerm topSearch = stats.topSearches.get(0);
            if (topSearch.successRate < 50) {
                insights.add(new Insight(
                    "Opportunité d'amélioration",
                    String.format(java.util.Locale.ROOT, "La recherche '%s' a un faible taux de succès (%.1f%%). " +
                        "Considérez ajouter plus de recettes dans cette catégorie.", 
                        topSearch.term, topSearch.successRate),
                    "warning"
                ));
            }
        }
        
        // Insight temporel
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 17 && hour <= 20) {
            insights.add(new Insight(
                "Pic d'activité",
                "C'est l'heure de pointe ! La plupart des utilisateurs consultent les recettes entre 17h et 20h",
                "info"
            ));
        }
        
        return insights;
    }
    
    // Classes pour les listeners
    public interface EngagementReportListener {
        void onEngagementReportReady(List<AppStatistics.UserEngagement> engagement);
        void onEngagementReportError(String error);
    }
    
    public interface TrendAnalysisListener {
        void onTrendAnalysisReady(AppStatistics.TrendAnalysis analysis);
        void onTrendAnalysisError(String error);
    }
    
    public interface InsightsListener {
        void onInsightsReady(List<Insight> insights);
    }
    
    // Classe pour les insights
    public static class Insight {
        public String title;
        public String description;
        public String type; // "info", "warning", "positive", "trending", "neutral"
        public Date createdAt;
        
        public Insight(String title, String description, String type) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.createdAt = new Date();
        }
    }
    
    // Méthodes utilitaires pour les données (à implémenter avec Room)
    private int getTotalUsersCount() {
        // TODO: Implémenter avec Room DAO
        return 0;
    }
    
    private int getTotalRecipesCount() {
        // TODO: Implémenter avec Room DAO
        return 0;
    }
    
    private int getTotalCommentsCount() {
        // TODO: Implémenter avec Room DAO
        return 0;
    }
    
    private int getTotalSessionsCount() {
        // TODO: Implémenter avec Room DAO
        return 0;
    }
    
    private long getTotalUsageTime() {
        // TODO: Calculer à partir des sessions
        return 0;
    }
    
    private int getActiveUsersCount(int days) {
        // TODO: Compter les utilisateurs actifs dans les X derniers jours
        return 0;
    }
    
    private Map<String, Integer> getDailyStats(int days) {
        // TODO: Implémenter statistiques quotidiennes
        return new HashMap<>();
    }
    
    private Map<String, Integer> getWeeklyStats(int weeks) {
        // TODO: Implémenter statistiques hebdomadaires
        return new HashMap<>();
    }
    
    private Map<String, Integer> getMonthlyStats(int months) {
        // TODO: Implémenter statistiques mensuelles
        return new HashMap<>();
    }
    
    private List<AppStatistics.EventCount> getTopEvents(int limit) {
        // TODO: Récupérer les événements les plus fréquents
        return new ArrayList<>();
    }
    
    private List<AppStatistics.RecipeStats> getTopRecipes(int limit) {
        // TODO: Calculer les recettes les plus populaires
        return new ArrayList<>();
    }
    
    private List<AppStatistics.CategoryStats> getTopCategories() {
        // TODO: Analyser la popularité par catégorie
        return new ArrayList<>();
    }
    
    private List<AppStatistics.SearchTerm> getTopSearchTerms(int limit) {
        // TODO: Récupérer les termes de recherche les plus fréquents
        return new ArrayList<>();
    }
    
    private List<AppStatistics.UserEngagement> generateUserEngagementAnalysis() {
        // TODO: Analyser l'engagement des utilisateurs
        return new ArrayList<>();
    }
    
    private AppStatistics.TrendAnalysis generateTrendData(String metric, String period) {
        // TODO: Générer l'analyse des tendances
        AppStatistics.TrendAnalysis analysis = new AppStatistics.TrendAnalysis();
        analysis.period = period;
        analysis.dataPoints = new ArrayList<>();
        
        // Générer des données de démonstration
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        for (int i = 0; i < 30; i++) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String date = dateFormat.format(cal.getTime());
            int value = (int) (Math.random() * 100) + 50; // Données aléatoires pour démo
            
            analysis.dataPoints.add(new AppStatistics.DataPoint(cal.getTime(), value, "Jour " + i));
        }
        
        Collections.reverse(analysis.dataPoints);
        analysis.analyzeTrend();
        
        return analysis;
    }
    
    private void saveUsageStatToDatabase(UsageStats stats) {
        // TODO: Implémenter avec Room DAO
    }
    
    private String getCurrentUserId() {
        return analyticsPrefs.getString("current_user_id", "anonymous");
    }
    
    private void loadConfiguration() {
        analyticsEnabled = analyticsPrefs.getBoolean("analytics_enabled", true);
    }
    
    // Gestion des listeners
    public void addListener(AnalyticsListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(AnalyticsListener listener) {
        listeners.remove(listener);
    }
    
    // Configuration
    public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    
    public void setAnalyticsEnabled(boolean enabled) {
        this.analyticsEnabled = enabled;
        analyticsPrefs.edit().putBoolean("analytics_enabled", enabled).apply();
    }
    
    public void clearAllData() {
        executorService.execute(() -> {
            // TODO: Supprimer toutes les données d'analytics
            cachedStats = null;
            lastStatsUpdate = 0;
        });
    }
    
    // Export des données
    public void exportStatistics(ExportListener listener) {
        executorService.execute(() -> {
            try {
                String csvData = generateCSVExport();
                mainHandler.post(() -> listener.onExportReady(csvData));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onExportError(e.getMessage()));
            }
        });
    }
    
    public interface ExportListener {
        void onExportReady(String csvData);
        void onExportError(String error);
    }
    
    private String generateCSVExport() {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Event Type,User ID,Data\n");
        
        // TODO: Récupérer toutes les stats et les formater en CSV
        
        return csv.toString();
    }
    
    /**
     * Convertit une Map<String,Integer> en List<DataPoint>
     */
    private List<AppStatistics.DataPoint> convertMapToDataPoints(Map<String, Integer> data) {
        List<AppStatistics.DataPoint> points = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            try {
                Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(entry.getKey());
                points.add(new AppStatistics.DataPoint(date, entry.getValue().doubleValue(), entry.getKey()));
            } catch (Exception e) {
                points.add(new AppStatistics.DataPoint(new Date(), entry.getValue().doubleValue(), entry.getKey()));
            }
        }
        return points;
    }
}
