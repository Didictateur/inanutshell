package fr.didictateur.inanutshell;

public class Recette implements Item {
    public long id;
    public String titre;
    public String taille;
    public String tempsPrep;
    public String ingredients;
    public String preparation;
    public String notes;
    public int imageResId;
    public String photoPath;
    public Long parentId;

    public Recette() {}

    public Recette(String titre, String taille, String tempsPrep, String ingredients, String preparation, String notes, int imageResId, String photoPath, Long parentId) {
        this.titre = titre;
        this.taille = taille;
        this.tempsPrep = tempsPrep;
        this.ingredients = ingredients;
        this.preparation = preparation;
        this.notes = notes;
        this.imageResId = imageResId;
        this.photoPath = photoPath;
        this.parentId = parentId;
    }

    @Override
    public boolean isFolder() { 
        return false; 
    }

    @Override
    public String getName() { 
        return titre; 
    }

    @Override
    public long getId() { 
        return id; 
    }
}
