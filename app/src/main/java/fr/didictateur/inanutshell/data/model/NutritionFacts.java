package fr.didictateur.inanutshell.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "nutrition_facts")
public class NutritionFacts implements Serializable {
    
    @PrimaryKey
    private String id;
    
    private String recipeId;
    private int servings;
    
    // Macronutriments (par portion)
    private double calories;
    private double totalFat;      // g
    private double saturatedFat;  // g
    private double transFat;      // g
    private double cholesterol;   // mg
    private double sodium;        // mg
    private double totalCarbs;    // g
    private double fiber;         // g
    private double sugars;        // g
    private double addedSugars;   // g
    private double protein;       // g
    
    // Vitamines (% valeur quotidienne recommandée)
    private double vitaminA;      // %
    private double vitaminC;      // %
    private double vitaminD;      // %
    private double vitaminE;      // %
    private double vitaminK;      // %
    private double thiamin;       // %
    private double riboflavin;    // %
    private double niacin;        // %
    private double vitaminB6;     // %
    private double folate;        // %
    private double vitaminB12;    // %
    
    // Minéraux (% valeur quotidienne recommandée)
    private double calcium;       // %
    private double iron;          // %
    private double magnesium;     // %
    private double phosphorus;    // %
    private double potassium;     // %
    private double zinc;          // %
    
    // Métadonnées
    private String calculationMethod;
    private long lastUpdated;
    private boolean verified;
    private String dataSource;
    
    public NutritionFacts() {
        this.id = java.util.UUID.randomUUID().toString();
        this.lastUpdated = System.currentTimeMillis();
        this.verified = false;
        this.servings = 1;
    }
    
