package fr.didictateur.inanutshell.service;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.data.model.NutritionFacts;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;

public class NutritionCalculator {
    private static NutritionCalculator instance;
    private Context context;
    private ExecutorService executorService;
    
    // Base de données nutritionnelle simplifiée
    private Map<String, FoodNutrition> nutritionDatabase = new HashMap<>();
    
    // Cache des analyses nutritionnelles
    private Map<String, NutritionFacts> nutritionCache = new HashMap<>();
    private MutableLiveData<NutritionFacts> currentNutritionLiveData = new MutableLiveData<>();
    
    public static class FoodNutrition {
        public String foodId;
        public String name;
        public double caloriesPer100g;
        public double fatPer100g;
        public double saturatedFatPer100g;
        public double carbsPer100g;
        public double fiberPer100g;
        public double sugarsPer100g;
        public double proteinPer100g;
        public double sodiumPer100g;
        public double cholesterolPer100g;
        
        // Vitamines et minéraux (% VQ par 100g)
        public double vitaminA;
        public double vitaminC;
        public double calcium;
        public double iron;
        
        public FoodNutrition(String name) {
            this.foodId = name.toLowerCase().replaceAll("\\s+", "_");
            this.name = name;
        }
    }
    
    public enum NutritionAnalysisLevel {
        BASIC,      // Macronutriments principaux seulement
        STANDARD,   // Macronutriments + vitamines principales
        COMPLETE    // Analyse complète avec tous les micronutriments
    }
    
    private NutritionCalculator(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        initializeNutritionDatabase();
    }
    
    public static synchronized NutritionCalculator getInstance(Context context) {
        if (instance == null) {
            instance = new NutritionCalculator(context);
        }
        return instance;
    }
    
    private void initializeNutritionDatabase() {
        // Initialiser une base de données nutritionnelle de base
        // En production, ceci serait chargé depuis une vraie DB ou API
        
        // Légumes
        addFood("tomate", 18, 0.2, 0.0, 3.9, 1.2, 2.6, 0.9, 5, 0);
        addFood("oignon", 40, 0.1, 0.0, 9.3, 1.7, 4.2, 1.1, 4, 0);
        addFood("carotte", 41, 0.2, 0.0, 9.6, 2.8, 4.7, 0.9, 69, 0);
        addFood("pomme de terre", 77, 0.1, 0.0, 17.5, 2.2, 0.8, 2.0, 6, 0);
        
        // Viandes
        addFood("poulet", 165, 3.6, 1.0, 0, 0, 0, 31, 74, 85);
        addFood("bœuf", 250, 15, 6.0, 0, 0, 0, 26, 72, 90);
        addFood("porc", 242, 14, 5.0, 0, 0, 0, 27, 62, 80);
        
        // Poissons
        addFood("saumon", 208, 12, 3.4, 0, 0, 0, 22, 59, 60);
        addFood("thon", 144, 4.9, 1.3, 0, 0, 0, 23, 39, 38);
        
        // Céréales et légumineuses
        addFood("riz", 130, 0.3, 0.1, 28, 0.4, 0.1, 2.7, 5, 0);
        addFood("pâtes", 131, 1.1, 0.2, 25, 1.8, 0.6, 5.0, 6, 0);
        addFood("lentilles", 116, 0.4, 0.1, 20, 7.9, 1.8, 9.0, 2, 0);
        addFood("haricots", 127, 0.5, 0.1, 23, 6.4, 0.3, 8.7, 2, 0);
        
        // Produits laitiers
        addFood("lait", 42, 1.0, 0.6, 5.0, 0, 5.0, 3.4, 44, 5);
        addFood("yaourt", 59, 0.4, 0.3, 3.6, 0, 3.2, 10, 52, 5);
        addFood("fromage", 402, 33, 21, 1.3, 0, 0.5, 25, 621, 105);
        
        // Matières grasses
        addFood("huile d'olive", 884, 100, 13.8, 0, 0, 0, 0, 2, 0);
        addFood("beurre", 717, 81, 51, 0.1, 0, 0.1, 0.9, 11, 215);
        
        // Fruits
        addFood("pomme", 52, 0.2, 0.0, 14, 2.4, 10, 0.3, 1, 0);
        addFood("banane", 89, 0.3, 0.1, 23, 2.6, 12, 1.1, 1, 0);
        addFood("orange", 47, 0.1, 0.0, 12, 2.4, 9.4, 0.9, 0, 0);
    }
    
