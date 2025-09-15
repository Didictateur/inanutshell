package fr.didictateur.inanutshell.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Category;
import java.util.List;
import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    
    private List<Category> categories = new ArrayList<>();
    private OnCategoryClickListener listener;
    private Category selectedCategory;
    private boolean selectionMode = false;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onClearSelection();
    }
    
    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addCategory(Category category) {
        categories.add(category);
        notifyItemInserted(categories.size() - 1);
    }
    
    public void removeCategory(int position) {
        if (position >= 0 && position < categories.size()) {
            categories.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }
    
    public void setSelectedCategory(Category category) {
        this.selectedCategory = category;
        notifyDataSetChanged();
    }
    
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCategoryClick(categories.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(Category category) {
            tvCategoryName.setText(category.getName());
            
            // Gérer l'affichage de la sélection si en mode sélection
            if (selectionMode) {
                boolean isSelected = selectedCategory != null && 
                    selectedCategory.getId() != null && 
                    selectedCategory.getId().equals(category.getId());
                
                itemView.setSelected(isSelected);
                itemView.setBackgroundColor(isSelected ? 
                    itemView.getContext().getColor(R.color.selected_background) : 
                    itemView.getContext().getColor(android.R.color.transparent));
            }
        }
    }
}
