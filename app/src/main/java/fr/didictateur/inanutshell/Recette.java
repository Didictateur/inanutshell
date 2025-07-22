package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Recette implements Item {
		@PrimaryKey(autoGenerate = true)
		public long id;
    public String titre;
    public String taille;
    public String tempsPrep;
    public String ingredients;
    public String preparation;
    public String notes;
    public int imageResId;
    public String photoPath; // Nouveau champ pour le chemin de la photo

		public Long parentId;

		public Recette() {}

		@Ignore
    public Recette(
				long id,
				String titre,
				String taille,
				String tempsPrep,
				String ingredients,
				String preparation,
				String notes,
				int imageResId,
				String photoPath,
				Long parentId
			) {
				this.id = id;
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
		public boolean isFolder() { return false; }

		@Override
		public String getTitle() { return titre; }

		public Long getParentId() { return parentId; }

		public long getId() { return id; }
}

