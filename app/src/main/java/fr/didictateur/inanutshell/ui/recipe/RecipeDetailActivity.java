package fr.didictateur.inanutshell.ui.recipe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.RecipeInstruction;
import fr.didictateur.inanutshell.data.model.Tool;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.databinding.ActivityRecipeDetailBinding;
import fr.didictateur.inanutshell.utils.ImageLoader;
import fr.didictateur.inanutshell.utils.OfflineManager;

import java.util.List;

/**
 * Activité pour afficher les détails d'une recette
 */
public class RecipeDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "RecipeDetailActivity";
    public static final String EXTRA_RECIPE_ID = "recipe_id";
    
    private ActivityRecipeDetailBinding binding;
    private OfflineManager offlineManager;
    private String recipeId;
    private Recipe recipe;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_recipe_detail);
        
        setupToolbar();
        
        // Initialiser l'OfflineManager
        offlineManager = OfflineManager.getInstance(this);
        
        // Récupérer l'ID de la recette
        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        
        if (recipeId == null || recipeId.isEmpty()) {
            Toast.makeText(this, "Erreur: ID de recette manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Charger les détails de la recette
        loadRecipeDetails();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détail de la recette");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        android.view.MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (favoriteItem != null && recipe != null) {
            fr.didictateur.inanutshell.utils.FavoritesManager favoritesManager = 
                fr.didictateur.inanutshell.utils.FavoritesManager.getInstance(this);
            boolean isFavorite = favoritesManager.isFavorite(recipe.getId());
            
            favoriteItem.setIcon(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_favorite) {
            toggleFavorite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleFavorite() {
        if (recipe == null) return;
        
        fr.didictateur.inanutshell.utils.FavoritesManager favoritesManager = 
            fr.didictateur.inanutshell.utils.FavoritesManager.getInstance(this);
        
        favoritesManager.toggleFavorite(recipe.getId());
        boolean isFavorite = favoritesManager.isFavorite(recipe.getId());
        
        // Update menu icon
        invalidateOptionsMenu();
        
        // Show feedback
        String message = isFavorite ? 
            getString(R.string.added_to_favorites) : getString(R.string.removed_from_favorites);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void loadRecipeDetails() {
        showLoading(true);
        
        // Si hors ligne, essayer de charger depuis le cache
        if (!offlineManager.isOnline()) {
            loadFromCache();
            return;
        }
        
        // Si en ligne, charger depuis le réseau et mettre en cache
        NetworkManager.getInstance().getRecipeById(recipeId, new NetworkManager.RecipeCallback() {
            @Override
            public void onSuccess(Recipe loadedRecipe) {
                runOnUiThread(() -> {
                    recipe = loadedRecipe;
                    
                    // Mettre en cache la recette
                    offlineManager.cacheRecipe(loadedRecipe);
                    
                    displayRecipeDetails();
                    showLoading(false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erreur chargement recette: " + error);
                    
                    // En cas d'erreur réseau, essayer le cache
                    loadFromCache();
                });
            }
        });
    }
    
    /**
     * Charger la recette depuis le cache (mode hors ligne)
     */
    private void loadFromCache() {
        offlineManager.getCachedRecipe(recipeId, new OfflineManager.RecipeCacheCallback() {
            @Override
            public void onSuccess(Recipe cachedRecipe) {
                runOnUiThread(() -> {
                    recipe = cachedRecipe;
                    displayRecipeDetails();
                    showLoading(false);
                    
                    // Afficher un indicateur que c'est du cache
                    if (!offlineManager.isOnline()) {
                        Toast.makeText(RecipeDetailActivity.this, 
                            "Mode hors ligne - Données du cache", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erreur chargement depuis le cache: " + error);
                    Toast.makeText(RecipeDetailActivity.this, 
                        "Recette non disponible hors ligne", 
                        Toast.LENGTH_LONG).show();
                    showLoading(false);
                    finish(); // Fermer l'activité si impossible de charger
                });
            }
        });
    }
    
    private void displayRecipeDetails() {
        if (recipe == null) {
            return;
        }
        
        // Titre
        binding.tvTitle.setText(recipe.getName());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(recipe.getName());
        }
        
        // Sync favorite status
        fr.didictateur.inanutshell.utils.FavoritesManager favoritesManager = 
            fr.didictateur.inanutshell.utils.FavoritesManager.getInstance(this);
        recipe.setFavorite(favoritesManager.isFavorite(recipe.getId()));
        
        // Update menu to show correct favorite state
        invalidateOptionsMenu();
        
        // Description
        if (!TextUtils.isEmpty(recipe.getDescription())) {
            binding.tvDescription.setText(recipe.getDescription());
            binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            binding.tvDescription.setVisibility(View.GONE);
        }
        
        // Image de la recette
        if (recipe.getId() != null) {
            ImageLoader.loadRecipeImage(this, recipe.getId(), binding.ivRecipeImage);
            
            // Clic sur l'image pour l'afficher en plein écran
            binding.ivRecipeImage.setOnClickListener(v -> {
                Intent intent = new Intent(this, fr.didictateur.inanutshell.ui.image.FullscreenImageActivity.class);
                String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipe.getId());
                intent.putExtra(fr.didictateur.inanutshell.ui.image.FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
                intent.putExtra(fr.didictateur.inanutshell.ui.image.FullscreenImageActivity.EXTRA_RECIPE_NAME, recipe.getName());
                startActivity(intent);
            });
        }
        
        // Temps et portions
        displayTimingInfo();
        
        // Ingrédients
        displayIngredients();
        
        // Outils et Ustensiles
        displayTools();
        
        // Instructions
        displayInstructions();
        
        // Bouton d'édition
        binding.fabEdit.setOnClickListener(v -> openEditActivity());
    }
    
    private void displayTimingInfo() {
        // Temps de préparation
        if (!TextUtils.isEmpty(recipe.getPrepTime())) {
            binding.tvPrepTime.setText("Préparation: " + recipe.getPrepTime());
            binding.tvPrepTime.setVisibility(View.VISIBLE);
        } else {
            binding.tvPrepTime.setVisibility(View.GONE);
        }
        
        // Temps de cuisson
        if (!TextUtils.isEmpty(recipe.getCookTime())) {
            binding.tvCookTime.setText("Cuisson: " + recipe.getCookTime());
            binding.tvCookTime.setVisibility(View.VISIBLE);
        } else {
            binding.tvCookTime.setVisibility(View.GONE);
        }
        
        // Temps total
        if (!TextUtils.isEmpty(recipe.getTotalTime())) {
            binding.tvTotalTime.setText("Total: " + recipe.getTotalTime());
            binding.tvTotalTime.setVisibility(View.VISIBLE);
        } else {
            binding.tvTotalTime.setVisibility(View.GONE);
        }
        
        // Portions
        if (!TextUtils.isEmpty(recipe.getRecipeYield())) {
            binding.tvServings.setText("Portions: " + recipe.getRecipeYield());
            binding.tvServings.setVisibility(View.VISIBLE);
        } else {
            binding.tvServings.setVisibility(View.GONE);
        }
    }
    
    private void displayIngredients() {
        List<RecipeIngredient> ingredients = recipe.getRecipeIngredient();
        
        if (ingredients != null && !ingredients.isEmpty()) {
            StringBuilder ingredientsText = new StringBuilder();
            
            for (RecipeIngredient ingredient : ingredients) {
                if (ingredient.getDisplay() != null && !ingredient.getDisplay().isEmpty()) {
                    ingredientsText.append("• ").append(ingredient.getDisplay()).append("\n");
                } else if (ingredient.getNote() != null && !ingredient.getNote().isEmpty()) {
                    ingredientsText.append("• ").append(ingredient.getNote()).append("\n");
                }
            }
            
            if (ingredientsText.length() > 0) {
                // Supprimer le dernier \n
                ingredientsText.setLength(ingredientsText.length() - 1);
                binding.tvIngredients.setText(ingredientsText.toString());
                binding.layoutIngredients.setVisibility(View.VISIBLE);
            } else {
                binding.layoutIngredients.setVisibility(View.GONE);
            }
        } else {
            binding.layoutIngredients.setVisibility(View.GONE);
        }
    }
    
    private void displayTools() {
        List<Tool> tools = recipe.getTools();
        
        if (tools != null && !tools.isEmpty()) {
            // Effacer les chips existantes
            binding.chipGroupTools.removeAllViews();
            
            // Créer une chip pour chaque outil
            for (Tool tool : tools) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(tool.getName());
                chip.setChipIcon(getDrawable(R.drawable.ic_tool)); // Nous créerons cette icône
                chip.setCheckable(false);
                chip.setClickable(false);
                
                // Style de la chip
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(getColor(R.color.chip_text));
                
                binding.chipGroupTools.addView(chip);
            }
            
            binding.layoutTools.setVisibility(View.VISIBLE);
        } else {
            binding.layoutTools.setVisibility(View.GONE);
        }
    }
    
    private void displayInstructions() {
        List<RecipeInstruction> instructions = recipe.getRecipeInstructions();
        
        if (instructions != null && !instructions.isEmpty()) {
            StringBuilder instructionsText = new StringBuilder();
            
            for (int i = 0; i < instructions.size(); i++) {
                RecipeInstruction instruction = instructions.get(i);
                if (instruction.getText() != null && !instruction.getText().isEmpty()) {
                    instructionsText.append(i + 1).append(". ")
                        .append(instruction.getText()).append("\n\n");
                }
            }
            
            if (instructionsText.length() > 0) {
                // Supprimer les derniers \n\n
                instructionsText.setLength(instructionsText.length() - 2);
                binding.tvInstructions.setText(instructionsText.toString());
                binding.layoutInstructions.setVisibility(View.VISIBLE);
            } else {
                binding.layoutInstructions.setVisibility(View.GONE);
            }
        } else {
            binding.layoutInstructions.setVisibility(View.GONE);
        }
    }
    
    private void openEditActivity() {
        Intent intent = new Intent(this, fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
        intent.putExtra("recipe_id", recipeId);
        startActivity(intent);
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.fabEdit.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
