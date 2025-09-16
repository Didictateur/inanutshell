package fr.didictateur.inanutshell.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityAdvancedSearchBinding;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.ui.categories.CategorySelectionActivity;
import fr.didictateur.inanutshell.ui.tags.TagSelectionActivity;
import fr.didictateur.inanutshell.ui.components.DifficultySelector;

public class AdvancedSearchActivity extends AppCompatActivity implements ActiveFiltersAdapter.OnFilterRemoveListener {
    
    private ActivityAdvancedSearchBinding binding;
    private SearchFilters currentFilters = new SearchFilters();
    private ActiveFiltersAdapter selectedFiltersAdapter;
    private List<Recipe> allRecipes = new ArrayList<>();
    
    // Request codes
    private static final int REQUEST_SELECT_CATEGORIES = 1001;
    private static final int REQUEST_SELECT_TAGS = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdvancedSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupFilterDisplay();
        setupTextWatchers();
        setupButtons();
        setupDifficultySelector();
        
        loadRecipesForPreview();
        
        // Load existing filters if any
        loadInitialFilters();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupFilterDisplay() {
        selectedFiltersAdapter = new ActiveFiltersAdapter(this);
        binding.selectedFiltersRecyclerView.setAdapter(selectedFiltersAdapter);
        binding.selectedFiltersRecyclerView.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }
    
