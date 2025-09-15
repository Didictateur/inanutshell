package fr.didictateur.inanutshell.data.response;

import com.google.gson.annotations.SerializedName;
import fr.didictateur.inanutshell.data.model.MealieMealPlan;
import java.util.List;

/**
 * Réponse API pour la liste des planifications de repas Mealie
 */
public class MealPlanListResponse {
    
    @SerializedName("items")
    private List<MealieMealPlan> items;
    
    @SerializedName("total")
    private int total;
    
    @SerializedName("page")
    private int page;
    
    @SerializedName("size")
    private int size;
    
    @SerializedName("pages")
    private int pages;
    
    // Constructeurs
    public MealPlanListResponse() {}
    
    public MealPlanListResponse(List<MealieMealPlan> items, int total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }
    
    // Getters et Setters
    public List<MealieMealPlan> getItems() { return items; }
    public void setItems(List<MealieMealPlan> items) { this.items = items; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }
}
