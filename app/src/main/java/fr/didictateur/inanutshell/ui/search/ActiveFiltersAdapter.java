package fr.didictateur.inanutshell.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;

/**
 * Adapter pour afficher les filtres actifs sous forme de chips
 */
public class ActiveFiltersAdapter extends RecyclerView.Adapter<ActiveFiltersAdapter.FilterViewHolder> {
    
    public interface OnFilterRemoveListener {
        void onFilterRemove(String filterType, String filterValue);
    }
    
    private List<FilterItem> filters = new ArrayList<>();
    private OnFilterRemoveListener listener;
    
    public ActiveFiltersAdapter(OnFilterRemoveListener listener) {
        this.listener = listener;
    }
    
    public void setFilters(List<FilterItem> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = new Chip(parent.getContext());
        chip.setLayoutParams(new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // Style du chip
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        
        return new FilterViewHolder(chip);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        FilterItem filter = filters.get(position);
        Chip chip = (Chip) holder.itemView;
        
        chip.setText(filter.getDisplayText());
        
        // Handle close button click
        chip.setOnCloseIconClickListener(v -> {
            if (listener != null) {
                listener.onFilterRemove(filter.getType(), filter.getValue());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return filters.size();
    }
    
    static class FilterViewHolder extends RecyclerView.ViewHolder {
        FilterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    
    /**
     * Classe pour représenter un élément de filtre
     */
    public static class FilterItem {
        private String type;
        private String value;
        private String displayText;
        
        public FilterItem(String type, String value, String displayText) {
            this.type = type;
            this.value = value;
            this.displayText = displayText;
        }
        
        public String getType() { return type; }
        public String getValue() { return value; }
        public String getDisplayText() { return displayText; }
    }
}
