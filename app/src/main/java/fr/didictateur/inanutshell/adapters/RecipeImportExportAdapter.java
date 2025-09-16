package fr.didictateur.inanutshell.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Recipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeImportExportAdapter extends RecyclerView.Adapter<RecipeImportExportAdapter.ViewHolder> {
    
    public enum Mode {
        IMPORT,
        EXPORT
    }
    
    private Context context;
    private List<Recipe> recipes;
    private Set<Recipe> selectedRecipes;
    private Mode mode;
    private OnRecipeSelectionChangedListener listener;
    
    public interface OnRecipeSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
        void onRecipeClicked(Recipe recipe);
    }
    
    public RecipeImportExportAdapter(Context context, Mode mode) {
        this.context = context;
        this.mode = mode;
        this.recipes = new ArrayList<>();
        this.selectedRecipes = new HashSet<>();
    }
    
    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? new ArrayList<>(recipes) : new ArrayList<>();
        this.selectedRecipes.clear();
        notifyDataSetChanged();
    }
    
    public void setOnRecipeSelectionChangedListener(OnRecipeSelectionChangedListener listener) {
        this.listener = listener;
    }
    
    public List<Recipe> getSelectedRecipes() {
        return new ArrayList<>(selectedRecipes);
    }
    
    public void selectAll() {
        selectedRecipes.clear();
        selectedRecipes.addAll(recipes);
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    public void deselectAll() {
        selectedRecipes.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    public boolean isAllSelected() {
        return !recipes.isEmpty() && selectedRecipes.size() == recipes.size();
    }
    
    public int getSelectedCount() {
        return selectedRecipes.size();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_import_export, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }
    
    @Override
    public int getItemCount() {
        return recipes.size();
    }
    
    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(selectedRecipes.size(), recipes.size());
        }
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView categoryTextView;
        private TextView descriptionTextView;
        private ImageView thumbnailImageView;
        private CheckBox selectionCheckBox;
        private View itemContainer;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
            selectionCheckBox = itemView.findViewById(R.id.selectionCheckBox);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            
            // Set click listeners
            itemContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Recipe recipe = recipes.get(position);
                        toggleSelection(recipe);
                        
                        if (listener != null) {
                            listener.onRecipeClicked(recipe);
                        }
                    }
                }
            });
            
            selectionCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Recipe recipe = recipes.get(position);
                        toggleSelection(recipe);
                    }
                }
            });
        }
        
        public void bind(Recipe recipe) {
            if (recipe == null) return;
            
            // Set recipe title
            titleTextView.setText(recipe.getName() != null ? recipe.getName() : "Recette sans nom");
            
            // Set category
            if (recipe.getRecipeCategory() != null && !recipe.getRecipeCategory().isEmpty()) {
                categoryTextView.setText(String.join(", ", recipe.getRecipeCategory()));
                categoryTextView.setVisibility(View.VISIBLE);
            } else {
                categoryTextView.setVisibility(View.GONE);
            }
            
            // Set description or ingredients count
            String description = getRecipeDescription(recipe);
            descriptionTextView.setText(description);
            
            // Set selection state
            boolean isSelected = selectedRecipes.contains(recipe);
            selectionCheckBox.setChecked(isSelected);
            
            // Visual feedback for selection
            itemContainer.setAlpha(isSelected ? 0.8f : 1.0f);
            
            // Load thumbnail if available
            if (recipe.getImage() != null && !recipe.getImage().isEmpty()) {
                // In a real implementation, you would use an image loading library like Glide or Picasso
                // Glide.with(context).load(recipe.getImage()).into(thumbnailImageView);
                thumbnailImageView.setVisibility(View.VISIBLE);
                thumbnailImageView.setImageResource(R.drawable.appicon); // Placeholder
            } else {
                thumbnailImageView.setVisibility(View.GONE);
            }
        }
        
        private String getRecipeDescription(Recipe recipe) {
            StringBuilder description = new StringBuilder();
            
            // Add ingredient count
            if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
                int ingredientCount = recipe.getRecipeIngredient().size();
                description.append(ingredientCount).append(" ingrédient");
                if (ingredientCount > 1) description.append("s");
            }
            
            // Add cooking time if available
            if (recipe.getCookTime() != null && !recipe.getCookTime().isEmpty()) {
                if (description.length() > 0) description.append(" • ");
                description.append(recipe.getCookTime());
            }
            
            // Add preparation time if available  
            if (recipe.getPrepTime() != null && !recipe.getPrepTime().isEmpty() && !recipe.getPrepTime().equals("0")) {
                if (description.length() > 0) description.append(" • ");
                description.append("Prep: ").append(recipe.getPrepTime()).append(" min");
            }
            
            // Add difficulty if available
            if (recipe.getDifficulty() != null && recipe.getDifficulty() > 0) {
                if (description.length() > 0) description.append(" • ");
                description.append("Difficulté: ").append(recipe.getDifficulty());
            }
            
            return description.length() > 0 ? description.toString() : "Aucune information disponible";
        }
        
        private void toggleSelection(Recipe recipe) {
            if (selectedRecipes.contains(recipe)) {
                selectedRecipes.remove(recipe);
            } else {
                selectedRecipes.add(recipe);
            }
            
            // Update checkbox state
            selectionCheckBox.setChecked(selectedRecipes.contains(recipe));
            
            // Update visual feedback
            itemContainer.setAlpha(selectedRecipes.contains(recipe) ? 0.8f : 1.0f);
            
            notifySelectionChanged();
        }
    }
    
    // Filter methods
    public void filterByCategory(String category) {
        // Implementation for filtering by category
        // This would be implemented based on your filtering requirements
    }
    
    public void filterByText(String searchText) {
        // Implementation for text-based filtering
        // This would be implemented based on your search requirements
    }
    
    // Utility methods
    public void updateRecipe(Recipe updatedRecipe) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getId() == updatedRecipe.getId()) {
                recipes.set(i, updatedRecipe);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void removeRecipe(Recipe recipe) {
        int position = recipes.indexOf(recipe);
        if (position >= 0) {
            recipes.remove(position);
            selectedRecipes.remove(recipe);
            notifyItemRemoved(position);
            notifySelectionChanged();
        }
    }
    
    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
        notifyItemInserted(recipes.size() - 1);
    }
}
