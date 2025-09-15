package fr.didictateur.inanutshell.data.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("slug")
    private String slug;

    // Constructeurs
    public Category() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
