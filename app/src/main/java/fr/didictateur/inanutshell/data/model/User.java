package fr.didictateur.inanutshell.data.model;

import java.util.Date;
import java.util.List;

public class User {
    public enum UserRole {
        ADMIN,
        USER,
        GUEST
    }
    
    private long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
    private UserRole role;
    private Date createdDate;
    private Date lastLoginDate;
    private boolean active;
    private List<Recipe> favoriteRecipes;
    private String preferences;
    private String bio;
    private String location;
    
    // Constructors
    public User() {
        this.createdDate = new Date();
        this.role = UserRole.USER;
        this.active = true;
    }
    
    public User(String username, String email) {
        this();
        this.username = username;
        this.email = email;
    }
    
    public User(String username, String email, String firstName, String lastName) {
        this(username, email);
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(Date lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public List<Recipe> getFavoriteRecipes() { return favoriteRecipes; }
    public void setFavoriteRecipes(List<Recipe> favoriteRecipes) { this.favoriteRecipes = favoriteRecipes; }
    
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    // Utility methods
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        return fullName.length() > 0 ? fullName.toString() : username;
    }
    
    public String getDisplayName() {
        String fullName = getFullName();
        return !fullName.equals(username) ? fullName : username;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isGuest() {
        return role == UserRole.GUEST;
    }
    
    public void addFavoriteRecipe(Recipe recipe) {
        if (favoriteRecipes != null && !favoriteRecipes.contains(recipe)) {
            favoriteRecipes.add(recipe);
        }
    }
    
    public void removeFavoriteRecipe(Recipe recipe) {
        if (favoriteRecipes != null) {
            favoriteRecipes.remove(recipe);
        }
    }
    
    public boolean isFavoriteRecipe(Recipe recipe) {
        return favoriteRecipes != null && favoriteRecipes.contains(recipe);
    }
    
    public int getFavoriteRecipeCount() {
        return favoriteRecipes != null ? favoriteRecipes.size() : 0;
    }
    
    public void updateLastLogin() {
        this.lastLoginDate = new Date();
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        User user = (User) obj;
        return id == user.id && 
               (username != null ? username.equals(user.username) : user.username == null);
    }
    
    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }
}
