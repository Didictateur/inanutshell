package fr.didictateur.inanutshell.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RecipeInstruction {
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("summary")
    private String summary;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("ingredientReferences")
    private List<String> ingredientReferences;

    // Constructeurs
    public RecipeInstruction() {
    }

    public RecipeInstruction(String text) {
        this.text = text;
    }

    // Getters et setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getIngredientReferences() {
        return ingredientReferences;
    }

    public void setIngredientReferences(List<String> ingredientReferences) {
        this.ingredientReferences = ingredientReferences;
    }

    @Override
    public String toString() {
        return text != null ? text : "";
    }
}
