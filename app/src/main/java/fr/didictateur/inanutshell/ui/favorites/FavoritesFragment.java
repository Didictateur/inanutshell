package fr.didictateur.inanutshell.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.FragmentFavoritesBinding;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.ui.recipes.RecipeAdapter;
import fr.didictateur.inanutshell.ui.image.FullscreenImageActivity;
import fr.didictateur.inanutshell.utils.FavoritesManager;
import fr.didictateur.inanutshell.utils.MealiePreferences;
import fr.didictateur.inanutshell.ui.dialogs.RatingDialog;
import fr.didictateur.inanutshell.utils.RatingManager;

public class FavoritesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {
    
    private FragmentFavoritesBinding binding;
    private RecipeAdapter adapter;
    private List<Recipe> favoriteRecipes = new ArrayList<>();
    private MealiePreferences preferences;
    private FavoritesManager favoritesManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        preferences = new MealiePreferences(requireContext());
        favoritesManager = FavoritesManager.getInstance(requireContext());
        
        setupRecyclerView();
        setupSwipeRefresh();
        loadFavoriteRecipes();
    }
    
    private void setupRecyclerView() {
        adapter = new RecipeAdapter(favoriteRecipes, this);
        
        int spanCount = getResources().getInteger(R.integer.recipe_grid_span_count);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        
        binding.recyclerViewFavorites.setLayoutManager(layoutManager);
        binding.recyclerViewFavorites.setAdapter(adapter);
        binding.recyclerViewFavorites.setHasFixedSize(true);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshFavorites.setOnRefreshListener(this::loadFavoriteRecipes);
        binding.swipeRefreshFavorites.setColorSchemeResources(R.color.primary);
    }
    
    private void loadFavoriteRecipes() {
        if (!preferences.hasValidCredentials()) {
            binding.swipeRefreshFavorites.setRefreshing(false);
            showNoFavoritesMessage("Configuration requise", "Configurez votre serveur Mealie pour voir vos favoris");
            return;
        }
        
        Set<String> favoriteIds = favoritesManager.getFavoriteRecipeIds();
        
        if (favoriteIds.isEmpty()) {
            binding.swipeRefreshFavorites.setRefreshing(false);
            showNoFavoritesMessage("Aucun favori", "Ajoutez des recettes à vos favoris en appuyant sur ♡");
            return;
        }
        
        binding.swipeRefreshFavorites.setRefreshing(true);
        showLoading(true);
        
        // Load all recipes and filter favorites
        NetworkManager.getInstance().getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> allRecipes) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        favoriteRecipes.clear();
                        RatingManager ratingManager = RatingManager.getInstance(requireContext());
                        
                        for (Recipe recipe : allRecipes) {
                            if (favoriteIds.contains(recipe.getId())) {
                                recipe.setFavorite(true);
                                // Sync user rating
                                float userRating = ratingManager.getRating(recipe.getId());
                                recipe.setUserRating(userRating);
                                favoriteRecipes.add(recipe);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        showLoading(false);
                        binding.swipeRefreshFavorites.setRefreshing(false);
                        
                        if (favoriteRecipes.isEmpty()) {
                            showNoFavoritesMessage("Aucun favori trouvé", "Les recettes favorites pourraient avoir été supprimées du serveur");
                        } else {
                            showFavorites();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        binding.swipeRefreshFavorites.setRefreshing(false);
                        showNoFavoritesMessage("Erreur de chargement", error);
                    });
                }
            }
        });
    }
    
    private void showLoading(boolean loading) {
        binding.progressBarFavorites.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.recyclerViewFavorites.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.layoutEmptyFavorites.setVisibility(View.GONE);
    }
    
    private void showFavorites() {
        binding.progressBarFavorites.setVisibility(View.GONE);
        binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
        binding.layoutEmptyFavorites.setVisibility(View.GONE);
    }
    
    private void showNoFavoritesMessage(String title, String message) {
        binding.progressBarFavorites.setVisibility(View.GONE);
        binding.recyclerViewFavorites.setVisibility(View.GONE);
        binding.layoutEmptyFavorites.setVisibility(View.VISIBLE);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptyMessage.setText(message);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh favorites when returning to this tab
        loadFavoriteRecipes();
    }
    
    // RecipeAdapter.OnRecipeClickListener methods
    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), fr.didictateur.inanutshell.ui.recipe.RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("recipe_name", recipe.getName());
        startActivity(intent);
    }
    
    @Override
    public void onRecipeFavoriteClick(Recipe recipe) {
        favoritesManager.toggleFavorite(recipe.getId());
        recipe.setFavorite(favoritesManager.isFavorite(recipe.getId()));
        
        String message = recipe.isFavorite() ? 
            getString(R.string.added_to_favorites) : getString(R.string.removed_from_favorites);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        
        // Remove from list if unfavorited
        if (!recipe.isFavorite()) {
            favoriteRecipes.remove(recipe);
            adapter.notifyDataSetChanged();
            
            if (favoriteRecipes.isEmpty()) {
                showNoFavoritesMessage("Aucun favori", "Ajoutez des recettes à vos favoris en appuyant sur ♡");
            }
        }
    }
    
    @Override
    public void onRecipeImageClick(Recipe recipe) {
        String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipe.getId());
        
        Intent intent = new Intent(getContext(), FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(FullscreenImageActivity.EXTRA_RECIPE_NAME, recipe.getName());
        startActivity(intent);
    }
    
    @Override
    public void onRecipeRatingClick(Recipe recipe) {
        // Show rating dialog for the recipe
        RatingDialog ratingDialog = new RatingDialog(requireContext(), recipe, new RatingDialog.OnRatingSetListener() {
            @Override
            public void onRatingSet(Recipe updatedRecipe, float newRating) {
                // Update recipe in favorites list
                for (Recipe r : favoriteRecipes) {
                    if (r.getId().equals(updatedRecipe.getId())) {
                        r.setUserRating(newRating);
                        break;
                    }
                }
                
                // Refresh adapter
                adapter.notifyDataSetChanged();
                
                // Show feedback
                String message = newRating > 0 ? 
                    "Note sauvegardée !" : "Note supprimée !";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        
        ratingDialog.show();
    }
    
    @Override
    public void onRecipeLongClick(Recipe recipe, android.view.View view) {
        // Pour les favoris, on peut simplement ouvrir le détail
        onRecipeClick(recipe);
    }
}
