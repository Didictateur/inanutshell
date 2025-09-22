package fr.didictateur.inanutshell.service;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.cache.CachedRecipe;
import fr.didictateur.inanutshell.data.cache.CachedRecipeDao;
import fr.didictateur.inanutshell.data.AppDatabase;
import fr.didictateur.inanutshell.data.model.Notification;

/**
 * Service de génération de suggestions de recettes intelligentes
 * Analyse les préférences utilisateur et propose des recettes adaptées
 */
public class RecipeSuggestionService {
    private static final String TAG = "RecipeSuggestionService";
    
    // Mots-clés pour différents types de suggestions
    private static final String[] QUICK_KEYWORDS = {
        "rapide", "express", "minutes", "facile", "simple", "instantané"
    };
    
    private static final String[] HEALTHY_KEYWORDS = {
        "sain", "léger", "fitness", "diet", "minceur", "végétarien", "vegan"
    };
    
    private static final String[] COMFORT_KEYWORDS = {
        "confort", "traditionnel", "familial", "grand-mère", "réconfortant"
    };
    
    private static final String[] SEASONAL_SPRING = {
        "printemps", "asperge", "radis", "petit pois", "frais"
    };
    
    private static final String[] SEASONAL_SUMMER = {
        "été", "tomate", "courgette", "salade", "grillé", "barbecue"
    };
    
    private static final String[] SEASONAL_AUTUMN = {
        "automne", "potiron", "champignon", "châtaigne", "pomme"
    };
    
    private static final String[] SEASONAL_WINTER = {
        "hiver", "soupe", "ragoût", "gratin", "chaud", "mijotée"
    };
    
    private final Context context;
    private final CachedRecipeDao cachedRecipeDao;
    private final NotificationService notificationService;
    private final ExecutorService executorService;
    private final Random random;
    
