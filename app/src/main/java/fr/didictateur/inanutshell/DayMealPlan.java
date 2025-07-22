package fr.didictateur.inanutshell;

import java.util.ArrayList;
import java.util.List;

public class DayMealPlan {
    private String date;
    private String dayName;
    private MealPlanWithRecette breakfast;
    private MealPlanWithRecette lunch;
    private MealPlanWithRecette dinner;
    
    public DayMealPlan(String date, String dayName) {
        this.date = date;
        this.dayName = dayName;
    }
    
    public void addMeal(MealPlanWithRecette mealPlan) {
        String mealType = mealPlan.getMealPlan().getMealType();
        switch (mealType) {
            case "breakfast":
                this.breakfast = mealPlan;
                break;
            case "lunch":
                this.lunch = mealPlan;
                break;
            case "dinner":
                this.dinner = mealPlan;
                break;
        }
    }
    
    // Getters et setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    
    public MealPlanWithRecette getBreakfast() { return breakfast; }
    public void setBreakfast(MealPlanWithRecette breakfast) { this.breakfast = breakfast; }
    
    public MealPlanWithRecette getLunch() { return lunch; }
    public void setLunch(MealPlanWithRecette lunch) { this.lunch = lunch; }
    
    public MealPlanWithRecette getDinner() { return dinner; }
    public void setDinner(MealPlanWithRecette dinner) { this.dinner = dinner; }
}
