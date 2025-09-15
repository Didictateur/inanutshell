package fr.didictateur.inanutshell.data.model;

import com.google.gson.annotations.SerializedName;

public class Nutrition {
    @SerializedName("calories")
    private String calories;
    
    @SerializedName("fatContent")
    private String fatContent;
    
    @SerializedName("proteinContent")
    private String proteinContent;
    
    @SerializedName("carbohydrateContent")
    private String carbohydrateContent;
    
    @SerializedName("fiberContent")
    private String fiberContent;
    
    @SerializedName("sugarContent")
    private String sugarContent;
    
    @SerializedName("sodiumContent")
    private String sodiumContent;

    // Constructeurs
    public Nutrition() {}

    // Getters et Setters
    public String getCalories() { return calories; }
    public void setCalories(String calories) { this.calories = calories; }

    public String getFatContent() { return fatContent; }
    public void setFatContent(String fatContent) { this.fatContent = fatContent; }

    public String getProteinContent() { return proteinContent; }
    public void setProteinContent(String proteinContent) { this.proteinContent = proteinContent; }

    public String getCarbohydrateContent() { return carbohydrateContent; }
    public void setCarbohydrateContent(String carbohydrateContent) { this.carbohydrateContent = carbohydrateContent; }

    public String getFiberContent() { return fiberContent; }
    public void setFiberContent(String fiberContent) { this.fiberContent = fiberContent; }

    public String getSugarContent() { return sugarContent; }
    public void setSugarContent(String sugarContent) { this.sugarContent = sugarContent; }

    public String getSodiumContent() { return sodiumContent; }
    public void setSodiumContent(String sodiumContent) { this.sodiumContent = sodiumContent; }
}
