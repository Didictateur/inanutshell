package fr.didictateur.inanutshell.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityMainBinding;
import fr.didictateur.inanutshell.ui.recipes.RecipesFragment;
import fr.didictateur.inanutshell.ui.categories.CategoriesFragment;
import fr.didictateur.inanutshell.ui.favorites.FavoritesFragment;
import fr.didictateur.inanutshell.ui.settings.SettingsActivity;
import fr.didictateur.inanutshell.ui.search.SearchFilters;
import fr.didictateur.inanutshell.ui.search.SearchFilterListener;
import fr.didictateur.inanutshell.ui.search.ActiveFiltersAdapter;
import fr.didictateur.inanutshell.ui.search.AdvancedSearchActivity;
import fr.didictateur.inanutshell.ui.categories.CategorySelectionActivity;
import fr.didictateur.inanutshell.ui.tags.TagSelectionActivity;
import fr.didictateur.inanutshell.utils.MealiePreferences;
import fr.didictateur.inanutshell.utils.OfflineManager;
import fr.didictateur.inanutshell.utils.AccessibilityHelper;
import fr.didictateur.inanutshell.ThemeSettingsActivity;
import fr.didictateur.inanutshell.ui.shopping.ShoppingListsActivity;

public class MainActivity extends AppCompatActivity implements ActiveFiltersAdapter.OnFilterRemoveListener {
    
    private ActivityMainBinding binding;
    private MealiePreferences preferences;
    private OfflineManager offlineManager;
    private SearchFilters currentFilters = new SearchFilters();
    private SearchFilterListener currentFilterListener;
    private boolean isSearchVisible = false;
    private ActiveFiltersAdapter activeFiltersAdapter;
    
    // Request codes for activities
    private static final int REQUEST_SELECT_CATEGORIES = 1001;
    private static final int REQUEST_SELECT_TAGS = 1002;
    private static final int REQUEST_ADVANCED_SEARCH = 1003;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = new MealiePreferences(this);
        offlineManager = OfflineManager.getInstance(this);

        // Vérifier la configuration Mealie
        if (!preferences.hasValidCredentials()) {
            Toast.makeText(this, getString(R.string.mealie_setup_required), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, fr.didictateur.inanutshell.ui.setup.SetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setupToolbar();
        setupSearch();
        setupActiveFilters();
        setupViewPager();
        setupOfflineManager();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        
        // Setup search button click
        binding.searchButton.setOnClickListener(v -> toggleSearch());
        
        // Configuration de l'accessibilité
        AccessibilityHelper.configureButtonAccessibility(
            binding.searchButton, 
            "Ouvrir la recherche", 
            isSearchVisible
        );
    }
    
    private void setupSearch() {
        // Vérifier que les éléments existent avant de leur ajouter des listeners
        if (binding.searchEditText != null) {
            // Setup text search change listener
            binding.searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().trim();
                    currentFilters.setTextQuery(query);
                    AccessibilityHelper.configureSearchFieldAccessibility(
                        binding.searchEditText, 
                        "Rechercher des recettes", 
                        query
                    );
                    notifyFiltersChanged();
                }
            });
            
