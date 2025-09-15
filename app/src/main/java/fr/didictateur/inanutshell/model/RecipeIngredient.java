package fr.didictateur.inanutshell.model;

public class RecipeIngredient {
    private double quantity;
    private String unit;
    private String food;
    private String note;
    private String display;
    private String title;
    private String originalText;
    private String referenceId;

    // Constructeurs
    public RecipeIngredient() {
    }

    public RecipeIngredient(String display) {
        this.display = display;
    }

    // Getters et setters
    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getFood() {
        return food;
    }

    public void setFood(String food) {
        this.food = food;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String toString() {
        // Retourne la représentation la plus appropriée pour affichage
        if (display != null && !display.isEmpty()) {
            return display;
        }
        if (note != null && !note.isEmpty()) {
            return note;
        }
        return "";
    }
}
