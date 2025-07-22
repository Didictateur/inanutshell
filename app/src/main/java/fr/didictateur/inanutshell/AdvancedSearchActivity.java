package fr.didictateur.inanutshell;

import android.        // Initialiser la base de données
        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();ent.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedSearchActivity extends AppCompatActivity {
    
    private TextInputEditText searchName, searchIngredients;
    private Slider prepTimeSlider;
    private ChipGroup chipGroup;
    private MaterialButton btnSearch, btnClear;
    private MaterialToolbar toolbar;
    private TextView sliderValue;
    
    private AppDatabase database;
    private ExecutorService executor;
    
    // Tags disponibles
    private final String[] availableTags = {
        "Végétarien", "Végan", "Sans gluten", "Sans lactose",
        "Rapide", "Dessert", "Entrée", "Plat principal",
        "Facile", "Difficile", "Français", "Italien",
        "Asiatique", "Mexicain", "Healthy", "Comfort food"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_search);
        
        initializeViews();
        setupToolbar();
        setupTags();
        setupSlider();
        setupButtons();
        
        // Initialiser la base de données
        AppDatabase database = AppDatabase.getInstance(this);
        recetteDAO = database.recetteDAO();
        executor = Executors.newSingleThreadExecutor();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        searchName = findViewById(R.id.searchName);
        searchIngredients = findViewById(R.id.searchIngredients);
        prepTimeSlider = findViewById(R.id.prepTimeSlider);
        chipGroup = findViewById(R.id.chipGroup);
        btnSearch = findViewById(R.id.btnSearch);
        btnClear = findViewById(R.id.btnClear);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupTags() {
        for (String tag : availableTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.colorPrimary);
            chip.setTextColor(getResources().getColor(android.R.color.white));
            chipGroup.addView(chip);
        }
    }
    
    private void setupSlider() {
        prepTimeSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                // Afficher la valeur quelque part si nécessaire
            }
        });
    }
    
    private void setupButtons() {
        btnSearch.setOnClickListener(v -> performSearch());
        btnClear.setOnClickListener(v -> clearAllFields());
    }
    
    private void performSearch() {
        String name = searchName.getText() != null ? searchName.getText().toString() : "";
        String ingredients = searchIngredients.getText() != null ? searchIngredients.getText().toString() : "";
        int maxPrepTime = (int) prepTimeSlider.getValue();
        
        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }
        
        // Effectuer la recherche en arrière-plan
        executor.execute(() -> {
            List<Recette> results = searchRecipes(name, ingredients, maxPrepTime, selectedTags);
            
            runOnUiThread(() -> {
                // Retourner les résultats à l'activité principale
                Intent resultIntent = new Intent();
                ArrayList<Long> recipeIds = new ArrayList<>();
                for (Recette recette : results) {
                    recipeIds.add(recette.getId());
                }
                resultIntent.putExtra("search_results", recipeIds);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        });
    }
    
    private List<Recette> searchRecipes(String name, String ingredients, int maxPrepTime, List<String> tags) {
        List<Recette> allRecipes = recetteDAO.getAllRecettes();
        List<Recette> filteredRecipes = new ArrayList<>();
        
        for (Recette recette : allRecipes) {
            boolean matches = true;
            
            // Filtrer par nom
            if (!name.isEmpty() && !recette.getNom().toLowerCase().contains(name.toLowerCase())) {
                matches = false;
            }
            
            // Filtrer par ingrédients
            if (!ingredients.isEmpty() && matches) {
                String[] searchIngredients = ingredients.split(",");
                boolean hasIngredient = false;
                for (String ingredient : searchIngredients) {
                    if (recette.getIngredients().toLowerCase().contains(ingredient.trim().toLowerCase())) {
                        hasIngredient = true;
                        break;
                    }
                }
                if (!hasIngredient) {
                    matches = false;
                }
            }
            
            // Filtrer par temps de préparation
            if (matches && recette.getTempsPrepMin() > maxPrepTime) {
                matches = false;
            }
            
            // Filtrer par tags
            if (matches && !tags.isEmpty()) {
                boolean hasTag = false;
                String recipeTags = recette.getTags() != null ? recette.getTags().toLowerCase() : "";
                for (String tag : tags) {
                    if (recipeTags.contains(tag.toLowerCase())) {
                        hasTag = true;
                        break;
                    }
                }
                if (!hasTag) {
                    matches = false;
                }
            }
            
            if (matches) {
                filteredRecipes.add(recette);
            }
        }
        
        return filteredRecipes;
    }
    
    private void clearAllFields() {
        searchName.setText("");
        searchIngredients.setText("");
        prepTimeSlider.setValue(60);
        
        // Décocher tous les chips
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setChecked(false);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
