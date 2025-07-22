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
}
