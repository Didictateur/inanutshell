package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {
    
    private EditText searchEditText;
    private ImageButton filterButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout noResultsLayout;
    
    // Chips de filtres
    private Chip chipFavorites, chipVegetarian, chipQuick, chipDessert;
    
    private SearchResultAdapter searchAdapter;
    private AppDatabase database;
    private ExecutorService executor;
    
    // Filtres actifs
    private boolean filterFavorites = false;
    private boolean filterVegetarian = false;
    private boolean filterQuick = false;
    private boolean filterDessert = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        initViews();
        setupDatabase();
        setupRecyclerView();
        setupSearchListener();
        setupFilterChips();
        setupFilterButton();
        
        // Focus automatique sur la barre de recherche
        searchEditText.requestFocus();
    }
    
    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        noResultsLayout = findViewById(R.id.noResultsLayout);
        
        chipFavorites = findViewById(R.id.chipFavorites);
        chipVegetarian = findViewById(R.id.chipVegetarian);
        chipQuick = findViewById(R.id.chipQuick);
        chipDessert = findViewById(R.id.chipDessert);
    }
    
    private void setupDatabase() {
        database = AppDatabase.getInstance(this);
        executor = Executors.newFixedThreadPool(2);
    }
    
    private void setupRecyclerView() {
        searchAdapter = new SearchResultAdapter(new ArrayList<>(), this::onRecipeClick);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);
    }
    
    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString().trim());
            }
        });
    }
    
    private void setupFilterChips() {
        chipFavorites.setOnCheckedChangeListener((view, isChecked) -> {
            filterFavorites = isChecked;
            performSearch(searchEditText.getText().toString().trim());
        });
        
        chipVegetarian.setOnCheckedChangeListener((view, isChecked) -> {
            filterVegetarian = isChecked;
            performSearch(searchEditText.getText().toString().trim());
        });
        
        chipQuick.setOnCheckedChangeListener((view, isChecked) -> {
            filterQuick = isChecked;
            performSearch(searchEditText.getText().toString().trim());
        });
        
        chipDessert.setOnCheckedChangeListener((view, isChecked) -> {
            filterDessert = isChecked;
            performSearch(searchEditText.getText().toString().trim());
        });
    }
    
    private void setupFilterButton() {
        filterButton.setOnClickListener(v -> {
            // TODO: Ouvrir dialog de filtres avancés
            showAdvancedFiltersDialog();
        });
    }
    
    private void performSearch(String query) {
        executor.execute(() -> {
            List<Recette> results;
            
            if (query.isEmpty() && !hasActiveFilters()) {
                // Pas de recherche, montrer les recettes récentes
                results = database.recetteDao().getRecentRecipes(20);
            } else {
                // Recherche avec query et filtres
                results = searchRecipes(query);
            }
            
            runOnUiThread(() -> {
                updateSearchResults(results);
            });
        });
    }
    
    private List<Recette> searchRecipes(String query) {
        List<Recette> allResults = new ArrayList<>();
        
        if (!query.isEmpty()) {
            // Recherche par nom
            allResults.addAll(database.recetteDao().searchByName("%" + query + "%"));
            
            // Recherche par ingrédients
            allResults.addAll(database.recetteDao().searchByIngredients("%" + query + "%"));
        } else {
            // Si pas de query mais des filtres actifs
            allResults.addAll(database.recetteDao().getAllRecipes());
        }
        
        // Appliquer les filtres
        return applyFilters(allResults);
    }
    
    private List<Recette> applyFilters(List<Recette> recipes) {
        List<Recette> filtered = new ArrayList<>();
        
        for (Recette recipe : recipes) {
            boolean include = true;
            
            // Filtre favoris (simulé pour l'instant)
            if (filterFavorites) {
                // TODO: Implémenter le système de favoris
                // include = recipe.isFavorite();
            }
            
            // Filtre végétarien
            if (filterVegetarian) {
                include = include && isVegetarian(recipe);
            }
            
            // Filtre rapide (<30min)
            if (filterQuick) {
                include = include && isQuickRecipe(recipe);
            }
            
            // Filtre dessert
            if (filterDessert) {
                include = include && isDessert(recipe);
            }
            
            if (include && !filtered.contains(recipe)) {
                filtered.add(recipe);
            }
        }
        
        return filtered;
    }
    
    private boolean hasActiveFilters() {
        return filterFavorites || filterVegetarian || filterQuick || filterDessert;
    }
    
    private boolean isVegetarian(Recette recipe) {
        // Recherche de mots-clés végétariens dans les ingrédients
        String ingredients = recipe.getIngredients().toLowerCase();
        String[] meatKeywords = {"viande", "porc", "boeuf", "agneau", "poulet", "volaille", 
                               "jambon", "lard", "bacon", "saucisse", "chorizo"};
        
        for (String keyword : meatKeywords) {
            if (ingredients.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isQuickRecipe(Recette recipe) {
        // Analyse du temps de cuisson dans les instructions
        String instructions = recipe.getInstructions().toLowerCase();
        
        // Recherche de patterns temporels
        if (instructions.contains("30 min") || instructions.contains("25 min") ||
            instructions.contains("20 min") || instructions.contains("15 min") ||
            instructions.contains("10 min") || instructions.contains("5 min")) {
            return true;
        }
        
        // Si pas d'indication temporelle, considérer comme rapide si peu d'étapes
        return recipe.getInstructions().split("\n").length <= 3;
    }
    
    private boolean isDessert(Recette recipe) {
        String name = recipe.getNom().toLowerCase();
        String ingredients = recipe.getIngredients().toLowerCase();
        
        String[] dessertKeywords = {"gâteau", "tarte", "mousse", "crème", "dessert", 
                                  "chocolat", "sucre", "vanille", "fruits", "glace"};
        
        for (String keyword : dessertKeywords) {
            if (name.contains(keyword) || ingredients.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private void updateSearchResults(List<Recette> results) {
        if (results.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            noResultsLayout.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            noResultsLayout.setVisibility(View.GONE);
            searchAdapter.updateResults(results);
        }
    }
    
    private void showAdvancedFiltersDialog() {
        // TODO: Implémenter dialog de filtres avancés
        // Pourrait inclure: temps de cuisson précis, difficulté, nombre de personnes, etc.
    }
    
    private void onRecipeClick(Recette recipe) {
        Intent intent = new Intent(this, DetailRecetteActivity.class);
        intent.putExtra("recetteId", recipe.getId());
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
