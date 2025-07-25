package fr.didictateur.inanutshell;

import androidx.room.Embedded;

public class MealPlanWithRecette {
    @Embedded
    public MealPlan mealPlan;
    
    public String recetteTitre;
    public String tempsPrep;
    public String taille;
    public String recetteIngredients;
    
    // Constructeurs
    public MealPlanWithRecette() {}
    
    // Getters et setters
    public MealPlan getMealPlan() { return mealPlan; }
    public void setMealPlan(MealPlan mealPlan) { this.mealPlan = mealPlan; }
    
    public String getRecetteTitre() { return recetteTitre; }
    public void setRecetteTitre(String recetteTitre) { this.recetteTitre = recetteTitre; }
    
    public String getTempsPrep() { return tempsPrep; }
    public void setTempsPrep(String tempsPrep) { this.tempsPrep = tempsPrep; }
    
    public String getTaille() { return taille; }
    public void setTaille(String taille) { this.taille = taille; }
    
    public String getRecetteIngredients() { return recetteIngredients; }
    public void setRecetteIngredients(String recetteIngredients) { this.recetteIngredients = recetteIngredients; }
}