    private void addFood(String name, double calories, double fat, double saturatedFat, 
                        double carbs, double fiber, double sugars, double protein, 
                        double sodium, double cholesterol) {
        FoodNutrition food = new FoodNutrition(name);
        food.caloriesPer100g = calories;
        food.fatPer100g = fat;
        food.saturatedFatPer100g = saturatedFat;
        food.carbsPer100g = carbs;
        food.fiberPer100g = fiber;
        food.sugarsPer100g = sugars;
        food.proteinPer100g = protein;
        food.sodiumPer100g = sodium;
        food.cholesterolPer100g = cholesterol;
        
        nutritionDatabase.put(food.foodId, food);
    }
    
    public void calculateNutrition(Recipe recipe, NutritionAnalysisLevel level, NutritionCallback callback) {
        executorService.execute(() -> {
            try {
                // Vérifier le cache d'abord
                String cacheKey = recipe.getId() + "_" + level.name();
                NutritionFacts cached = nutritionCache.get(cacheKey);
                if (cached != null && isRecentCalculation(cached)) {
                    currentNutritionLiveData.postValue(cached);
                    if (callback != null) {
                        callback.onSuccess(cached);
                    }
                    return;
                }
                
                NutritionFacts nutrition = performNutritionCalculation(recipe, level);
                
                // Mettre à jour le cache
                nutritionCache.put(cacheKey, nutrition);
                currentNutritionLiveData.postValue(nutrition);
                
                // Sauvegarder en base de données
                saveNutritionFacts(nutrition);
                
                if (callback != null) {
                    callback.onSuccess(nutrition);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors du calcul nutritionnel: " + e.getMessage());
                }
            }
        });
    }
    
    private NutritionFacts performNutritionCalculation(Recipe recipe, NutritionAnalysisLevel level) {
        NutritionFacts facts = new NutritionFacts(recipe.getId(), parseServings(recipe.getRecipeYield()));
        facts.setCalculationMethod("Automatic calculation - " + level.name());
        facts.setDataSource("Internal database");
        
        if (recipe.getRecipeIngredient() == null || recipe.getRecipeIngredient().isEmpty()) {
            return facts; // Retourner des valeurs vides si pas d'ingrédients
        }
        
        double totalCalories = 0;
        double totalFat = 0;
        double totalSaturatedFat = 0;
        double totalCarbs = 0;
        double totalFiber = 0;
        double totalSugars = 0;
        double totalProtein = 0;
        double totalSodium = 0;
        double totalCholesterol = 0;
        
        for (RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
            NutrientContribution contribution = calculateIngredientContribution(ingredient, level);
            
            totalCalories += contribution.calories;
            totalFat += contribution.fat;
            totalSaturatedFat += contribution.saturatedFat;
            totalCarbs += contribution.carbs;
            totalFiber += contribution.fiber;
            totalSugars += contribution.sugars;
            totalProtein += contribution.protein;
            totalSodium += contribution.sodium;
            totalCholesterol += contribution.cholesterol;
        }
        
        // Diviser par le nombre de portions pour avoir les valeurs par portion
        int servings = facts.getServings();
        if (servings > 0) {
            facts.setCalories(totalCalories / servings);
            facts.setTotalFat(totalFat / servings);
            facts.setSaturatedFat(totalSaturatedFat / servings);
            facts.setTotalCarbs(totalCarbs / servings);
            facts.setFiber(totalFiber / servings);
            facts.setSugars(totalSugars / servings);
            facts.setProtein(totalProtein / servings);
            facts.setSodium(totalSodium / servings);
            facts.setCholesterol(totalCholesterol / servings);
        }
        
        facts.setVerified(false); // Calcul automatique, pas vérifié manuellement
        
        return facts;
    }
    
    private static class NutrientContribution {
        public double calories = 0;
        public double fat = 0;
        public double saturatedFat = 0;
        public double carbs = 0;
        public double fiber = 0;
        public double sugars = 0;
        public double protein = 0;
        public double sodium = 0;
        public double cholesterol = 0;
    }
    
