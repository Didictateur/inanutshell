package fr.didictateur.inanutshell.data.cache;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

/**
 * Base de données Room pour le cache offline
 */
@Database(
    entities = {CachedRecipe.class},
    version = 1,
    exportSchema = false
)
public abstract class CacheDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "inanutshell_cache.db";
    private static CacheDatabase INSTANCE;
    
    public abstract CachedRecipeDao cachedRecipeDao();
    
    public static synchronized CacheDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                context.getApplicationContext(),
                CacheDatabase.class,
                DATABASE_NAME
            )
            .allowMainThreadQueries() // À utiliser avec parcimonie, idéalement async
            .build();
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
