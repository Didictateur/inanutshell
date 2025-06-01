package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Recette implements Item {
		@PrimaryKey(autoGenerate = true)
		public int id;
    public String titre;
    public String taille;
    public String tempsPrep;
    public String ingredients;
    public String preparation;
    public String notes;
    public int imageResId;

		public Integer parentId;

		public Recette() {}

		@Ignore
    public Recette(
				int id,
				String titre,
				String taille,
				String tempsPrep,
				String ingredients,
				String preparation,
				String notes,
				int imageResId,
				Integer parentId
			) {
				this.id = id;
        this.titre = titre;
        this.taille = taille;
        this.tempsPrep = tempsPrep;
        this.ingredients = ingredients;
        this.preparation = preparation;
        this.notes = notes;
        this.imageResId = imageResId;
				this.parentId = parentId;
    }

		@Override
		public boolean isFolder() { return false; }

		@Override
		public String getTitle() { return titre; }

		public Integer getParentId() { return parentId; }

		public int getId() { return id; }
}

