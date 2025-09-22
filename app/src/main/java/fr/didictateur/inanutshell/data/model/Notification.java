package fr.didictateur.inanutshell.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

import fr.didictateur.inanutshell.Converters;

/**
 * Entité représentant une notification dans l'application
 */
@Entity(tableName = "notifications")
@TypeConverters(Converters.class)
public class Notification {
    
    @PrimaryKey(autoGenerate = true)
    private int notificationId;
    
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private Date scheduledTime;
    private Date createdAt;
    private boolean isRead;
    private boolean isScheduled;
    private boolean isRepeating;
    private String actionData; // JSON data for action (recipe ID, meal plan ID, etc.)
    private String iconName;
    private int color;
    
    // Constructeurs
    public Notification() {
        this.createdAt = new Date();
        this.isRead = false;
        this.isScheduled = false;
        this.isRepeating = false;
        this.priority = NotificationPriority.NORMAL;
    }
    
    @androidx.room.Ignore
    public Notification(String title, String message, NotificationType type) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
    }
    
    // Types de notifications
    public enum NotificationType {
        MEAL_REMINDER,      // Rappel de repas
        RECIPE_SUGGESTION,  // Suggestion de recette
        NEW_RECIPE,         // Nouvelle recette
        TIMER_ALERT,        // Alerte minuterie
        COOKING_TIP,        // Conseil de cuisine
        SHOPPING_REMINDER,  // Rappel liste de courses
        MEAL_PLAN_REMINDER, // Rappel planification
        SYSTEM_UPDATE       // Mise à jour système
    }
    
    // Priorités
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    // Getters et Setters
    public int getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public NotificationPriority getPriority() {
        return priority;
    }
    
    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }
    
    public Date getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public boolean isScheduled() {
        return isScheduled;
    }
    
    public void setScheduled(boolean scheduled) {
        isScheduled = scheduled;
    }
    
    public boolean isRepeating() {
        return isRepeating;
    }
    
    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }
    
    public String getActionData() {
        return actionData;
    }
    
    public void setActionData(String actionData) {
        this.actionData = actionData;
    }
    
    public String getIconName() {
        return iconName;
    }
    
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    // Méthodes utilitaires
    public boolean isPending() {
        if (!isScheduled || scheduledTime == null) {
            return false;
        }
        return scheduledTime.after(new Date());
    }
    
    public boolean isOverdue() {
        if (!isScheduled || scheduledTime == null) {
            return false;
        }
        return scheduledTime.before(new Date()) && !isRead;
    }
    
    public long getTimeUntilScheduled() {
        if (scheduledTime == null) {
            return 0;
        }
        return scheduledTime.getTime() - System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + notificationId +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", scheduledTime=" + scheduledTime +
                ", isRead=" + isRead +
                '}';
    }
}