    private void setupTextWatchers() {
        // Text search watcher
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentFilters.setTextQuery(s.toString().trim());
                updatePreview();
            }
        });
        
        // Ingredient search watcher
        binding.ingredientEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentFilters.setIngredient(s.toString().trim());
                updatePreview();
            }
        });
        
        // Max prep time watcher
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
                        // Invalid number
                    }
                }
                currentFilters.setMaxPrepTime(maxPrepTime);
                updatePreview();
            }
        });
        
        // Max cook time watcher
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
                        // Invalid number
                    }
                }
                currentFilters.setMaxCookTime(maxCookTime);
                updatePreview();
            }
        });
        
        // Favorites checkbox
        binding.favoritesOnlyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentFilters.setFavoritesOnly(isChecked);
            updatePreview();
        });
    }
    
    private void setupButtons() {
        binding.selectCategoriesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CategorySelectionActivity.class);
            if (currentFilters.getCategories() != null && !currentFilters.getCategories().isEmpty()) {
                intent.putStringArrayListExtra("selected_categories", 
                    new ArrayList<>(currentFilters.getCategories()));
            }
            startActivityForResult(intent, REQUEST_SELECT_CATEGORIES);
        });
        
        binding.selectTagsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TagSelectionActivity.class);
            if (currentFilters.getTags() != null && !currentFilters.getTags().isEmpty()) {
                intent.putStringArrayListExtra("selected_tags", 
                    new ArrayList<>(currentFilters.getTags()));
            }
            startActivityForResult(intent, REQUEST_SELECT_TAGS);
        });
        
        binding.clearButton.setOnClickListener(v -> clearAllFilters());
        
        binding.applyButton.setOnClickListener(v -> applyFiltersAndFinish());
    }
    
    private void setupDifficultySelector() {
        binding.difficultySelector.setOnDifficultySelectedListener(new DifficultySelector.OnDifficultySelectedListener() {
            @Override
            public void onDifficultySelected(SearchFilters.DifficultyLevel difficulty) {
                currentFilters.setMaxDifficulty(difficulty);
                updateFilterDisplay();
                updatePreview();
            }
            
            @Override
            public void onDifficultyCleared() {
                currentFilters.setMaxDifficulty(null);
                updateFilterDisplay();
                updatePreview();
            }
        });
    }
    
    private void loadRecipesForPreview() {
        NetworkManager.getInstance().getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                allRecipes.clear();
                allRecipes.addAll(recipes);
                runOnUiThread(() -> updatePreview());
            }
            
            @Override
            public void onError(String error) {
                // Handle error
                runOnUiThread(() -> binding.resultCountText.setText("Erreur lors du chargement"));
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_SELECT_CATEGORIES:
                    ArrayList<String> selectedCategories = 
                        data.getStringArrayListExtra("selected_categories");
                    currentFilters.setCategories(selectedCategories);
                    updateFilterDisplay();
                    updatePreview();
                    break;
                    
                case REQUEST_SELECT_TAGS:
                    ArrayList<String> selectedTags = 
                        data.getStringArrayListExtra("selected_tags");
                    currentFilters.setTags(selectedTags);
                    updateFilterDisplay();
                    updatePreview();
                    break;
            }
        }
    }
    
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
        }
        
        updateFilterDisplay();
        updatePreview();
    }
    
    private void updateFilterDisplay() {
        List<ActiveFiltersAdapter.FilterItem> filterItems = new ArrayList<>();
        
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
        
        selectedFiltersAdapter.setFilters(filterItems);
    }
    
    private void updatePreview() {
        int matchingCount = 0;
        
        for (Recipe recipe : allRecipes) {
            if (matchesFilters(recipe)) {
                matchingCount++;
            }
        }
        
        String countText = matchingCount + " recette" + (matchingCount > 1 ? "s" : "") + " trouvée" + (matchingCount > 1 ? "s" : "");
        binding.resultCountText.setText(countText);
    }
    
    private boolean matchesFilters(Recipe recipe) {
        // Text search
        if (!currentFilters.getTextQuery().isEmpty()) {
            String query = currentFilters.getTextQuery().toLowerCase();
            String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
            String recipeDescription = recipe.getDescription() != null ? recipe.getDescription().toLowerCase() : "";
            
            if (!recipeName.contains(query) && !recipeDescription.contains(query)) {
                return false;
            }
        }
        
        // Ingredient search
        if (!currentFilters.getIngredient().isEmpty()) {
            String ingredientQuery = currentFilters.getIngredient().toLowerCase();
            boolean ingredientFound = false;
            
            if (recipe.getRecipeIngredient() != null) {
                for (fr.didictateur.inanutshell.data.model.RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                    String food = ingredient.getFood() != null ? ingredient.getFood().toLowerCase() : "";
                    String display = ingredient.getDisplay() != null ? ingredient.getDisplay().toLowerCase() : "";
                    String original = ingredient.getOriginalText() != null ? ingredient.getOriginalText().toLowerCase() : "";
                    
                    if (food.contains(ingredientQuery) || display.contains(ingredientQuery) || original.contains(ingredientQuery)) {
                        ingredientFound = true;
                        break;
                    }
                }
            }
            
            if (!ingredientFound) {
                return false;
            }
        }
        
        // Category filters
        if (currentFilters.getCategories() != null && !currentFilters.getCategories().isEmpty()) {
            boolean categoryMatches = false;
            
            if (recipe.getCategories() != null) {
                for (fr.didictateur.inanutshell.data.model.Category category : recipe.getCategories()) {
                    if (category.getName() != null && currentFilters.getCategories().contains(category.getName())) {
                        categoryMatches = true;
                        break;
                    }
                }
            }
            
            if (!categoryMatches) {
                return false;
            }
        }
        
        // Tag filters
        if (currentFilters.getTags() != null && !currentFilters.getTags().isEmpty()) {
            boolean tagMatches = false;
            
            if (recipe.getTags() != null) {
                for (fr.didictateur.inanutshell.data.model.Tag tag : recipe.getTags()) {
                    if (tag.getName() != null && currentFilters.getTags().contains(tag.getName())) {
                        tagMatches = true;
                        break;
                    }
                }
            }
            
            if (!tagMatches) {
                return false;
            }
        }
        
        // Time filters
        if (currentFilters.getMaxPrepTime() != null) {
            Integer recipePrepTime = parseTimeToMinutes(recipe.getPrepTime());
            if (recipePrepTime != null && recipePrepTime > currentFilters.getMaxPrepTime()) {
                return false;
            }
        }
        
        if (currentFilters.getMaxCookTime() != null) {
            Integer recipeCookTime = parseTimeToMinutes(recipe.getCookTime());
            if (recipeCookTime != null && recipeCookTime > currentFilters.getMaxCookTime()) {
                return false;
            }
        }
        
        // Favorites filter
        if (currentFilters.isFavoritesOnly() && !recipe.isFavorite()) {
            return false;
        }
        
        // Difficulty filter
        if (currentFilters.getMaxDifficulty() != null) {
            Integer recipeDifficulty = recipe.getDifficulty();
            if (recipeDifficulty != null && recipeDifficulty > currentFilters.getMaxDifficulty().getLevel()) {
                return false;
            }
        }
        
        return true;
    }
    
    private Integer parseTimeToMinutes(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            String time = timeString.trim().toUpperCase();
            
            // Handle ISO 8601 duration format (PT30M, PT1H30M)
            if (time.startsWith("PT")) {
                int minutes = 0;
                time = time.substring(2);
                
                if (time.contains("H")) {
                    int hIndex = time.indexOf("H");
                    String hoursStr = time.substring(0, hIndex);
                    minutes += Integer.parseInt(hoursStr) * 60;
                    time = time.substring(hIndex + 1);
                }
                
                if (time.contains("M")) {
                    int mIndex = time.indexOf("M");
                    String minutesStr = time.substring(0, mIndex);
                    if (!minutesStr.isEmpty()) {
                        minutes += Integer.parseInt(minutesStr);
                    }
                }
                
                return minutes;
            }
            
            if (time.endsWith("M")) {
                time = time.substring(0, time.length() - 1);
            }
            
            if (time.contains(":")) {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int mins = Integer.parseInt(parts[1]);
                return hours * 60 + mins;
            }
            
            return Integer.parseInt(time);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private void clearAllFilters() {
        binding.searchEditText.setText("");
        binding.ingredientEditText.setText("");
        binding.maxPrepTimeEditText.setText("");
        binding.maxCookTimeEditText.setText("");
        binding.favoritesOnlyCheckBox.setChecked(false);
        binding.difficultySelector.clearSelection();
        
        currentFilters.clear();
        updateFilterDisplay();
        updatePreview();
    }
    
    private void applyFiltersAndFinish() {
        Intent resultIntent = new Intent();
        
        // Pass the filters back to MainActivity
        resultIntent.putExtra("text_query", currentFilters.getTextQuery());
        resultIntent.putExtra("ingredient", currentFilters.getIngredient());
        
        if (currentFilters.getCategories() != null) {
            resultIntent.putStringArrayListExtra("categories", 
                new ArrayList<>(currentFilters.getCategories()));
        }
        
        if (currentFilters.getTags() != null) {
            resultIntent.putStringArrayListExtra("tags", 
                new ArrayList<>(currentFilters.getTags()));
        }
        
        if (currentFilters.getMaxPrepTime() != null) {
            resultIntent.putExtra("max_prep_time", currentFilters.getMaxPrepTime());
        }
        
        if (currentFilters.getMaxCookTime() != null) {
            resultIntent.putExtra("max_cook_time", currentFilters.getMaxCookTime());
        }
        
        if (currentFilters.getMaxDifficulty() != null) {
            resultIntent.putExtra("max_difficulty", currentFilters.getMaxDifficulty().name());
        }
        
        resultIntent.putExtra("favorites_only", currentFilters.isFavoritesOnly());
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    private void loadInitialFilters() {
        Intent intent = getIntent();
        
        // Load text query
        String textQuery = intent.getStringExtra("text_query");
        if (textQuery != null && !textQuery.isEmpty()) {
            currentFilters.setTextQuery(textQuery);
            binding.searchEditText.setText(textQuery);
        }
        
        // Load ingredient
        String ingredient = intent.getStringExtra("ingredient");
        if (ingredient != null && !ingredient.isEmpty()) {
            currentFilters.setIngredient(ingredient);
            binding.ingredientEditText.setText(ingredient);
        }
        
        // Load categories
        java.util.ArrayList<String> categories = intent.getStringArrayListExtra("categories");
        if (categories != null && !categories.isEmpty()) {
            currentFilters.setCategories(categories);
        }
        
        // Load tags
        java.util.ArrayList<String> tags = intent.getStringArrayListExtra("tags");
        if (tags != null && !tags.isEmpty()) {
            currentFilters.setTags(tags);
        }
        
        // Load time filters
        if (intent.hasExtra("max_prep_time")) {
            int maxPrepTime = intent.getIntExtra("max_prep_time", 0);
            if (maxPrepTime > 0) {
                currentFilters.setMaxPrepTime(maxPrepTime);
                binding.maxPrepTimeEditText.setText(String.valueOf(maxPrepTime));
            }
        }
        
        if (intent.hasExtra("max_cook_time")) {
            int maxCookTime = intent.getIntExtra("max_cook_time", 0);
            if (maxCookTime > 0) {
                currentFilters.setMaxCookTime(maxCookTime);
                binding.maxCookTimeEditText.setText(String.valueOf(maxCookTime));
            }
        }
        
        // Load favorites filter
        boolean favoritesOnly = intent.getBooleanExtra("favorites_only", false);
        if (favoritesOnly) {
            currentFilters.setFavoritesOnly(true);
            binding.favoritesOnlyCheckBox.setChecked(true);
        }
        
        // Load difficulty filter
        String difficultyName = intent.getStringExtra("max_difficulty");
        if (difficultyName != null) {
            SearchFilters.DifficultyLevel difficulty = SearchFilters.DifficultyLevel.fromName(difficultyName);
            if (difficulty != null) {
                currentFilters.setMaxDifficulty(difficulty);
                binding.difficultySelector.setSelectedDifficulty(difficulty);
            }
        }
        
        // Update display
        updateFilterDisplay();
        updatePreview();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
