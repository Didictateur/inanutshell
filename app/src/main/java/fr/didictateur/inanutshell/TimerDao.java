package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TimerDao {
    
    @Insert
    long insertTimer(Timer timer);
    
    @Update
    void updateTimer(Timer timer);
    
    @Delete
    void deleteTimer(Timer timer);
    
    @Query("SELECT * FROM timers WHERE id = :id")
    Timer getTimerById(int id);
    
    @Query("SELECT * FROM timers WHERE id = :id")
    LiveData<Timer> getTimerByIdLive(int id);
    
    @Query("SELECT * FROM timers ORDER BY createdAt DESC")
    List<Timer> getAllTimers();
    
    @Query("SELECT * FROM timers ORDER BY createdAt DESC")
    LiveData<List<Timer>> getAllTimersLive();
    
    @Query("SELECT * FROM timers WHERE state IN ('RUNNING', 'PAUSED') ORDER BY createdAt DESC")
    List<Timer> getActiveTimers();
    
    @Query("SELECT * FROM timers WHERE state IN ('RUNNING', 'PAUSED') ORDER BY createdAt DESC")
    LiveData<List<Timer>> getActiveTimersLive();
    
    @Query("SELECT * FROM timers WHERE state = 'RUNNING' ORDER BY createdAt DESC")
    List<Timer> getRunningTimers();
    
    @Query("SELECT * FROM timers WHERE state = 'RUNNING' ORDER BY createdAt DESC")
    LiveData<List<Timer>> getRunningTimersLive();
    
    @Query("SELECT * FROM timers WHERE state = 'FINISHED' ORDER BY createdAt DESC LIMIT 10")
    List<Timer> getRecentFinishedTimers();
    
    @Query("SELECT * FROM timers WHERE recipeId = :recipeId ORDER BY createdAt DESC")
    List<Timer> getTimersForRecipe(String recipeId);
    
    @Query("SELECT * FROM timers WHERE recipeId = :recipeId ORDER BY createdAt DESC")
    LiveData<List<Timer>> getTimersForRecipeLive(String recipeId);
    
    @Query("DELETE FROM timers WHERE state IN ('FINISHED', 'CANCELLED') AND createdAt < :beforeTimestamp")
    void deleteOldTimers(long beforeTimestamp);
    
    @Query("DELETE FROM timers WHERE state IN ('FINISHED', 'CANCELLED')")
    void deleteAllFinishedTimers();
    
    @Query("UPDATE timers SET state = 'CANCELLED' WHERE state IN ('RUNNING', 'PAUSED')")
    void cancelAllActiveTimers();
    
    @Query("SELECT COUNT(*) FROM timers WHERE state IN ('RUNNING', 'PAUSED')")
    int getActiveTimersCount();
    
    @Query("SELECT COUNT(*) FROM timers WHERE state = 'RUNNING'")
    int getRunningTimersCount();
}
