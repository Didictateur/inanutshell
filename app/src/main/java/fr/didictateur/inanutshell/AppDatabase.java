package fr.didictateur.inanutshell;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Folder.class, Recette.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
	public abstract FolderDao folderDao();
	public abstract RecetteDao recetteDao();
}
