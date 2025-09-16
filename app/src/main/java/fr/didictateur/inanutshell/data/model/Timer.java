package fr.didictateur.inanutshell.data.model;

import java.util.Date;

public class Timer {
    public enum TimerState {
        STOPPED,
        RUNNING,
        PAUSED,
        FINISHED
    }
    
    private long id;
    private String name;
    private long duration; // durée totale en millisecondes
    private long remainingTime; // temps restant en millisecondes
    private TimerState state;
    private Date createdDate;
    private Date startedDate;
    private Date pausedDate;
    private Date finishedDate;
    private String description;
    private boolean notificationEnabled;
    private String soundPath;
    
    // Constructors
    public Timer() {
        this.createdDate = new Date();
        this.state = TimerState.STOPPED;
        this.notificationEnabled = true;
    }
    
    public Timer(String name, long duration) {
        this();
        this.name = name;
        this.duration = duration;
        this.remainingTime = duration;
    }
    
    public Timer(String name, long duration, String description) {
        this(name, duration);
        this.description = description;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { 
        this.duration = duration;
        if (state == TimerState.STOPPED) {
            this.remainingTime = duration;
        }
    }
    
    public long getRemainingTime() { return remainingTime; }
    public void setRemainingTime(long remainingTime) { this.remainingTime = remainingTime; }
    
    public TimerState getState() { return state; }
    public void setState(TimerState state) { this.state = state; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getStartedDate() { return startedDate; }
    public void setStartedDate(Date startedDate) { this.startedDate = startedDate; }
    
    public Date getPausedDate() { return pausedDate; }
    public void setPausedDate(Date pausedDate) { this.pausedDate = pausedDate; }
    
    public Date getFinishedDate() { return finishedDate; }
    public void setFinishedDate(Date finishedDate) { this.finishedDate = finishedDate; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    
    public String getSoundPath() { return soundPath; }
    public void setSoundPath(String soundPath) { this.soundPath = soundPath; }
    
    // Timer control methods
    public void start() {
        if (state == TimerState.STOPPED || state == TimerState.PAUSED) {
            state = TimerState.RUNNING;
            if (startedDate == null) {
                startedDate = new Date();
            }
            pausedDate = null;
        }
    }
    
    public void pause() {
        if (state == TimerState.RUNNING) {
            state = TimerState.PAUSED;
            pausedDate = new Date();
        }
    }
    
    public void stop() {
        state = TimerState.STOPPED;
        remainingTime = duration;
        startedDate = null;
        pausedDate = null;
        finishedDate = null;
    }
    
    public void finish() {
        state = TimerState.FINISHED;
        remainingTime = 0;
        finishedDate = new Date();
    }
    
    public void reset() {
        stop();
    }
    
    // Utility methods
    public boolean isRunning() {
        return state == TimerState.RUNNING;
    }
    
    public boolean isPaused() {
        return state == TimerState.PAUSED;
    }
    
    public boolean isStopped() {
        return state == TimerState.STOPPED;
    }
    
    public boolean isFinished() {
        return state == TimerState.FINISHED;
    }
    
    public long getElapsedTime() {
        return duration - remainingTime;
    }
    
    public double getProgressPercentage() {
        if (duration == 0) return 0.0;
        return (double) getElapsedTime() / duration * 100.0;
    }
    
    public String getFormattedRemainingTime() {
        return formatTime(remainingTime);
    }
    
    public String getFormattedDuration() {
        return formatTime(duration);
    }
    
    public static String formatTime(long timeInMillis) {
        long seconds = timeInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Timer sans nom";
    }
}
