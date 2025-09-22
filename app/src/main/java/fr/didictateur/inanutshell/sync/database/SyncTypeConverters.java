package fr.didictateur.inanutshell.sync.database;

import androidx.room.TypeConverter;
import fr.didictateur.inanutshell.sync.database.PendingSync;

/**
 * Convertisseurs de types pour Room
 */
public class SyncTypeConverters {
    
    @TypeConverter
    public static String fromStatus(PendingSync.Status status) {
        return status == null ? null : status.name();
    }
    
    @TypeConverter
    public static PendingSync.Status toStatus(String status) {
        return status == null ? null : PendingSync.Status.valueOf(status);
    }
}
