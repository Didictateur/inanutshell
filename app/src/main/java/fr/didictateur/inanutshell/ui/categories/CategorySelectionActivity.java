package fr.didictateur.inanutshell.ui.categories;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityCategorySelectionBinding;
import fr.didictateur.inanutshell.data.manager.CategoryTagManager;
import fr.didictateur.inanutshell.data.model.Category;
import fr.didictateur.inanutshell.ui.adapter.CategoryAdapter;
import java.util.List;
import java.util.ArrayList;

public class CategorySelectionActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    
    private static final String TAG_LOG = "CategorySelectionActivity";
    public static final String EXTRA_SELECTED_CATEGORY = "selected_category";
    public static final String EXTRA_RESULT_CATEGORY = "result_category";
    
    private ActivityCategorySelectionBinding binding;
    private CategoryAdapter categoryAdapter;
    private CategoryTagManager categoryTagManager;
    private Category selectedCategory;
    private List<Category> allCategories = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategorySelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        loadInitialCategory();
        loadCategories();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.category_selection);
        }
        
        binding.btnDone.setOnClickListener(v -> finishWithResult());
        
        // Gestion de "Aucune catégorie"
        binding.cardNoCategory.setOnClickListener(v -> onClearSelection());
        
        // Gestion de la création de nouvelles catégories
        binding.btnCreateCategory.setOnClickListener(v -> createNewCategory());
        
        // Création par appui sur "Enter"
        binding.etNewCategory.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createNewCategory();
                return true;
            }
            return false;
        });
    }
    
    private void setupRecyclerView() {
        categoryTagManager = CategoryTagManager.getInstance();
        categoryAdapter = new CategoryAdapter(this);
        categoryAdapter.setSelectionMode(true);
        
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
    }
    
    private void loadInitialCategory() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_SELECTED_CATEGORY)) {
            String categoryId = intent.getStringExtra(EXTRA_SELECTED_CATEGORY);
            if (categoryId != null) {
                Category category = categoryTagManager.getCategoryById(categoryId);
                if (category != null) {
                    selectedCategory = category;
                }
            }
        }
    }
    
    private void loadCategories() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        
        categoryTagManager.getCategories(new CategoryTagManager.CategoriesCallback() {
            @Override
            public void onSuccess(List<Category> categories) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    allCategories = new ArrayList<>(categories);
                    categoryAdapter.setCategories(categories);
                    categoryAdapter.setSelectedCategory(selectedCategory);
                    updateDoneButton();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    Log.e(TAG_LOG, "Erreur lors du chargement des catégories: " + error);
                });
            }
        });
    }
    
    @Override
    public void onCategoryClick(Category category) {
        selectedCategory = category;
        categoryAdapter.setSelectedCategory(selectedCategory);
        updateDoneButton();
    }
    
    @Override
    public void onClearSelection() {
        selectedCategory = null;
        categoryAdapter.setSelectedCategory(null);
        updateDoneButton();
    }
    
    private void updateDoneButton() {
        binding.btnDone.setEnabled(true);
    }
    
    private void createNewCategory() {
        String categoryName = binding.etNewCategory.getText().toString().trim();
        
        if (TextUtils.isEmpty(categoryName)) {
            binding.etNewCategory.setError("Le nom de la catégorie ne peut pas être vide");
            return;
        }
        
        // Vérifier si la catégorie existe déjà
        for (Category existingCategory : allCategories) {
            if (existingCategory.getName().equalsIgnoreCase(categoryName)) {
                binding.etNewCategory.setError("Cette catégorie existe déjà");
                return;
            }
        }
        
        // Désactiver le bouton pendant la création
        binding.btnCreateCategory.setEnabled(false);
        binding.btnCreateCategory.setText("Création...");
        
        categoryTagManager.createCategory(categoryName, new CategoryTagManager.CategoryCreateCallback() {
            @Override
            public void onSuccess(Category category) {
                runOnUiThread(() -> {
                    // Ajouter la nouvelle catégorie à la liste
                    allCategories.add(category);
                    
                    // Sélectionner automatiquement la nouvelle catégorie
                    selectedCategory = category;
                    
                    // Mettre à jour l'adapter
                    categoryAdapter.setCategories(allCategories);
                    categoryAdapter.setSelectedCategory(selectedCategory);
                    
                    // Vider le champ de texte
                    binding.etNewCategory.setText("");
                    binding.tilNewCategory.setError(null);
                    
                    // Réactiver le bouton
                    binding.btnCreateCategory.setEnabled(true);
                    binding.btnCreateCategory.setText(R.string.create);
                    
                    updateDoneButton();
                    
                    Log.d(TAG_LOG, "Nouvelle catégorie créée et sélectionnée: " + category.getName());
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.etNewCategory.setError("Erreur: " + error);
                    binding.btnCreateCategory.setEnabled(true);
                    binding.btnCreateCategory.setText(R.string.create);
                    Log.e(TAG_LOG, "Erreur lors de la création de la catégorie: " + error);
                });
            }
        });
    }
    
    private void finishWithResult() {
        Intent resultIntent = new Intent();
        if (selectedCategory != null) {
            resultIntent.putExtra(EXTRA_RESULT_CATEGORY, selectedCategory.getId());
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
