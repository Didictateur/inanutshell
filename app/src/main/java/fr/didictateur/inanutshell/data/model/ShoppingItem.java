package fr.didictateur.inanutshell.data.model;

import java.util.Date;

public class ShoppingItem {
    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private boolean purchased;
    private String notes;
    private Date createdDate;
    private double price;
    private String store;
    
    // Constructors
    public ShoppingItem() {
        this.createdDate = new Date();
        this.purchased = false;
    }
    
    public ShoppingItem(String name, double quantity, String unit) {
        this();
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
    
    public ShoppingItem(String name, double quantity, String unit, String category) {
        this(name, quantity, unit);
        this.category = category;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
    
    // Utility methods
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        if (quantity > 0) {
            sb.append(quantity);
            if (unit != null && !unit.isEmpty()) {
                sb.append(" ").append(unit);
            }
            sb.append(" ");
        }
        sb.append(name);
        return sb.toString();
    }
    
    public double getTotalPrice() {
        return price * quantity;
    }
    
    public void togglePurchased() {
        this.purchased = !this.purchased;
    }
    
    @Override
    public String toString() {
        return getDisplayText() + (purchased ? " (acheté)" : "");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ShoppingItem item = (ShoppingItem) obj;
        return name != null ? name.equals(item.name) : item.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
