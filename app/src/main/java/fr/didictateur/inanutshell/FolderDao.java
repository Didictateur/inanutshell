package fr.didictateur.inanutshell;

import androidx.room.*;
import java.util.List;

@Dao
public interface FolderDao {
	@Query("SELECT * FROM Folder WHERE parentId IS :parentId")
	List<Folder> getFoldersByParent(Long parentId);

	@Insert
	long insert(Folder folder);

	@Delete
	void delete(Folder folder);

	@Query("SELECT * FROM Folder WHERE id = :id")
	Folder getFolderById(long id);

	@Query("SELECT * FROM Folder")
	List<Folder> getAllFolders();
}