    public RecipeSuggestionService(Context context) {
        this.context = context;
        this.cachedRecipeDao = AppDatabase.getInstance(context).cachedRecipeDao();
        this.notificationService = new NotificationService(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.random = new Random();
    }
    
    /**
     * Génère des suggestions selon les préférences utilisateur
     */
    public void generatePersonalizedSuggestions(List<String> preferredCategories, 
                                              List<String> dietaryRestrictions) {
        executorService.execute(() -> {
            try {
                List<CachedRecipe> suggestions = getRecipesByPreferences(
                    preferredCategories, dietaryRestrictions);
                
                if (!suggestions.isEmpty()) {
                    CachedRecipe recipe = suggestions.get(random.nextInt(suggestions.size()));
                    String message = generatePersonalizedMessage(recipe, preferredCategories);
                    createSuggestionNotification(recipe, message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de suggestions personnalisées", e);
            }
        });
    }
    
    /**
     * Génère des suggestions selon l'heure du jour
     */
    public void generateTimeBasedSuggestions() {
        executorService.execute(() -> {
            try {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                
                String[] keywords = getTimeBasedKeywords(hour);
                List<CachedRecipe> suggestions = getRecipesByKeywords(keywords);
                
                if (!suggestions.isEmpty()) {
                    CachedRecipe recipe = suggestions.get(random.nextInt(suggestions.size()));
                    String message = generateTimeBasedMessage(hour, recipe);
                    createSuggestionNotification(recipe, message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de suggestions temporelles", e);
            }
        });
    }
    
    /**
     * Recherche de recettes par mots-clés
     */
    private List<CachedRecipe> getRecipesByKeywords(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            // Recettes rapides par défaut
            keywords = QUICK_KEYWORDS;
        }
        
        List<CachedRecipe> allRecipes = cachedRecipeDao.getAllCachedRecipes();
        List<CachedRecipe> matchingRecipes = new ArrayList<>();
        
        for (CachedRecipe recipe : allRecipes) {
            if (recipeMatchesKeywords(recipe, keywords)) {
                matchingRecipes.add(recipe);
            }
        }
        
        return matchingRecipes;
    }
    
    /**
     * Vérifie si une recette correspond aux mots-clés
     */
    private boolean recipeMatchesKeywords(CachedRecipe recipe, String[] keywords) {
        String searchText = (recipe.name + " " + recipe.description + " " + 
                           recipe.recipeCategoryJson).toLowerCase();
        
        for (String keyword : keywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtient des mots-clés selon l'heure
     */
    private String[] getTimeBasedKeywords(int hour) {
        if (hour >= 6 && hour < 10) {
            // Petit-déjeuner
            return new String[]{"petit-déjeuner", "matin", "croissant", "tartine", "café"};
        } else if (hour >= 11 && hour < 15) {
            // Déjeuner
            return new String[]{"déjeuner", "salade", "sandwich", "plat principal"};
        } else if (hour >= 16 && hour < 19) {
            // Goûter
            return new String[]{"goûter", "gâteau", "biscuit", "fruit", "yaourt"};
        } else if (hour >= 19 && hour < 22) {
            // Dîner
            return new String[]{"dîner", "souper", "plat du soir", "léger"};
        } else {
            // Collation nocturne
            return new String[]{"collation", "léger", "tisane", "fruit"};
        }
    }
    
    /**
     * Récupère les recettes populaires
     */
    private List<CachedRecipe> getPopularRecipes() {
        List<CachedRecipe> allRecipes = cachedRecipeDao.getAllCachedRecipes();
        
        // Trier par note décroissante (utiliser un score par défaut)
        allRecipes.sort((r1, r2) -> r1.name.compareTo(r2.name)); // Tri par nom par défaut
        
        // Prendre les 10 meilleures
        return allRecipes.subList(0, Math.min(10, allRecipes.size()));
    }
    
    /**
     * Génère des suggestions saisonnières
     */
    public void generateSeasonalSuggestions() {
        executorService.execute(() -> {
            try {
                Calendar calendar = Calendar.getInstance();
                int month = calendar.get(Calendar.MONTH);
                
                String[] seasonalKeywords = getSeasonalKeywords(month);
                
                List<CachedRecipe> allRecipes = cachedRecipeDao.getAllCachedRecipes();
                List<CachedRecipe> seasonalRecipes = new ArrayList<>();
                
                for (CachedRecipe recipe : allRecipes) {
                    if (recipeMatchesKeywords(recipe, seasonalKeywords)) {
                        seasonalRecipes.add(recipe);
                    }
                }
                
                if (!seasonalRecipes.isEmpty()) {
                    CachedRecipe recipe = seasonalRecipes.get(random.nextInt(seasonalRecipes.size()));
                    String season = getSeasonName(month);
                    String message = "Recette de saison parfaite pour " + season + " !";
                    createSuggestionNotification(recipe, message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de suggestions saisonnières", e);
            }
        });
    }
    
    /**
     * Obtient les mots-clés saisonniers selon le mois
     */
    private String[] getSeasonalKeywords(int month) {
        if (month >= 2 && month <= 4) { // Mars, Avril, Mai
            return SEASONAL_SPRING;
        } else if (month >= 5 && month <= 7) { // Juin, Juillet, Août
            return SEASONAL_SUMMER;
        } else if (month >= 8 && month <= 10) { // Septembre, Octobre, Novembre
            return SEASONAL_AUTUMN;
        } else { // Décembre, Janvier, Février
            return SEASONAL_WINTER;
        }
    }
    
    /**
     * Obtient le nom de la saison selon le mois
     */
    private String getSeasonName(int month) {
        if (month >= 2 && month <= 4) {
            return "le printemps";
        } else if (month >= 5 && month <= 7) {
            return "l'été";
        } else if (month >= 8 && month <= 10) {
            return "l'automne";
        } else {
            return "l'hiver";
        }
    }
    
    /**
     * Génère des suggestions de nouvelles recettes
     */
    public void generateNewRecipeSuggestions() {
        executorService.execute(() -> {
            try {
                // Simuler de nouvelles recettes (dernières ajoutées)
                Date weekAgo = new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000));
                List<CachedRecipe> recentRecipes = getRecipesSince(weekAgo);
                
                if (!recentRecipes.isEmpty()) {
                    for (CachedRecipe recipe : recentRecipes) {
                        String message = "Nouvelle recette disponible ! Découvre \"" + recipe.name + "\"";
                        createNewRecipeNotification(recipe, message);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de suggestions de nouvelles recettes", e);
            }
        });
    }
    
    /**
     * Récupère les recettes ajoutées depuis une date
     */
    private List<CachedRecipe> getRecipesSince(Date since) {
        // Comme nous n'avons pas de date de création dans CachedRecipe, 
        // on simule en prenant les dernières recettes ajoutées
        List<CachedRecipe> allRecipes = cachedRecipeDao.getAllCachedRecipes();
        Collections.reverse(allRecipes); // Les plus récentes en premier
        
        // Prendre les 5 dernières
        return allRecipes.subList(0, Math.min(5, allRecipes.size()));
    }
    
    /**
     * Obtient des recettes selon les préférences
     */
    public List<CachedRecipe> getRecipesByPreferences(List<String> preferredCategories, 
                                                    List<String> dietaryRestrictions) {
        // Implémentation simplifiée - chercher par catégories
        List<CachedRecipe> allRecipes = cachedRecipeDao.getAllCachedRecipes();
        List<CachedRecipe> filteredRecipes = new ArrayList<>();
        
        for (CachedRecipe recipe : allRecipes) {
            boolean matches = false;
            
            // Vérifier les catégories préférées
            if (preferredCategories != null && !preferredCategories.isEmpty()) {
                for (String category : preferredCategories) {
                    if (recipe.recipeCategoryJson != null && 
                        recipe.recipeCategoryJson.toLowerCase().contains(category.toLowerCase())) {
                        matches = true;
                        break;
                    }
                }
            } else {
                matches = true; // Si pas de préférence, toutes les recettes conviennent
            }
            
            // Exclure selon les restrictions alimentaires
            if (matches && dietaryRestrictions != null && !dietaryRestrictions.isEmpty()) {
                for (String restriction : dietaryRestrictions) {
                    if (recipe.description != null && 
                        recipe.description.toLowerCase().contains(restriction.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
            }
            
            if (matches) {
                filteredRecipes.add(recipe);
            }
        }
        
        return filteredRecipes;
    }
    
    /**
     * Génère un message personnalisé pour une recette
     */
    private String generatePersonalizedMessage(CachedRecipe recipe, List<String> preferences) {
        String[] messages = {
            "Une recette parfaite pour tes goûts !",
            "Découvre cette délicieuse recette qui devrait te plaire !",
            "Une suggestion spécialement pour toi !",
            "Cette recette correspond exactement à ce que tu aimes !",
            "Tu vas adorer cette nouvelle découverte !"
        };
        
        return messages[random.nextInt(messages.length)];
    }
    
    /**
     * Génère un message basé sur l'heure
     */
    private String generateTimeBasedMessage(int hour, CachedRecipe recipe) {
        if (hour >= 6 && hour < 10) {
            return "Commence bien ta journée avec cette délicieuse recette !";
        } else if (hour >= 11 && hour < 15) {
            return "L'heure du déjeuner approche, que dirais-tu de cette recette ?";
        } else if (hour >= 16 && hour < 19) {
            return "Une petite pause gourmande avec cette recette ?";
        } else if (hour >= 19 && hour < 22) {
            return "Pour un dîner parfait, essaie cette recette !";
        } else {
            return "Une petite collation ? Cette recette pourrait t'intéresser !";
        }
    }
    
    /**
     * Crée une notification de suggestion
     */
    private void createSuggestionNotification(CachedRecipe recipe, String message) {
        Notification notification = new Notification(
            "Suggestion : " + recipe.name,
            message,
            Notification.NotificationType.RECIPE_SUGGESTION
        );
        
        notification.setPriority(Notification.NotificationPriority.NORMAL);
        notification.setActionData(recipe.id);
        
        notificationService.showNotification(notification);
    }
    
    /**
     * Crée une notification de nouvelle recette
     */
    private void createNewRecipeNotification(CachedRecipe recipe, String message) {
        Notification notification = new Notification(
            recipe.name,
            message,
            Notification.NotificationType.NEW_RECIPE
        );
        
        notification.setPriority(Notification.NotificationPriority.NORMAL);
        notification.setActionData(recipe.id);
        
        notificationService.showNotification(notification);
    }
    
    /**
     * Génère des conseils de cuisine
     */
    public void generateCookingTips() {
        executorService.execute(() -> {
            try {
                String[] tips = {
                    "Conseil du jour : Toujours préchauffer le four pour une cuisson optimale !",
                    "Astuce cuisine : Saler l'eau de cuisson des pâtes pour plus de saveur !",
                    "Conseil : Laisse reposer la viande quelques minutes après cuisson.",
                    "Astuce : Utilise du papier sulfurisé pour éviter que ça colle !",
                    "Conseil : Goûte et ajuste l'assaisonnement en fin de cuisson.",
                    "Astuce : Conserve les herbes fraîches dans un verre d'eau au frigo.",
                    "Conseil : Organise tes ingrédients avant de commencer à cuisiner !",
                    "Astuce : Un couteau bien aiguisé est plus sûr qu'un couteau émoussé."
                };
                
                String tip = tips[random.nextInt(tips.length)];
                
                Notification notification = new Notification(
                    "💡 Conseil Cuisine",
                    tip,
                    Notification.NotificationType.COOKING_TIP
                );
                
                notification.setPriority(Notification.NotificationPriority.LOW);
                
                notificationService.showNotification(notification);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de conseils de cuisine", e);
            }
        });
    }
    
    /**
     * Programme des suggestions automatiques périodiques
     */
    public void scheduleAutomaticSuggestions() {
        // Suggestions basées sur l'heure toutes les 3 heures
        executorService.execute(() -> {
            try {
                Thread.sleep(3 * 60 * 60 * 1000); // 3 heures
                generateTimeBasedSuggestions();
            } catch (InterruptedException e) {
                Log.d(TAG, "Suggestions automatiques interrompues");
            }
        });
        
        // Conseils de cuisine quotidiens
        executorService.execute(() -> {
            try {
                Thread.sleep(24 * 60 * 60 * 1000); // 24 heures  
                generateCookingTips();
            } catch (InterruptedException e) {
                Log.d(TAG, "Conseils de cuisine interrompus");
            }
        });
        
        // Suggestions saisonnières hebdomadaires
        executorService.execute(() -> {
            try {
                Thread.sleep(7 * 24 * 60 * 60 * 1000); // 1 semaine
                generateSeasonalSuggestions();
            } catch (InterruptedException e) {
                Log.d(TAG, "Suggestions saisonnières interrompues");
            }
        });
    }
    
    /**
     * Nettoie les ressources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}