package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.Ignore;

@Entity(tableName = "meal_plans",
        foreignKeys = @ForeignKey(entity = Recette.class,
                parentColumns = "id",
                childColumns = "recetteId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("recetteId")})
public class MealPlan {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public String date; // Format: YYYY-MM-DD
    public String mealType; // "breakfast", "lunch", "dinner"
    public Long recetteId; // ID de la recette associée
    public String notes; // Notes optionnelles
    public String portions; // Nombre de portions (optionnel)
    
    @Ignore
    public MealPlan(String date, String mealType, Long recetteId, String notes) {
        this.date = date;
        this.mealType = mealType;
        this.recetteId = recetteId;
        this.notes = notes;
        this.portions = "1"; // Par défaut 1 portion
    }
    
    public MealPlan(String date, String mealType, Long recetteId, String notes, String portions) {
        this.date = date;
        this.mealType = mealType;
        this.recetteId = recetteId;
        this.notes = notes;
        this.portions = portions;
    }
    
    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    
    public Long getRecetteId() { return recetteId; }
    public void setRecetteId(Long recetteId) { this.recetteId = recetteId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getPortions() { return portions; }
    public void setPortions(String portions) { this.portions = portions; }
}
