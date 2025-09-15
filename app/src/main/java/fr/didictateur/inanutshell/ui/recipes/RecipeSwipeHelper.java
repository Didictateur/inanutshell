package fr.didictateur.inanutshell.ui.recipes;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import fr.didictateur.inanutshell.data.model.Recipe;

/**
 * Helper pour les gestures de swipe sur les recettes
 */
public class RecipeSwipeHelper extends ItemTouchHelper.SimpleCallback {
    
    public interface OnRecipeSwipeListener {
        void onRecipeSwipeRight(Recipe recipe, int position);
        void onRecipeSwipeLeft(Recipe recipe, int position);
    }
    
    private final RecipeAdapter adapter;
    private final OnRecipeSwipeListener swipeListener;
    
    public RecipeSwipeHelper(RecipeAdapter adapter, OnRecipeSwipeListener swipeListener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.swipeListener = swipeListener;
    }
    
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // Pas de drag & drop pour l'instant
        return false;
    }
    
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        
        // Ne pas traiter les swipes sur l'indicateur de chargement
        if (position >= adapter.getRecipes().size()) {
            adapter.notifyItemChanged(position);
            return;
        }
        
        Recipe recipe = adapter.getRecipes().get(position);
        
        if (direction == ItemTouchHelper.RIGHT) {
            // Swipe à droite = ajouter/retirer des favoris
            swipeListener.onRecipeSwipeRight(recipe, position);
        } else if (direction == ItemTouchHelper.LEFT) {
            // Swipe à gauche = noter la recette
            swipeListener.onRecipeSwipeLeft(recipe, position);
        }
        
        // Restaurer l'item à sa position
        adapter.notifyItemChanged(position);
    }
    
    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Désactiver le swipe pour l'indicateur de chargement
        if (viewHolder instanceof RecipeAdapter.LoadingViewHolder) {
            return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
    }
}
