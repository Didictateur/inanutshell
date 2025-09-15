package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timers")
public class Timer {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;                    // Nom de la minuterie
    public long originalDurationMs;        // Durée originale en millisecondes
    public long remainingTimeMs;           // Temps restant en millisecondes
    public TimerState state;               // État actuel de la minuterie
    public long createdAt;                 // Timestamp de création
    public long startedAt;                 // Timestamp de démarrage
    public long pausedAt;                  // Timestamp de pause (si applicable)
    public String recipeId;                // ID de recette associée (optionnel)
    
    public enum TimerState {
        CREATED,    // Créé mais pas encore démarré
        RUNNING,    // En cours d'exécution
        PAUSED,     // En pause
        FINISHED,   // Terminé
        CANCELLED   // Annulé
    }
    
    public Timer() {
        this.createdAt = System.currentTimeMillis();
        this.state = TimerState.CREATED;
    }
    
    public Timer(String name, long durationMs) {
        this();
        this.name = name;
        this.originalDurationMs = durationMs;
        this.remainingTimeMs = durationMs;
    }
    
    public Timer(String name, long durationMs, String recipeId) {
        this(name, durationMs);
        this.recipeId = recipeId;
    }
    
    // Méthodes utilitaires
    public boolean isActive() {
        return state == TimerState.RUNNING || state == TimerState.PAUSED;
    }
    
    public boolean isFinished() {
        return state == TimerState.FINISHED;
    }
    
    public boolean isRunning() {
        return state == TimerState.RUNNING;
    }
    
    public boolean isPaused() {
        return state == TimerState.PAUSED;
    }
    
    public float getProgress() {
        if (originalDurationMs <= 0) return 0f;
        return Math.max(0f, Math.min(1f, 1f - ((float) remainingTimeMs / originalDurationMs)));
    }
    
    public String getFormattedTime() {
        return formatDuration(remainingTimeMs);
    }
    
    public String getFormattedOriginalDuration() {
        return formatDuration(originalDurationMs);
    }
    
    public static String formatDuration(long durationMs) {
        if (durationMs < 0) return "00:00";
        
        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    public void start() {
        if (state == TimerState.CREATED || state == TimerState.PAUSED) {
            this.startedAt = System.currentTimeMillis();
            this.state = TimerState.RUNNING;
        }
    }
    
    public void pause() {
        if (state == TimerState.RUNNING) {
            this.pausedAt = System.currentTimeMillis();
            this.state = TimerState.PAUSED;
        }
    }
    
    public void finish() {
        this.state = TimerState.FINISHED;
        this.remainingTimeMs = 0;
    }
    
    public void cancel() {
        this.state = TimerState.CANCELLED;
    }
    
    public void reset() {
        this.remainingTimeMs = originalDurationMs;
        this.state = TimerState.CREATED;
        this.startedAt = 0;
        this.pausedAt = 0;
    }
    
    // Mettre à jour le temps restant basé sur l'heure actuelle
    public void updateRemainingTime() {
        if (state == TimerState.RUNNING && startedAt > 0) {
            long elapsed = System.currentTimeMillis() - startedAt;
            remainingTimeMs = Math.max(0, originalDurationMs - elapsed);
            
            if (remainingTimeMs <= 0) {
                finish();
            }
        }
    }
}
