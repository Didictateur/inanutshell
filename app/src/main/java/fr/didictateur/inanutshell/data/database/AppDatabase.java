package fr.didictateur.inanutshell.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import fr.didictateur.inanutshell.data.model.Recipe;

@Database(
    entities = {Recipe.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({DatabaseConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "inanutshell_database";
    private static AppDatabase instance;
    
    public abstract RecipeDao recipeDao();
    // Add other DAOs here as needed
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
    
    public static void destroyInstance() {
        instance = null;
    }
}