    private NutrientContribution calculateIngredientContribution(RecipeIngredient ingredient, NutritionAnalysisLevel level) {
        NutrientContribution contribution = new NutrientContribution();
        
        // Extraire la quantité et l'unité de l'ingrédient
        IngredientQuantity quantity = parseIngredientQuantity(ingredient.getDisplay());
        
        // Trouver les données nutritionnelles pour cet ingrédient
        FoodNutrition foodData = findFoodNutrition(quantity.foodName);
        
        if (foodData != null && quantity.amountInGrams > 0) {
            double ratio = quantity.amountInGrams / 100.0; // Base de données en pour 100g
            
            contribution.calories = foodData.caloriesPer100g * ratio;
            contribution.fat = foodData.fatPer100g * ratio;
            contribution.saturatedFat = foodData.saturatedFatPer100g * ratio;
            contribution.carbs = foodData.carbsPer100g * ratio;
            contribution.fiber = foodData.fiberPer100g * ratio;
            contribution.sugars = foodData.sugarsPer100g * ratio;
            contribution.protein = foodData.proteinPer100g * ratio;
            contribution.sodium = foodData.sodiumPer100g * ratio;
            contribution.cholesterol = foodData.cholesterolPer100g * ratio;
        }
        
        return contribution;
    }
    
    /**
     * Parse une chaîne représentant un ingrédient avec quantité
     */
    private IngredientQuantity parseIngredientQuantity(String ingredientText) {
        return new IngredientQuantity(ingredientText);
    }
    
    private static class IngredientQuantity {
        public String foodName;
        public double amountInGrams;
        public String originalText;
        
        public IngredientQuantity(String text) {
            this.originalText = text;
            parseQuantity();
        }
        
        private void parseQuantity() {
            // Parser simple pour extraire quantité et nom de l'aliment
            // Exemples: "2 pommes", "300g de riz", "1 cuillère à soupe d'huile"
            
            String text = originalText.toLowerCase().trim();
            
            // Patterns courants
            if (text.matches("\\d+\\s*g\\s+.*")) {
                // Format: "300g de riz"
                String[] parts = text.split("\\s+", 3);
                amountInGrams = Double.parseDouble(parts[0].replaceAll("[^\\d.]", ""));
                foodName = parts.length > 2 ? parts[2] : "";
            } else if (text.matches("\\d+\\s*kg\\s+.*")) {
                // Format: "1kg de pommes de terre"
                String[] parts = text.split("\\s+", 3);
                amountInGrams = Double.parseDouble(parts[0].replaceAll("[^\\d.]", "")) * 1000;
                foodName = parts.length > 2 ? parts[2] : "";
            } else if (text.matches("\\d+\\s+.*")) {
                // Format: "2 pommes" - estimer le poids
                String[] parts = text.split("\\s+", 2);
                int count = Integer.parseInt(parts[0]);
                foodName = parts.length > 1 ? parts[1] : "";
                amountInGrams = estimateWeightFromCount(foodName, count);
            } else {
                // Par défaut, assumer 100g
                foodName = text;
                amountInGrams = 100;
            }
            
            // Nettoyer le nom de l'aliment
            foodName = cleanFoodName(foodName);
        }
        
        private double estimateWeightFromCount(String foodName, int count) {
            // Estimations de poids pour les aliments courants
            Map<String, Double> weights = new HashMap<>();
            weights.put("pomme", 150.0);
            weights.put("banane", 120.0);
            weights.put("orange", 180.0);
            weights.put("tomate", 100.0);
            weights.put("oignon", 80.0);
            weights.put("carotte", 60.0);
            weights.put("œuf", 50.0);
            
            Double estimatedWeight = weights.get(foodName.toLowerCase());
            return (estimatedWeight != null ? estimatedWeight : 100.0) * count;
        }
        
        private String cleanFoodName(String name) {
            // Supprimer les mots de liaison et articles
            return name.replaceAll("\\b(de|du|des|le|la|les|d'|l')\\s+", "")
                      .replaceAll("\\s+", " ")
                      .trim();
        }
    }
    
