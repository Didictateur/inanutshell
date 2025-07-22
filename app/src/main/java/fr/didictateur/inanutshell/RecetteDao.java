package fr.didictateur.inanutshell;

import androidx.room.*;
import java.util.List;

@Dao
public interface RecetteDao {
	@Query("SELECT * FROM Recette WHERE (:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId")
	List<Recette> getRecettesByParent(Long parentId);

	@Query("SELECT * FROM Recette WHERE parentId IS NULL")
	List<Recette> getRecettesWithoutParent();

	@Insert
	long insert(Recette recette);

	@Update
	void update(Recette recette);

	@Delete
	void delete(Recette recette);

	@Query("SELECT * FROM Recette WHERE id = :id")
	Recette getRecetteById(long id);

	@Query("SELECT * FROM Recette")
	List<Recette> getAllRecettes();

	// Méthodes de recherche
	@Query("SELECT * FROM Recette WHERE titre LIKE :query ORDER BY titre ASC")
	List<Recette> searchByName(String query);

	@Query("SELECT * FROM Recette WHERE ingredients LIKE :query ORDER BY titre ASC")
	List<Recette> searchByIngredients(String query);

	@Query("SELECT * FROM Recette WHERE titre LIKE :query OR ingredients LIKE :query OR preparation LIKE :query ORDER BY titre ASC")
	List<Recette> searchAll(String query);

	@Query("SELECT * FROM Recette ORDER BY id DESC LIMIT :limit")
	List<Recette> getRecentRecipes(int limit);

	@Query("SELECT * FROM Recette")
	List<Recette> getAllRecipes();
}
