package fr.didictateur.inanutshell.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(tableName = "usage_stats",
        indices = {@Index("userId"), @Index("date"), @Index("eventType")})
public class UsageStats {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String userId;
    private String eventType;
    private String eventData;
    private Date date;
    private long timestamp;
    private String sessionId;
    
    // Types d'événements
    public static final String EVENT_APP_OPEN = "app_open";
    public static final String EVENT_APP_CLOSE = "app_close";
    public static final String EVENT_RECIPE_VIEW = "recipe_view";
    public static final String EVENT_RECIPE_CREATE = "recipe_create";
    public static final String EVENT_RECIPE_EDIT = "recipe_edit";
    public static final String EVENT_RECIPE_DELETE = "recipe_delete";
    public static final String EVENT_RECIPE_FAVORITE = "recipe_favorite";
    public static final String EVENT_RECIPE_UNFAVORITE = "recipe_unfavorite";
    public static final String EVENT_RECIPE_SHARE = "recipe_share";
    public static final String EVENT_SEARCH_QUERY = "search_query";
    public static final String EVENT_SEARCH_ADVANCED = "search_advanced";
    public static final String EVENT_CATEGORY_BROWSE = "category_browse";
    public static final String EVENT_COMMENT_ADD = "comment_add";
    public static final String EVENT_COMMENT_VOTE = "comment_vote";
    public static final String EVENT_NUTRITION_VIEW = "nutrition_view";
    public static final String EVENT_NUTRITION_CALCULATE = "nutrition_calculate";
    public static final String EVENT_USER_FOLLOW = "user_follow";
    public static final String EVENT_OFFLINE_SYNC = "offline_sync";
    public static final String EVENT_API_REQUEST = "api_request";
    
    public UsageStats() {}
    
    public UsageStats(String userId, String eventType, String eventData, String sessionId) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.sessionId = sessionId;
        this.date = new Date();
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters et Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
