package fr.didictateur.inanutshell.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "recipe_comments",
        foreignKeys = @ForeignKey(
            entity = Recipe.class,
            parentColumns = "id",
            childColumns = "recipeId",
            onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("recipeId"), @Index("userId")})
public class RecipeComment implements Serializable {
    
    @PrimaryKey
    private String id;
    
    private String recipeId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private float rating; // Note sur 5 étoiles
    private Date createdAt;
    private Date updatedAt;
    private boolean isEdited;
    private boolean isReported;
    private boolean isApproved;
    private String moderatorNote;
    private int helpfulVotes;
    private String parentCommentId; // Pour les réponses
    
    public RecipeComment() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isApproved = true; // Auto-approuvé par défaut
        this.helpfulVotes = 0;
    }
    
    public RecipeComment(String recipeId, String userId, String userName, String content, float rating) {
        this();
        this.recipeId = recipeId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.rating = rating;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    
    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content;
        this.updatedAt = new Date();
        this.isEdited = true;
    }
    
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }
    
    public boolean isReported() { return isReported; }
    public void setReported(boolean reported) { isReported = reported; }
    
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }
    
    public String getModeratorNote() { return moderatorNote; }
    public void setModeratorNote(String moderatorNote) { this.moderatorNote = moderatorNote; }
    
    public int getHelpfulVotes() { return helpfulVotes; }
    public void setHelpfulVotes(int helpfulVotes) { this.helpfulVotes = helpfulVotes; }
    
    public String getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
    
    // Méthodes utilitaires
    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isEmpty();
    }
    
    public void incrementHelpfulVotes() {
        this.helpfulVotes++;
    }
    
    public void decrementHelpfulVotes() {
        if (this.helpfulVotes > 0) {
            this.helpfulVotes--;
        }
    }
    
    public String getTimeAgo() {
        if (createdAt == null) return "";
        
        long diff = System.currentTimeMillis() - createdAt.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " jour" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " heure" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return "À l'instant";
        }
    }
}