            // Handle search actions on keyboard
            binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                    if (binding.searchIngredientEditText != null) {
                        binding.searchIngredientEditText.requestFocus();
                    }
                    return true;
                }
                return false;
            });
        }
        
        if (binding.searchIngredientEditText != null) {
            // Setup ingredient search change listener
            binding.searchIngredientEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    String ingredient = s.toString().trim();
                    currentFilters.setIngredient(ingredient);
                    AccessibilityHelper.configureSearchFieldAccessibility(
                        binding.searchIngredientEditText, 
                        "Rechercher par ingrédient", 
                        ingredient
                    );
                    notifyFiltersChanged();
                }
            });
            
            binding.searchIngredientEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard();
                    return true;
                }
                return false;
            });
        }
        
        if (binding.maxPrepTimeEditText != null) {
            // Setup time filters change listeners
            binding.maxPrepTimeEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    String time = s.toString().trim();
                    Integer maxPrepTime = null;
                    if (!time.isEmpty()) {
                        try {
                            maxPrepTime = Integer.parseInt(time);
                        } catch (NumberFormatException e) {
                            // Invalid number, ignore
                        }
                    }
                    currentFilters.setMaxPrepTime(maxPrepTime);
                    notifyFiltersChanged();
                }
            });
        }
        
        if (binding.maxCookTimeEditText != null) {
            binding.maxCookTimeEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    String time = s.toString().trim();
                    Integer maxCookTime = null;
                    if (!time.isEmpty()) {
                        try {
                            maxCookTime = Integer.parseInt(time);
                        } catch (NumberFormatException e) {
                            // Invalid number, ignore
                        }
                    }
                    currentFilters.setMaxCookTime(maxCookTime);
                    notifyFiltersChanged();
                }
            });
        }
        
        // Setup action buttons - avec vérifications nulles
        if (binding.clearSearchButton != null) {
            binding.clearSearchButton.setOnClickListener(v -> clearAllSearch());
        }
        if (binding.advancedSearchButton != null) {
            binding.advancedSearchButton.setOnClickListener(v -> openAdvancedSearch());
        }
        
        // Setup category and tag selection buttons - avec vérifications nulles
        if (binding.selectCategoriesButton != null) {
            binding.selectCategoriesButton.setOnClickListener(v -> openCategorySelection());
        }
        if (binding.selectTagsButton != null) {
            binding.selectTagsButton.setOnClickListener(v -> openTagSelection());
        }
    }
    
    private void setupActiveFilters() {
        activeFiltersAdapter = new ActiveFiltersAdapter(this);
        binding.activeFiltersRecyclerView.setAdapter(activeFiltersAdapter);
        
        // Use horizontal linear layout manager for chips
        androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
            new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
        binding.activeFiltersRecyclerView.setLayoutManager(layoutManager);
    }
    
    private void notifyFiltersChanged() {
        // Notify current fragment about filter changes
        if (currentFilterListener != null) {
            currentFilterListener.onFiltersChanged(currentFilters);
        }
    }
    
    private void clearAllSearch() {
        binding.searchEditText.setText("");
        binding.searchIngredientEditText.setText("");
        binding.maxPrepTimeEditText.setText("");
        binding.maxCookTimeEditText.setText("");
        currentFilters.clear();
        updateActiveFiltersDisplay();
        
        if (currentFilterListener != null) {
            currentFilterListener.onClearFilters();
        }
    }
    
    private void openAdvancedSearch() {
        Intent intent = new Intent(this, AdvancedSearchActivity.class);
        
        // Pass current filters to advanced search
        intent.putExtra("text_query", currentFilters.getTextQuery());
        intent.putExtra("ingredient", currentFilters.getIngredient());
        
        if (currentFilters.getCategories() != null) {
            intent.putStringArrayListExtra("categories", new java.util.ArrayList<>(currentFilters.getCategories()));
        }
        
        if (currentFilters.getTags() != null) {
            intent.putStringArrayListExtra("tags", new java.util.ArrayList<>(currentFilters.getTags()));
        }
        
        if (currentFilters.getMaxPrepTime() != null) {
            intent.putExtra("max_prep_time", currentFilters.getMaxPrepTime());
        }
        
        if (currentFilters.getMaxCookTime() != null) {
            intent.putExtra("max_cook_time", currentFilters.getMaxCookTime());
        }
        
        intent.putExtra("favorites_only", currentFilters.isFavoritesOnly());
        
        if (currentFilters.getMaxDifficulty() != null) {
            intent.putExtra("max_difficulty", currentFilters.getMaxDifficulty().name());
        }
        
        startActivityForResult(intent, REQUEST_ADVANCED_SEARCH);
    }
    
    private void openCategorySelection() {
        Intent intent = new Intent(this, CategorySelectionActivity.class);
        
        // Pass currently selected categories
        if (currentFilters.getCategories() != null && !currentFilters.getCategories().isEmpty()) {
            intent.putStringArrayListExtra("selected_categories", 
                new java.util.ArrayList<>(currentFilters.getCategories()));
        }
        
        startActivityForResult(intent, REQUEST_SELECT_CATEGORIES);
    }
    
    private void openTagSelection() {
        Intent intent = new Intent(this, TagSelectionActivity.class);
        
        // Pass currently selected tags
        if (currentFilters.getTags() != null && !currentFilters.getTags().isEmpty()) {
            intent.putStringArrayListExtra("selected_tags", 
                new java.util.ArrayList<>(currentFilters.getTags()));
        }
        
        startActivityForResult(intent, REQUEST_SELECT_TAGS);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_SELECT_CATEGORIES:
                    java.util.ArrayList<String> selectedCategories = 
                        data.getStringArrayListExtra("selected_categories");
                    currentFilters.setCategories(selectedCategories);
                    updateActiveFiltersDisplay();
                    notifyFiltersChanged();
                    break;
                    
                case REQUEST_SELECT_TAGS:
                    java.util.ArrayList<String> selectedTags = 
                        data.getStringArrayListExtra("selected_tags");
                    currentFilters.setTags(selectedTags);
                    updateActiveFiltersDisplay();
                    notifyFiltersChanged();
                    break;
                    
                case REQUEST_ADVANCED_SEARCH:
                    // Load filters from advanced search
                    loadFiltersFromIntent(data);
                    updateActiveFiltersDisplay();
                    notifyFiltersChanged();
                    break;
            }
        }
    }
    
    // Implementation of OnFilterRemoveListener
    @Override
    public void onFilterRemove(String filterType, String filterValue) {
        switch (filterType) {
            case "category":
                if (currentFilters.getCategories() != null) {
                    currentFilters.getCategories().remove(filterValue);
                    if (currentFilters.getCategories().isEmpty()) {
                        currentFilters.setCategories(null);
                    }
                }
                break;
                
            case "tag":
                if (currentFilters.getTags() != null) {
                    currentFilters.getTags().remove(filterValue);
                    if (currentFilters.getTags().isEmpty()) {
                        currentFilters.setTags(null);
                    }
                }
                break;
                
            case "maxPrepTime":
                currentFilters.setMaxPrepTime(null);
                binding.maxPrepTimeEditText.setText("");
                break;
                
            case "maxCookTime":
                currentFilters.setMaxCookTime(null);
                binding.maxCookTimeEditText.setText("");
                break;
        }
        
        updateActiveFiltersDisplay();
        notifyFiltersChanged();
    }
    
    private void updateActiveFiltersDisplay() {
        java.util.List<ActiveFiltersAdapter.FilterItem> filterItems = new java.util.ArrayList<>();
        
        // Add category filters
        if (currentFilters.getCategories() != null && !currentFilters.getCategories().isEmpty()) {
            for (String category : currentFilters.getCategories()) {
                filterItems.add(new ActiveFiltersAdapter.FilterItem("category", category, "📁 " + category));
            }
        }
        
        // Add tag filters
        if (currentFilters.getTags() != null && !currentFilters.getTags().isEmpty()) {
            for (String tag : currentFilters.getTags()) {
                filterItems.add(new ActiveFiltersAdapter.FilterItem("tag", tag, "🏷️ " + tag));
            }
        }
        
        // Add time filters
        if (currentFilters.getMaxPrepTime() != null) {
            filterItems.add(new ActiveFiltersAdapter.FilterItem("maxPrepTime", 
                currentFilters.getMaxPrepTime().toString(), 
                "⏱️ Prépa ≤ " + currentFilters.getMaxPrepTime() + "min"));
        }
        
        if (currentFilters.getMaxCookTime() != null) {
            filterItems.add(new ActiveFiltersAdapter.FilterItem("maxCookTime", 
                currentFilters.getMaxCookTime().toString(), 
                "🔥 Cuisson ≤ " + currentFilters.getMaxCookTime() + "min"));
        }
        
        // Show/hide the filters RecyclerView
        if (filterItems.isEmpty()) {
            binding.activeFiltersRecyclerView.setVisibility(View.GONE);
        } else {
            binding.activeFiltersRecyclerView.setVisibility(View.VISIBLE);
            activeFiltersAdapter.setFilters(filterItems);
        }
    }
    
    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(getString(R.string.recipes));
                            tab.setIcon(R.drawable.ic_recipe);
                            break;
                        case 1:
                            tab.setText(getString(R.string.categories));
                            tab.setIcon(R.drawable.ic_category);
                            break;
                        case 2:
                            tab.setText(getString(R.string.favorites));
                            tab.setIcon(R.drawable.ic_favorite);
                            break;
                    }
                }).attach();
        
        // Listen to page changes to update filter listeners
        binding.viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateFilterListener(position);
            }
        });
    }
    
    private void setupOfflineManager() {
        // Écouter les changements de connexion
        offlineManager.addConnectionStatusListener(isOnline -> {
            runOnUiThread(() -> {
                // Mettre à jour l'interface selon l'état de connexion
                updateConnectionStatusUI(isOnline);
            });
        });
    }
    
    private void updateConnectionStatusUI(boolean isOnline) {
        // Afficher/masquer un indicateur de statut hors ligne
        // Ceci peut être étendu avec une SnackBar ou un badge dans la toolbar
        if (!isOnline) {
            // Mode hors ligne - on pourrait afficher un message ou changer la couleur de la toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("Mode hors ligne");
            }
        } else {
            // Mode en ligne
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(null);
            }
        }
    }
    
    private void updateFilterListener(int position) {
        // Reset current listener
        currentFilterListener = null;
        
        // Get current fragment and set it as listener if it supports search
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentByTag("f" + position);
        
        if (currentFragment instanceof SearchFilterListener) {
            setSearchFilterListener((SearchFilterListener) currentFragment);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_meal_planner) {
            startActivity(new Intent(this, fr.didictateur.inanutshell.MealPlannerActivity.class));
            return true;
        } else if (id == R.id.action_shopping_lists) {
            startActivity(new Intent(this, fr.didictateur.inanutshell.ui.shopping.ShoppingListsActivity.class));
            return true;
        } else if (id == R.id.action_timers) {
            startActivity(new Intent(this, fr.didictateur.inanutshell.TimersActivity.class));
            return true;
        } else if (id == R.id.action_themes) {
            startActivity(new Intent(this, fr.didictateur.inanutshell.ThemeSettingsActivity.class));
            return true;
        } else if (id == R.id.action_sync) {
            // TODO: Implement sync
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        // Si la recherche est visible, la fermer au lieu de quitter l'app
        if (isSearchVisible) {
            hideSearch();
        } else {
            // Minimiser l'app au lieu de la fermer
            moveTaskToBack(true);
        }
        super.onBackPressed();
    }
    
    private void toggleSearch() {
        if (isSearchVisible) {
            hideSearch();
        } else {
            showSearch();
        }
    }
    
    private void showSearch() {
        isSearchVisible = true;
        binding.searchContainer.setVisibility(View.VISIBLE);
        binding.searchEditText.requestFocus();
        showKeyboard();
        
        // Mise à jour de l'accessibilité
        AccessibilityHelper.configureButtonAccessibility(
            binding.searchButton, 
            "Fermer la recherche", 
            true
        );
        AccessibilityHelper.announceForAccessibility(
            binding.searchContainer, 
            "Panneau de recherche ouvert"
        );
    }
    
    private void hideSearch() {
        isSearchVisible = false;
        binding.searchContainer.setVisibility(View.GONE);
        binding.searchEditText.setText("");
        binding.searchIngredientEditText.setText("");
        binding.maxPrepTimeEditText.setText("");
        binding.maxCookTimeEditText.setText("");
        
        // Mise à jour de l'accessibilité
        AccessibilityHelper.configureButtonAccessibility(
            binding.searchButton, 
            "Ouvrir la recherche", 
            false
        );
        AccessibilityHelper.announceForAccessibility(
            binding.searchButton, 
            "Recherche fermée"
        );
        hideKeyboard();
        
        // Clear filters
        currentFilters.clear();
        updateActiveFiltersDisplay();
        if (currentFilterListener != null) {
            currentFilterListener.onClearFilters();
        }
    }
    
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && binding.searchEditText != null) {
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    
    /**
     * Méthode pour que les fragments puissent s'enregistrer pour recevoir les notifications de filtre
     */
    public void setSearchFilterListener(SearchFilterListener listener) {
        this.currentFilterListener = listener;
        
        // Send current filters to new listener
        if (listener != null && !currentFilters.isEmpty()) {
            listener.onFiltersChanged(currentFilters);
        }
    }
    
    /**
     * Méthode pour obtenir les filtres actuels
     */
    public SearchFilters getCurrentFilters() {
        return currentFilters;
    }
    
    private static class MainPagerAdapter extends FragmentStateAdapter {
        
        public MainPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new RecipesFragment();
                case 1:
                    return new CategoriesFragment();
                case 2:
                    return new FavoritesFragment();
                default:
                    return new RecipesFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
    
    private void loadFiltersFromIntent(Intent data) {
        // Load text query
        String textQuery = data.getStringExtra("text_query");
        if (textQuery != null) {
            currentFilters.setTextQuery(textQuery);
            binding.searchEditText.setText(textQuery);
        }
        
        // Load ingredient
        String ingredient = data.getStringExtra("ingredient");
        if (ingredient != null) {
            currentFilters.setIngredient(ingredient);
            binding.searchIngredientEditText.setText(ingredient);
        }
        
        // Load categories
        java.util.ArrayList<String> categories = data.getStringArrayListExtra("categories");
        currentFilters.setCategories(categories);
        
        // Load tags
        java.util.ArrayList<String> tags = data.getStringArrayListExtra("tags");
        currentFilters.setTags(tags);
        
        // Load time filters
        if (data.hasExtra("max_prep_time")) {
            int maxPrepTime = data.getIntExtra("max_prep_time", 0);
            currentFilters.setMaxPrepTime(maxPrepTime);
            binding.maxPrepTimeEditText.setText(String.valueOf(maxPrepTime));
        }
        
        if (data.hasExtra("max_cook_time")) {
            int maxCookTime = data.getIntExtra("max_cook_time", 0);
            currentFilters.setMaxCookTime(maxCookTime);
            binding.maxCookTimeEditText.setText(String.valueOf(maxCookTime));
        }
        
        // Load favorites filter
        boolean favoritesOnly = data.getBooleanExtra("favorites_only", false);
        currentFilters.setFavoritesOnly(favoritesOnly);
        
        // Load difficulty filter
        String difficultyName = data.getStringExtra("max_difficulty");
        if (difficultyName != null) {
            SearchFilters.DifficultyLevel difficulty = SearchFilters.DifficultyLevel.fromName(difficultyName);
            currentFilters.setMaxDifficulty(difficulty);
        } else {
            currentFilters.setMaxDifficulty(null);
        }
    }
    
    /**
     * Efface tous les champs de recherche
     */
    public void clearSearchFields() {
        binding.searchEditText.setText("");
        binding.searchIngredientEditText.setText("");
        binding.maxCookTimeEditText.setText("");
        
        // Reset filters
        currentFilters = new SearchFilters();
        
        // Hide search if visible
        if (isSearchVisible) {
            toggleSearch();
        }
        
        // Notify filter listener
        notifyFiltersChanged();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (offlineManager != null) {
            offlineManager.removeConnectionStatusListener(isOnline -> updateConnectionStatusUI(isOnline));
        }
    }
}
