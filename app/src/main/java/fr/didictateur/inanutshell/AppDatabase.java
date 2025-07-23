package fr.didictateur.inanutshell;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(
    entities = {Recette.class, Folder.class, MealPlan.class},
    version = 4,  // Incrémenté pour le champ portions dans MealPlan
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract RecetteDao recetteDao();
    public abstract FolderDao folderDao();
    public abstract MealPlanDao mealPlanDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "recipe_database"
                    ).fallbackToDestructiveMigration()  // Pour simplicité, recrée la DB si changement de schéma
                     .allowMainThreadQueries()  // À éviter en production mais OK pour développement
                     .build();
                }
            }
        }
        return INSTANCE;
    }
}
