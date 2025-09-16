package fr.didictateur.inanutshell.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Ingredient implements Parcelable {
    private String name;
    private double quantity;
    private String unit;
    private boolean optional;
    private String description;
    
    // Constructors
    public Ingredient() {}
    
    public Ingredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.optional = false;
    }
    
    public Ingredient(String name, double quantity, String unit, boolean optional, String description) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.optional = optional;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    // Utility method
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
        if (optional) {
            sb.append(" (optionnel)");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getDisplayText();
    }
    
    // Parcelable implementation
    protected Ingredient(Parcel in) {
        name = in.readString();
        quantity = in.readDouble();
        unit = in.readString();
        optional = in.readByte() != 0;
        description = in.readString();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeDouble(quantity);
        dest.writeString(unit);
        dest.writeByte((byte) (optional ? 1 : 0));
        dest.writeString(description);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<Ingredient> CREATOR = new Creator<Ingredient>() {
        @Override
        public Ingredient createFromParcel(Parcel in) {
            return new Ingredient(in);
        }
        
        @Override
        public Ingredient[] newArray(int size) {
            return new Ingredient[size];
        }
    };
}
