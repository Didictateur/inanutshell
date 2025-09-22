package fr.didictateur.inanutshell.sync.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

/**
 * Base de données Room pour la synchronisation offline
 */
@Database(
    entities = {PendingSync.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({SyncTypeConverters.class})
public abstract class OfflineSyncDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "offline_sync_db";
    private static volatile OfflineSyncDatabase instance;
    
    public abstract PendingSyncDao pendingSyncDao();
    
    public static OfflineSyncDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineSyncDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        OfflineSyncDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
