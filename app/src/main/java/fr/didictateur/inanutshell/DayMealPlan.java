package fr.didictateur.inanutshell;

import java.util.ArrayList;
import java.util.List;

public class DayMealPlan {
    private String date;
    private String dayName;
    private List<MealPlanWithRecette> breakfast;
    private List<MealPlanWithRecette> lunch;
    private List<MealPlanWithRecette> dinner;
    
    public DayMealPlan(String date, String dayName) {
        this.date = date;
        this.dayName = dayName;
        this.breakfast = new ArrayList<>();
        this.lunch = new ArrayList<>();
        this.dinner = new ArrayList<>();
    }
    
    public void addMeal(MealPlanWithRecette mealPlan) {
        String mealType = mealPlan.getMealPlan().getMealType();
        switch (mealType) {
            case "breakfast":
                this.breakfast.add(mealPlan);
                break;
            case "lunch":
                this.lunch.add(mealPlan);
                break;
            case "dinner":
                this.dinner.add(mealPlan);
                break;
        }
    }
    
    // Getters et setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    
    public List<MealPlanWithRecette> getBreakfast() { return breakfast; }
    public void setBreakfast(List<MealPlanWithRecette> breakfast) { this.breakfast = breakfast; }
    
    public List<MealPlanWithRecette> getLunch() { return lunch; }
    public void setLunch(List<MealPlanWithRecette> lunch) { this.lunch = lunch; }
    
    public List<MealPlanWithRecette> getDinner() { return dinner; }
    public void setDinner(List<MealPlanWithRecette> dinner) { this.dinner = dinner; }
    
    // Méthodes utilitaires pour la compatibilité
    public MealPlanWithRecette getFirstBreakfast() { 
        return breakfast.isEmpty() ? null : breakfast.get(0); 
    }
    
    public MealPlanWithRecette getFirstLunch() { 
        return lunch.isEmpty() ? null : lunch.get(0); 
    }
    
    public MealPlanWithRecette getFirstDinner() { 
        return dinner.isEmpty() ? null : dinner.get(0); 
    }
}
