package fr.didictateur.inanutshell.data.model;

import java.util.Date;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private List<ShoppingItem> items;
    private Date createdDate;
    private Date modifiedDate;
    private boolean archived;
    private String description;
    private double totalBudget;
    private String store;
    
    // Constructors
    public ShoppingList() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.archived = false;
    }
    
    public ShoppingList(String name) {
        this();
        this.name = name;
    }
    
    public ShoppingList(String name, List<ShoppingItem> items) {
        this(name);
        this.items = items;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedDate = new Date();
    }
    
    public List<ShoppingItem> getItems() { return items; }
    public void setItems(List<ShoppingItem> items) { 
        this.items = items;
        this.modifiedDate = new Date();
    }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { 
        this.archived = archived;
        this.modifiedDate = new Date();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.modifiedDate = new Date();
    }
    
    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }
    
    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
    
    // Utility methods
    public void addItem(ShoppingItem item) {
        if (items != null && !items.contains(item)) {
            items.add(item);
            this.modifiedDate = new Date();
        }
    }
    
    public void removeItem(ShoppingItem item) {
        if (items != null) {
            items.remove(item);
            this.modifiedDate = new Date();
        }
    }
    
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    public int getPurchasedItemCount() {
        if (items == null) return 0;
        
        int count = 0;
        for (ShoppingItem item : items) {
            if (item.isPurchased()) {
                count++;
            }
        }
        return count;
    }
    
    public double getTotalCost() {
        if (items == null) return 0.0;
        
        double total = 0.0;
        for (ShoppingItem item : items) {
            total += item.getTotalPrice();
        }
        return total;
    }
    
    public double getPurchasedCost() {
        if (items == null) return 0.0;
        
        double total = 0.0;
        for (ShoppingItem item : items) {
            if (item.isPurchased()) {
                total += item.getTotalPrice();
            }
        }
        return total;
    }
    
    public boolean isComplete() {
        return getItemCount() > 0 && getPurchasedItemCount() == getItemCount();
    }
    
    public double getCompletionPercentage() {
        int total = getItemCount();
        if (total == 0) return 0.0;
        
        return (double) getPurchasedItemCount() / total * 100.0;
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Liste de courses sans nom";
    }
}