    private FoodNutrition findFoodNutrition(String foodName) {
        // Recherche exacte d'abord
        String key = foodName.toLowerCase().replaceAll("\\s+", "_");
        FoodNutrition exact = nutritionDatabase.get(key);
        if (exact != null) {
            return exact;
        }
        
        // Recherche par similarité/mots-clés
        for (Map.Entry<String, FoodNutrition> entry : nutritionDatabase.entrySet()) {
            if (entry.getKey().contains(key) || key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Recherche par mots partiels
        String[] words = foodName.toLowerCase().split("\\s+");
        for (String word : words) {
            for (Map.Entry<String, FoodNutrition> entry : nutritionDatabase.entrySet()) {
                if (entry.getKey().contains(word) || entry.getValue().name.toLowerCase().contains(word)) {
                    return entry.getValue();
                }
            }
        }
        
        return null; // Pas trouvé
    }
    
    private int parseServings(String recipeYield) {
        if (recipeYield == null || recipeYield.isEmpty()) {
            return 4; // Valeur par défaut
        }
        
        try {
            // Extraire le nombre du texte (ex: "4 portions" -> 4)
            String number = recipeYield.replaceAll("[^\\d]", "");
            if (!number.isEmpty()) {
                return Integer.parseInt(number);
            }
        } catch (NumberFormatException e) {
            // Ignorer et utiliser la valeur par défaut
        }
        
        return 4; // Valeur par défaut
    }
    
    private boolean isRecentCalculation(NutritionFacts facts) {
        long oneDay = 24 * 60 * 60 * 1000; // 1 jour en millisecondes
        return (System.currentTimeMillis() - facts.getLastUpdated()) < oneDay;
    }
    
    public LiveData<NutritionFacts> getCurrentNutritionLiveData() {
        return currentNutritionLiveData;
    }
    
    public void addCustomFoodData(String name, FoodNutrition nutrition, NutritionCallback callback) {
        executorService.execute(() -> {
            try {
                nutritionDatabase.put(nutrition.foodId, nutrition);
                
                // Sauvegarder dans la base de données personnalisée
                saveCustomFoodData(nutrition);
                
                if (callback != null) {
                    NutritionFacts facts = new NutritionFacts();
                    callback.onSuccess(facts);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de l'ajout de l'aliment: " + e.getMessage());
                }
            }
        });
    }
    
    public NutritionFacts scaleNutritionToServings(NutritionFacts original, int newServings) {
        if (original == null) return null;
        return original.scaleToServings(newServings);
    }
    
    public String generateNutritionReport(NutritionFacts facts) {
        if (facts == null) return "Aucune donnée nutritionnelle disponible.";
        
        StringBuilder report = new StringBuilder();
        report.append("=== ANALYSE NUTRITIONNELLE ===\n\n");
        report.append(String.format("Portions: %d\n", facts.getServings()));
        report.append(String.format("Calories par portion: %.0f\n", facts.getCalories()));
        report.append(String.format("Note nutritionnelle: %s\n\n", facts.getNutritionGrade()));
        
        report.append("MACRONUTRIMENTS (par portion):\n");
        report.append(String.format("• Lipides: %.1fg (%.0f%% des calories)\n", 
            facts.getTotalFat(), facts.getFatPercentage()));
        report.append(String.format("• Glucides: %.1fg (%.0f%% des calories)\n", 
            facts.getTotalCarbs(), facts.getCarbsPercentage()));
        report.append(String.format("• Protéines: %.1fg (%.0f%% des calories)\n", 
            facts.getProtein(), facts.getProteinPercentage()));
        report.append(String.format("• Fibres: %.1fg\n", facts.getFiber()));
        
        report.append("\nAUTRES NUTRIMENTS:\n");
        report.append(String.format("• Sucres: %.1fg\n", facts.getSugars()));
        report.append(String.format("• Sodium: %.0fmg\n", facts.getSodium()));
        report.append(String.format("• Cholestérol: %.0fmg\n", facts.getCholesterol()));
        
        report.append("\nINDICATEURS SANTÉ:\n");
        if (facts.isGoodSourceOfProtein()) {
            report.append("✓ Bonne source de protéines\n");
        }
        if (facts.isGoodSourceOfFiber()) {
            report.append("✓ Bonne source de fibres\n");
        }
        if (facts.isHighInSodium()) {
            report.append("⚠ Riche en sodium\n");
        }
        if (facts.isHighInSaturatedFat()) {
            report.append("⚠ Riche en graisses saturées\n");
        }
        if (facts.isHighInSugars()) {
            report.append("⚠ Riche en sucres\n");
        }
        
        report.append(String.format("\nCalculé le: %s\n", 
            new java.util.Date(facts.getLastUpdated()).toString()));
        
        return report.toString();
    }
    
    // Méthodes de persistance (à implémenter)
    private void saveNutritionFacts(NutritionFacts facts) {
        // TODO: Sauvegarder en base de données
    }
    
    private void saveCustomFoodData(FoodNutrition food) {
        // TODO: Sauvegarder en base de données
    }
    
    // Interface pour les callbacks
    public interface NutritionCallback {
        void onSuccess(NutritionFacts nutrition);
        void onError(String message);
    }
}
