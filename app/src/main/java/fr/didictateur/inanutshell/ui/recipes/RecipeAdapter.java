package fr.didictateur.inanutshell.ui.recipes;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ItemRecipeBinding;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.utils.ImageLoader;
import fr.didictateur.inanutshell.utils.AccessibilityHelper;

public class RecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_RECIPE = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    
    private final List<Recipe> recipes;
    private final OnRecipeClickListener listener;
    private boolean isLoadingMoreEnabled = false;
    
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onRecipeFavoriteClick(Recipe recipe);
        void onRecipeLongClick(Recipe recipe, android.view.View view);
        void onRecipeImageClick(Recipe recipe);
        void onRecipeRatingClick(Recipe recipe);
    }
    
    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }
    
    public void setLoadingMoreEnabled(boolean enabled) {
        boolean wasEnabled = isLoadingMoreEnabled;
        isLoadingMoreEnabled = enabled;
        
        if (wasEnabled && !enabled) {
            // Remove loading item
            notifyItemRemoved(recipes.size());
        } else if (!wasEnabled && enabled) {
            // Add loading item
            notifyItemInserted(recipes.size());
        }
    }
    
    public List<Recipe> getRecipes() {
        return recipes;
    }
    
    private int getViewType(int position) {
        if (position == recipes.size() && isLoadingMoreEnabled) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_RECIPE;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            android.view.View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            ItemRecipeBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.getContext()),
                    R.layout.item_recipe,
                    parent,
                    false
            );
            return new RecipeViewHolder(binding);
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        return getViewType(position);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            // Nothing to bind for loading view
            return;
        }
        
        RecipeViewHolder recipeHolder = (RecipeViewHolder) holder;
        Recipe recipe = recipes.get(position);
        recipeHolder.bind(recipe, listener);
    }
    
    @Override
    public int getItemCount() {
        return recipes.size() + (isLoadingMoreEnabled ? 1 : 0);
    }
    
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(android.view.View view) {
            super(view);
        }
    }
    
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecipeBinding binding;
        
        public RecipeViewHolder(ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        public void bind(Recipe recipe, OnRecipeClickListener listener) {
            binding.tvName.setText(recipe.getName());
            binding.tvDescription.setText(recipe.getDescription());
            
            // Set favorite icon
            binding.btnFavorite.setImageResource(
                recipe.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
            );
            
            // Load recipe image with thumbnail optimization
            ImageLoader.loadRecipeImageThumbnail(
                binding.ivImage.getContext(), 
                recipe.getId(), 
                binding.ivImage,
                300, // width
                200  // height
            );
            
            // Set click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe);
                }
            });
            
            // Click sur l'image pour l'afficher en plein écran
            binding.ivImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeImageClick(recipe);
                }
            });

            // Set long click listener for context menu
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeLongClick(recipe, v);
                    return true;
                }
                return false;
            });

            binding.btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeFavoriteClick(recipe);
                }
            });

            // Rating button click
            binding.btnRating.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeRatingClick(recipe);
                }
            });
            
            // Set rating display and button state
            if (recipe.hasUserRating()) {
                // Show filled star and rating badge
                binding.btnRating.setImageResource(R.drawable.ic_star_filled);
                binding.starRating.setRating(recipe.getUserRating());
                binding.starRating.setInteractive(false); // Read-only in list
                binding.layoutRating.setVisibility(android.view.View.VISIBLE);
            } else {
                // Show empty star and hide rating badge
                binding.btnRating.setImageResource(R.drawable.ic_star_empty);
                binding.layoutRating.setVisibility(android.view.View.GONE);
            }
            
            // Click on rating to open rating dialog
            binding.layoutRating.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeRatingClick(recipe);
                }
            });
            
            // Show prep time if available
            if (recipe.getPrepTime() != null && !recipe.getPrepTime().isEmpty()) {
                binding.tvPrepTime.setText(recipe.getPrepTime());
                binding.tvPrepTime.setVisibility(android.view.View.VISIBLE);
                binding.ivClock.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvPrepTime.setVisibility(android.view.View.GONE);
                binding.ivClock.setVisibility(android.view.View.GONE);
            }
            
            // Configuration de l'accessibilité
            AccessibilityHelper.configureRecipeItemAccessibility(
                binding.getRoot(), 
                recipe.getName(), 
                recipe.getDescription(), 
                recipe.isFavorite(), 
                recipe.getUserRating()
            );
            
            // Configuration de l'accessibilité pour les gestes de swipe
            AccessibilityHelper.configureSwipeAccessibility(
                binding.getRoot(),
                "noter la recette",
                "ajouter aux favoris"
            );
            
            // Configuration des éléments spécifiques
            AccessibilityHelper.configureButtonAccessibility(
                binding.btnFavorite, 
                "Favori", 
                recipe.isFavorite()
            );
            
            if (binding.tvPrepTime.getVisibility() == android.view.View.VISIBLE) {
                binding.tvPrepTime.setContentDescription(
                    "Temps de préparation: " + recipe.getPrepTime()
                );
            }
        }
    }
}