    public NutritionFacts(String recipeId, int servings) {
        this();
        this.recipeId = recipeId;
        this.servings = servings;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    
    public int getServings() { return servings; }
    public void setServings(int servings) { 
        if (servings > 0) {
            this.servings = servings;
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    // Macronutriments
    public double getCalories() { return calories; }
    public void setCalories(double calories) { 
        this.calories = Math.max(0, calories);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getTotalFat() { return totalFat; }
    public void setTotalFat(double totalFat) { 
        this.totalFat = Math.max(0, totalFat);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getSaturatedFat() { return saturatedFat; }
    public void setSaturatedFat(double saturatedFat) { 
        this.saturatedFat = Math.max(0, saturatedFat);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getTransFat() { return transFat; }
    public void setTransFat(double transFat) { 
        this.transFat = Math.max(0, transFat);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getCholesterol() { return cholesterol; }
    public void setCholesterol(double cholesterol) { 
        this.cholesterol = Math.max(0, cholesterol);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getSodium() { return sodium; }
    public void setSodium(double sodium) { 
        this.sodium = Math.max(0, sodium);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getTotalCarbs() { return totalCarbs; }
    public void setTotalCarbs(double totalCarbs) { 
        this.totalCarbs = Math.max(0, totalCarbs);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { 
        this.fiber = Math.max(0, fiber);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getSugars() { return sugars; }
    public void setSugars(double sugars) { 
        this.sugars = Math.max(0, sugars);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getAddedSugars() { return addedSugars; }
    public void setAddedSugars(double addedSugars) { 
        this.addedSugars = Math.max(0, addedSugars);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public double getProtein() { return protein; }
    public void setProtein(double protein) { 
        this.protein = Math.max(0, protein);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Vitamines
    public double getVitaminA() { return vitaminA; }
    public void setVitaminA(double vitaminA) { this.vitaminA = Math.max(0, vitaminA); }
    
    public double getVitaminC() { return vitaminC; }
    public void setVitaminC(double vitaminC) { this.vitaminC = Math.max(0, vitaminC); }
    
    public double getVitaminD() { return vitaminD; }
    public void setVitaminD(double vitaminD) { this.vitaminD = Math.max(0, vitaminD); }
    
    // ... (autres vitamines suivent le même pattern)
    
    // Minéraux
    public double getCalcium() { return calcium; }
    public void setCalcium(double calcium) { this.calcium = Math.max(0, calcium); }
    
    public double getIron() { return iron; }
    public void setIron(double iron) { this.iron = Math.max(0, iron); }
    
    // ... (autres minéraux suivent le même pattern)
    
    // Métadonnées
    public String getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(String calculationMethod) { this.calculationMethod = calculationMethod; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    // Méthodes utilitaires
    public double getCaloriesFromFat() {
        return totalFat * 9; // 9 calories par gramme de graisse
    }
    
    public double getCaloriesFromCarbs() {
        return totalCarbs * 4; // 4 calories par gramme de glucides
    }
    
    public double getCaloriesFromProtein() {
        return protein * 4; // 4 calories par gramme de protéines
    }
    
    public double getFatPercentage() {
        if (calories == 0) return 0;
        return (getCaloriesFromFat() / calories) * 100;
    }
    
    public double getCarbsPercentage() {
        if (calories == 0) return 0;
        return (getCaloriesFromCarbs() / calories) * 100;
    }
    
    public double getProteinPercentage() {
        if (calories == 0) return 0;
        return (getCaloriesFromProtein() / calories) * 100;
    }
    
    public NutritionFacts scaleToServings(int newServings) {
        if (newServings <= 0 || servings <= 0) return this;
        
        double ratio = (double) newServings / servings;
        NutritionFacts scaled = new NutritionFacts();
        scaled.recipeId = this.recipeId;
        scaled.servings = newServings;
        
        // Multiplier tous les nutriments par le ratio
        scaled.calories = this.calories * ratio;
        scaled.totalFat = this.totalFat * ratio;
        scaled.saturatedFat = this.saturatedFat * ratio;
        scaled.transFat = this.transFat * ratio;
        scaled.cholesterol = this.cholesterol * ratio;
        scaled.sodium = this.sodium * ratio;
        scaled.totalCarbs = this.totalCarbs * ratio;
        scaled.fiber = this.fiber * ratio;
        scaled.sugars = this.sugars * ratio;
        scaled.addedSugars = this.addedSugars * ratio;
        scaled.protein = this.protein * ratio;
        
        // Les vitamines et minéraux sont aussi multipliés
        scaled.vitaminA = this.vitaminA * ratio;
        scaled.vitaminC = this.vitaminC * ratio;
        scaled.calcium = this.calcium * ratio;
        scaled.iron = this.iron * ratio;
        
        scaled.calculationMethod = "Scaled from " + servings + " to " + newServings + " servings";
        scaled.dataSource = this.dataSource;
        
        return scaled;
    }
    
    public Map<String, Double> getNutrientSummary() {
        Map<String, Double> summary = new HashMap<>();
        summary.put("Calories", calories);
        summary.put("Lipides (g)", totalFat);
        summary.put("Glucides (g)", totalCarbs);
        summary.put("Protéines (g)", protein);
        summary.put("Fibres (g)", fiber);
        summary.put("Sucres (g)", sugars);
        summary.put("Sodium (mg)", sodium);
        summary.put("Cholestérol (mg)", cholesterol);
        return summary;
    }
    
    public String getNutritionGrade() {
        // Système de notation simplifié basé sur les nutriments
        double score = 0;
        
        // Points positifs (fibres, protéines)
        score += Math.min(fiber * 2, 10);  // Max 10 points pour fibres
        score += Math.min(protein, 15);     // Max 15 points pour protéines
        
        // Points négatifs (graisses saturées, sucres, sodium)
        score -= Math.max(0, saturatedFat - 3) * 2;  // Pénalité au-dessus de 3g
        score -= Math.max(0, sugars - 10);           // Pénalité au-dessus de 10g
        score -= Math.max(0, sodium - 600) / 100;    // Pénalité au-dessus de 600mg
        
        if (score >= 15) return "A";
        else if (score >= 10) return "B";
        else if (score >= 5) return "C";
        else if (score >= 0) return "D";
        else return "E";
    }
    
    public boolean isHighInSodium() {
        return sodium > 400; // Plus de 400mg par portion
    }
    
    public boolean isHighInSaturatedFat() {
        return saturatedFat > 5; // Plus de 5g par portion
    }
    
    public boolean isHighInSugars() {
        return sugars > 15; // Plus de 15g par portion
    }
    
    public boolean isGoodSourceOfProtein() {
        return protein >= 10; // Au moins 10g par portion
    }
    
    public boolean isGoodSourceOfFiber() {
        return fiber >= 3; // Au moins 3g par portion
    }
}
