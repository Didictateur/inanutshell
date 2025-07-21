package fr.didictateur.inanutshell;

import androidx.room.*;
import java.util.List;

@Dao
public interface RecetteDao {
	@Query("SELECT * FROM Recette Where parentId IS :parentId")
	List<Recette> getRecettesByParent(Integer parentId);

	@Insert
	long insert(Recette recette);

	@Update
	void update(Recette recette);

	@Delete
	void delete(Recette recette);

	@Query("SELECT * FROM Recette WHERE id = :id")
	Recette getRecetteById(int id);

	@Query("SELECT * FROM Recette")
	List<Recette> getAllRecettes();
}
