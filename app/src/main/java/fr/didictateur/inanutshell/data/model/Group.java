package fr.didictateur.inanutshell.data.model;

import java.util.Date;
import java.util.List;

public class Group {
    public enum GroupType {
        FAMILY,
        FRIENDS,
        COOKING_CLUB,
        PROFESSIONAL,
        PUBLIC
    }
    
    private long id;
    private String name;
    private String description;
    private GroupType type;
    private List<User> members;
    private User owner;
    private List<Recipe> sharedRecipes;
    private Date createdDate;
    private Date modifiedDate;
    private boolean active;
    private String avatar;
    private int maxMembers;
    private boolean publicGroup;
    
    // Constructors
    public Group() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.active = true;
        this.maxMembers = 50;
        this.publicGroup = false;
    }
    
    public Group(String name, User owner) {
        this();
        this.name = name;
        this.owner = owner;
    }
    
    public Group(String name, String description, GroupType type, User owner) {
        this(name, owner);
        this.description = description;
        this.type = type;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedDate = new Date();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.modifiedDate = new Date();
    }
    
    public GroupType getType() { return type; }
    public void setType(GroupType type) { 
        this.type = type;
        this.modifiedDate = new Date();
    }
    
    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { 
        this.members = members;
        this.modifiedDate = new Date();
    }
    
    public User getOwner() { return owner; }
    public void setOwner(User owner) { 
        this.owner = owner;
        this.modifiedDate = new Date();
    }
    
    public List<Recipe> getSharedRecipes() { return sharedRecipes; }
    public void setSharedRecipes(List<Recipe> sharedRecipes) { 
        this.sharedRecipes = sharedRecipes;
        this.modifiedDate = new Date();
    }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { 
        this.active = active;
        this.modifiedDate = new Date();
    }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    
    public boolean isPublicGroup() { return publicGroup; }
    public void setPublicGroup(boolean publicGroup) { 
        this.publicGroup = publicGroup;
        this.modifiedDate = new Date();
    }
    
    // Utility methods
    public void addMember(User user) {
        if (members != null && !members.contains(user) && members.size() < maxMembers) {
            members.add(user);
            this.modifiedDate = new Date();
        }
    }
    
    public void removeMember(User user) {
        if (members != null && !user.equals(owner)) {
            members.remove(user);
            this.modifiedDate = new Date();
        }
    }
    
    public boolean isMember(User user) {
        return members != null && (members.contains(user) || user.equals(owner));
    }
    
    public boolean isOwner(User user) {
        return owner != null && owner.equals(user);
    }
    
    public int getMemberCount() {
        int count = members != null ? members.size() : 0;
        return owner != null ? count + 1 : count; // Include owner in count
    }
    
    public boolean isFull() {
        return getMemberCount() >= maxMembers;
    }
    
    public void shareRecipe(Recipe recipe) {
        if (sharedRecipes != null && !sharedRecipes.contains(recipe)) {
            sharedRecipes.add(recipe);
            this.modifiedDate = new Date();
        }
    }
    
    public void removeSharedRecipe(Recipe recipe) {
        if (sharedRecipes != null) {
            sharedRecipes.remove(recipe);
            this.modifiedDate = new Date();
        }
    }
    
    public int getSharedRecipeCount() {
        return sharedRecipes != null ? sharedRecipes.size() : 0;
    }
    
    public boolean hasSharedRecipe(Recipe recipe) {
        return sharedRecipes != null && sharedRecipes.contains(recipe);
    }
    
    public String getTypeDisplayName() {
        if (type == null) return "Non défini";
        
        switch (type) {
            case FAMILY: return "Famille";
            case FRIENDS: return "Amis";
            case COOKING_CLUB: return "Club de cuisine";
            case PROFESSIONAL: return "Professionnel";
            case PUBLIC: return "Public";
            default: return "Non défini";
        }
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Groupe sans nom";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Group group = (Group) obj;
        return id == group.id;
    }
    
    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
