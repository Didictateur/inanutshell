package fr.didictateur.inanutshell.data.model;

import java.util.Date;
import java.util.List;

public class MealPlan {
    private long id;
    private String name;
    private Date startDate;
    private Date endDate;
    private List<Recipe> recipes;
    private String description;
    private boolean active;
    private Date createdDate;
    private Date modifiedDate;
    
    // Constructors
    public MealPlan() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.active = true;
    }
    
    public MealPlan(String name, Date startDate, Date endDate) {
        this();
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedDate = new Date();
    }
    
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { 
        this.startDate = startDate;
        this.modifiedDate = new Date();
    }
    
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { 
        this.endDate = endDate;
        this.modifiedDate = new Date();
    }
    
    public List<Recipe> getRecipes() { return recipes; }
    public void setRecipes(List<Recipe> recipes) { 
        this.recipes = recipes;
        this.modifiedDate = new Date();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.modifiedDate = new Date();
    }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { 
        this.active = active;
        this.modifiedDate = new Date();
    }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    // Utility methods
    public void addRecipe(Recipe recipe) {
        if (recipes != null && !recipes.contains(recipe)) {
            recipes.add(recipe);
            this.modifiedDate = new Date();
        }
    }
    
    public void removeRecipe(Recipe recipe) {
        if (recipes != null) {
            recipes.remove(recipe);
            this.modifiedDate = new Date();
        }
    }
    
    public int getRecipeCount() {
        return recipes != null ? recipes.size() : 0;
    }
    
    public boolean containsRecipe(Recipe recipe) {
        return recipes != null && recipes.contains(recipe);
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Plan de repas sans nom";
    }
}
