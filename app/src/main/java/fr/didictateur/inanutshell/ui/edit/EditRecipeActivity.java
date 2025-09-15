package fr.didictateur.inanutshell.ui.edit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;

import com.google.android.material.chip.Chip;
import com.google.android.material.button.MaterialButton;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.manager.CategoryTagManager;
import fr.didictateur.inanutshell.data.model.Category;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeIngredient;
import fr.didictateur.inanutshell.data.model.Tag;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.databinding.ActivityEditRecipeBinding;
import fr.didictateur.inanutshell.ui.tags.TagSelectionActivity;
import fr.didictateur.inanutshell.ui.categories.CategorySelectionActivity;
import fr.didictateur.inanutshell.ui.tools.ToolSelectionActivity;
import fr.didictateur.inanutshell.utils.ToolsManager;
import fr.didictateur.inanutshell.data.model.Tool;
import fr.didictateur.inanutshell.utils.ImageUtils;
import fr.didictateur.inanutshell.utils.PermissionManager;
import fr.didictateur.inanutshell.data.network.ImageUploadService;

import java.util.List;
import java.util.ArrayList;

public class EditRecipeActivity extends AppCompatActivity implements ImageUtils.ImageSelectionListener, ImageUploadService.ImageUploadListener {
    
    private ActivityEditRecipeBinding binding;
    private Recipe recipe;
    private boolean isEditing = false;
    
    // Gestion des catégories et tags
    private CategoryTagManager categoryTagManager;
    private List<Category> availableCategories = new ArrayList<>();
    private List<Tag> availableTags = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();
    private Category selectedCategory;
    
    // Gestion des outils
    private ToolsManager toolsManager;
    private List<Tool> availableTools = new ArrayList<>();
    private List<Tool> selectedTools = new ArrayList<>();
    
    // ActivityResultLaunchers pour la sélection des tags, catégories et outils
    private ActivityResultLauncher<Intent> tagSelectionLauncher;
    private ActivityResultLauncher<Intent> categorySelectionLauncher;
    private ActivityResultLauncher<Intent> toolSelectionLauncher;
    
    // Gestion des images
    private ImageView imagePreview;
    private MaterialButton btnSelectFromGallery;
    private MaterialButton btnTakePhoto;
    private MaterialButton btnRemoveImage;
    private Uri currentImageUri;
    private Uri tempCameraUri;
    private String uploadedImageUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_recipe);
        
        // Initialiser le gestionnaire de catégories et tags
        categoryTagManager = CategoryTagManager.getInstance();
        
        // Initialiser le gestionnaire d'outils
        toolsManager = ToolsManager.getInstance(this);
        
        setupToolbar();
        setupViews();
        setupCategoryTagViews();
        setupToolsViews();
        
        // Vérifier si on édite une recette existante
        String recipeId = getIntent().getStringExtra("recipe_id");
        if (recipeId != null) {
            isEditing = true;
            binding.toolbar.setTitle(R.string.edit_recipe);
            loadRecipeForEditing(recipeId);
        }
        
        // Charger les catégories, tags et outils disponibles
        loadCategoriesAndTags();
        loadTools();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupViews() {
        binding.btnSave.setOnClickListener(v -> saveRecipe());
        setupImageComponents();
    }
    
    private void setupImageComponents() {
        // Récupérer les références des composants d'image
        imagePreview = findViewById(R.id.iv_recipe_image);
        btnSelectFromGallery = findViewById(R.id.btn_select_from_gallery);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnRemoveImage = findViewById(R.id.btn_remove_image);
        
        // Configuration des boutons
        btnSelectFromGallery.setOnClickListener(v -> selectImageFromGallery());
        btnTakePhoto.setOnClickListener(v -> takePhotoWithCamera());
        btnRemoveImage.setOnClickListener(v -> removeCurrentImage());
        
        // Debug: Long click sur le bouton galerie pour réinitialiser les permissions
        btnSelectFromGallery.setOnLongClickListener(v -> {
            PermissionManager.debugPermissions(this);
            PermissionManager.resetPermissionState(this);
            Toast.makeText(this, "État des permissions réinitialisé + debug affiché", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // Masquer le bouton de suppression au début
        btnRemoveImage.setVisibility(View.GONE);
    }
    
    private void setupCategoryTagViews() {
        // Configuration du launcher pour la sélection des tags
        tagSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> tagIds = result.getData().getStringArrayListExtra(TagSelectionActivity.EXTRA_RESULT_TAGS);
                    if (tagIds != null) {
                        updateSelectedTags(tagIds);
                    }
                }
            }
        );
        
        // Configuration du launcher pour la sélection des catégories
        categorySelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String categoryId = result.getData().getStringExtra(CategorySelectionActivity.EXTRA_RESULT_CATEGORY);
                    updateSelectedCategory(categoryId);
                }
            }
        );
        
        // Configuration du bouton de sélection des tags
        binding.btnSelectTags.setOnClickListener(v -> openTagSelection());
        
        // Configuration du bouton de sélection des catégories
        binding.btnSelectCategory.setOnClickListener(v -> openCategorySelection());
    }
    
    private void setupToolsViews() {
        // Configuration du launcher pour la sélection des outils
        toolSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> toolIds = result.getData().getStringArrayListExtra(ToolSelectionActivity.EXTRA_RESULT_TOOLS);
                    if (toolIds != null) {
                        updateSelectedTools(toolIds);
                    }
                }
            }
        );
        
        // Configuration du bouton de sélection des outils
        binding.btnSelectTools.setOnClickListener(v -> openToolSelection());
    }
    
    private void saveRecipe() {
        if (!validateInputs()) {
            return;
        }
        
        // Désactiver le bouton pendant la sauvegarde
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Sauvegarde...");
        
        // Si nous avons une nouvelle image, l'uploader d'abord
        if (currentImageUri != null) {
            uploadImageAndSaveRecipe();
        } else {
            // Pas d'image, sauvegarder directement
            performRecipeSave(null);
        }
    }
    
    private void uploadImageAndSaveRecipe() {
        ImageUploadService.uploadRecipeImage(this, null, currentImageUri, this);
    }
    
    private void performRecipeSave(String imageUrl) {
        // Créer l'objet Recipe
        Recipe newRecipe = new Recipe();
        newRecipe.setName(binding.etTitle.getText().toString().trim());
        newRecipe.setDescription(binding.etDescription.getText().toString().trim());
        
        // Convertir les temps en minutes
        String prepTimeStr = binding.etPrepTime.getText().toString().trim();
        if (!TextUtils.isEmpty(prepTimeStr)) {
            newRecipe.setPrepTime(prepTimeStr + " min");
        }
        
        String cookTimeStr = binding.etCookTime.getText().toString().trim();
        if (!TextUtils.isEmpty(cookTimeStr)) {
            newRecipe.setCookTime(cookTimeStr + " min");
        }
        
        // Nombre de portions
        String servingsStr = binding.etServings.getText().toString().trim();
        if (!TextUtils.isEmpty(servingsStr)) {
            newRecipe.setRecipeYield(servingsStr + " portions");
        }
        
        // Ingrédients - convertir le texte en liste
        String ingredientsText = binding.etIngredients.getText().toString().trim();
        if (!TextUtils.isEmpty(ingredientsText)) {
            java.util.List<RecipeIngredient> ingredientsList = new java.util.ArrayList<>();
            String[] lines = ingredientsText.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    RecipeIngredient ingredient = new RecipeIngredient(line.trim());
                    ingredientsList.add(ingredient);
                }
            }
            newRecipe.setRecipeIngredient(ingredientsList);
        }
        
        // Instructions - convertir le texte en liste d'instructions
        String instructionsText = binding.etInstructions.getText().toString().trim();
        if (!TextUtils.isEmpty(instructionsText)) {
            java.util.List<fr.didictateur.inanutshell.data.model.RecipeInstruction> instructionsList = new java.util.ArrayList<>();
            String[] lines = instructionsText.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    fr.didictateur.inanutshell.data.model.RecipeInstruction instruction = new fr.didictateur.inanutshell.data.model.RecipeInstruction();
                    instruction.setText(line);
                    instructionsList.add(instruction);
                }
            }
            newRecipe.setRecipeInstructions(instructionsList);
        }
        
        // Appliquer les catégories et tags
        applyCategoryTagsAndToolsToRecipe(newRecipe);
        
        // Ajouter l'URL de l'image si disponible
        if (imageUrl != null && !imageUrl.isEmpty()) {
            newRecipe.setImage(imageUrl);
        } else if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            newRecipe.setImage(uploadedImageUrl);
        }
        
        // Sauvegarder via NetworkManager
        
        if (isEditing && recipe != null) {
            // Mettre à jour la recette existante
            newRecipe.setId(recipe.getId());
            NetworkManager.getInstance().updateRecipe(recipe.getId(), newRecipe, new NetworkManager.UpdateRecipeCallback() {
                @Override
                public void onSuccess(Recipe updatedRecipe) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditRecipeActivity.this, 
                            "Recette modifiée avec succès", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setText(R.string.save_recipe);
                        Toast.makeText(EditRecipeActivity.this, 
                            "Erreur lors de la modification: " + error, 
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // Créer une nouvelle recette
            NetworkManager.getInstance().createRecipe(newRecipe, new NetworkManager.CreateRecipeCallback() {
                @Override
                public void onSuccess(Recipe createdRecipe) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditRecipeActivity.this, 
                            R.string.recipe_saved, 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setText(R.string.save_recipe);
                        Toast.makeText(EditRecipeActivity.this, 
                            getString(R.string.error_saving_recipe, error), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }
    
    private boolean validateInputs() {
        boolean valid = true;
        
        // Titre obligatoire
        if (TextUtils.isEmpty(binding.etTitle.getText().toString().trim())) {
            binding.etTitle.setError(getString(R.string.fields_required));
            valid = false;
        }
        
        // Instructions obligatoires
        if (TextUtils.isEmpty(binding.etInstructions.getText().toString().trim())) {
            binding.etInstructions.setError(getString(R.string.fields_required));
            valid = false;
        }
        
        return valid;
    }
    
    // === MÉTHODES POUR CATÉGORIES ET TAGS ===
    
    private void loadCategoriesAndTags() {
        // Charger les catégories
        categoryTagManager.getCategories(new CategoryTagManager.CategoriesCallback() {
            @Override
            public void onSuccess(List<Category> categories) {
                runOnUiThread(() -> {
                    availableCategories = categories;
                    Log.d("EditRecipeActivity", "Catégories chargées: " + categories.size());
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("EditRecipeActivity", "Erreur chargement catégories: " + error);
            }
        });
        
        // Charger les tags
        categoryTagManager.getTags(new CategoryTagManager.TagsCallback() {
            @Override
            public void onSuccess(List<Tag> tags) {
                runOnUiThread(() -> {
                    availableTags = tags;
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("EditRecipeActivity", "Erreur chargement tags: " + error);
            }
        });
    }
    
    private void loadTools() {
        // Charger les outils disponibles
        toolsManager.getTools(new ToolsManager.ToolsCallback() {
            @Override
            public void onSuccess(List<Tool> tools) {
                runOnUiThread(() -> {
                    availableTools = tools;
                    Log.d("EditRecipeActivity", "Outils chargés: " + tools.size());
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("EditRecipeActivity", "Erreur chargement outils: " + error);
            }
        });
    }
    
    private void openTagSelection() {
        Intent intent = new Intent(this, TagSelectionActivity.class);
        ArrayList<String> selectedTagIds = new ArrayList<>();
        for (Tag tag : selectedTags) {
            selectedTagIds.add(tag.getId());
        }
        intent.putStringArrayListExtra(TagSelectionActivity.EXTRA_SELECTED_TAGS, selectedTagIds);
        tagSelectionLauncher.launch(intent);
    }
    
    private void openCategorySelection() {
        Intent intent = new Intent(this, CategorySelectionActivity.class);
        if (selectedCategory != null) {
            intent.putExtra(CategorySelectionActivity.EXTRA_SELECTED_CATEGORY, selectedCategory.getId());
        }
        categorySelectionLauncher.launch(intent);
    }
    
    private void openToolSelection() {
        Intent intent = new Intent(this, ToolSelectionActivity.class);
        ArrayList<String> selectedToolIds = new ArrayList<>();
        for (Tool tool : selectedTools) {
            selectedToolIds.add(tool.getId());
        }
        intent.putStringArrayListExtra(ToolSelectionActivity.EXTRA_SELECTED_TOOLS, selectedToolIds);
        toolSelectionLauncher.launch(intent);
    }
    
    private void updateSelectedTools(ArrayList<String> toolIds) {
        selectedTools.clear();
        for (String toolId : toolIds) {
            // Pour l'instant, nous devrons chercher dans la liste des outils disponibles
            for (Tool tool : availableTools) {
                if (tool.getId().equals(toolId)) {
                    selectedTools.add(tool);
                    break;
                }
            }
        }
        updateToolChips();
    }
    
    private void updateToolChips() {
        binding.chipGroupTools.removeAllViews();
        
        for (Tool tool : selectedTools) {
            Chip chip = new Chip(this);
            chip.setText(tool.getName());
            chip.setCloseIconVisible(true);
            chip.setChipIcon(getDrawable(R.drawable.ic_tool));
            chip.setOnCloseIconClickListener(v -> {
                selectedTools.remove(tool);
                updateToolChips();
            });
            binding.chipGroupTools.addView(chip);
        }
    }
    
    private void updateSelectedTags(ArrayList<String> tagIds) {
        selectedTags.clear();
        for (String tagId : tagIds) {
            Tag tag = categoryTagManager.getTagById(tagId);
            if (tag != null) {
                selectedTags.add(tag);
            }
        }
        updateTagChips();
    }
    
    private void updateSelectedCategory(String categoryId) {
        if (categoryId != null && !categoryId.isEmpty()) {
            selectedCategory = categoryTagManager.getCategoryById(categoryId);
            if (selectedCategory != null) {
                binding.tvSelectedCategory.setText(selectedCategory.getName());
                binding.tvSelectedCategory.setVisibility(android.view.View.VISIBLE);
                Log.d("EditRecipeActivity", "Catégorie mise à jour: " + selectedCategory.getName());
            }
        } else {
            selectedCategory = null;
            binding.tvSelectedCategory.setText("Aucune catégorie");
            binding.tvSelectedCategory.setVisibility(android.view.View.VISIBLE);
            Log.d("EditRecipeActivity", "Catégorie effacée");
        }
    }
    
    private void updateTagChips() {
        binding.chipGroupTags.removeAllViews();
        
        for (Tag tag : selectedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedTags.remove(tag);
                updateTagChips();
            });
            binding.chipGroupTags.addView(chip);
        }
    }
    
    private void populateCategoryAndTags(Recipe recipe) {
        // Catégories
        if (recipe.getCategories() != null && !recipe.getCategories().isEmpty()) {
            Category recipeCategory = recipe.getCategories().get(0);
            selectedCategory = recipeCategory;
            updateSelectedCategory(selectedCategory.getId());
            Log.d("EditRecipeActivity", "Catégorie chargée: " + recipeCategory.getName());
        } else {
            updateSelectedCategory(null);
            Log.d("EditRecipeActivity", "Aucune catégorie à charger");
        }
        
        // Tags
        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            selectedTags = new ArrayList<>(recipe.getTags());
            updateTagChips();
            Log.d("EditRecipeActivity", "Tags chargés: " + recipe.getTags().size() + " tags");
        } else {
            selectedTags.clear();
            updateTagChips();
            Log.d("EditRecipeActivity", "Aucun tag à charger");
        }
        
        // Outils
        if (recipe.getTools() != null && !recipe.getTools().isEmpty()) {
            selectedTools = new ArrayList<>(recipe.getTools());
            updateToolChips();
            Log.d("EditRecipeActivity", "Outils chargés: " + recipe.getTools().size() + " outils");
        } else {
            selectedTools.clear();
            updateToolChips();
            Log.d("EditRecipeActivity", "Aucun outil à charger");
        }
    }
    
    private void applyCategoryTagsAndToolsToRecipe(Recipe recipe) {
        // Appliquer la catégorie sélectionnée
        if (selectedCategory != null) {
            List<Category> categories = new ArrayList<>();
            categories.add(selectedCategory);
            recipe.setCategories(categories);
        }
        
        // Appliquer les tags sélectionnés
        if (!selectedTags.isEmpty()) {
            recipe.setTags(new ArrayList<>(selectedTags));
        }
        
        // Appliquer les outils sélectionnés
        if (!selectedTools.isEmpty()) {
            recipe.setTools(new ArrayList<>(selectedTools));
        }
    }
    
    private void loadRecipeForEditing(String recipeId) {
        // Afficher un indicateur de chargement
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Chargement...");
        
        // Récupérer la recette complète via l'API
        NetworkManager.getInstance().getRecipe(recipeId, new NetworkManager.RecipeCallback() {
            @Override
            public void onSuccess(Recipe loadedRecipe) {
                runOnUiThread(() -> {
                    recipe = loadedRecipe;
                    populateFormWithRecipe(recipe);
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_recipe);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Fallback: utiliser les données passées via intent
                    String recipeName = getIntent().getStringExtra("recipe_name");
                    String recipeDescription = getIntent().getStringExtra("recipe_description");
                    
                    if (recipeName != null) {
                        binding.etTitle.setText(recipeName);
                    }
                    
                    if (recipeDescription != null) {
                        binding.etDescription.setText(recipeDescription);
                    }
                    
                    // Créer un objet Recipe temporaire
                    recipe = new Recipe();
                    recipe.setId(recipeId);
                    recipe.setName(recipeName);
                    recipe.setDescription(recipeDescription);
                    
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_recipe);
                    
                    Toast.makeText(EditRecipeActivity.this, 
                        "Attention: Impossible de charger toutes les données de la recette. Erreur: " + error, 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void populateFormWithRecipe(Recipe recipe) {
        Log.d("EditRecipeActivity", "Populating form with recipe: " + recipe.getName());
        
        // Remplir le titre
        if (recipe.getName() != null) {
            binding.etTitle.setText(recipe.getName());
            Log.d("EditRecipeActivity", "Set title: " + recipe.getName());
        }
        
        // Remplir la description
        if (recipe.getDescription() != null) {
            binding.etDescription.setText(recipe.getDescription());
            Log.d("EditRecipeActivity", "Set description: " + recipe.getDescription());
        }
        
        // Remplir les temps (extraire les nombres des temps ISO 8601 si nécessaire)
        if (recipe.getPrepTime() != null) {
            String prepTime = extractMinutesFromDuration(recipe.getPrepTime());
            binding.etPrepTime.setText(prepTime);
            Log.d("EditRecipeActivity", "PrepTime raw: " + recipe.getPrepTime() + " -> extracted: " + prepTime);
        } else if (recipe.getPerformTime() != null) {
            // Utiliser performTime comme fallback pour prepTime
            String prepTime = extractMinutesFromDuration(recipe.getPerformTime());
            binding.etPrepTime.setText(prepTime);
            Log.d("EditRecipeActivity", "Using PerformTime as PrepTime - raw: " + recipe.getPerformTime() + " -> extracted: " + prepTime);
        } else {
            Log.d("EditRecipeActivity", "PrepTime and PerformTime are null");
        }
        
        if (recipe.getCookTime() != null) {
            String cookTime = extractMinutesFromDuration(recipe.getCookTime());
            binding.etCookTime.setText(cookTime);
            Log.d("EditRecipeActivity", "CookTime raw: " + recipe.getCookTime() + " -> extracted: " + cookTime);
        } else {
            Log.d("EditRecipeActivity", "CookTime is null");
        }
        
        if (recipe.getTotalTime() != null) {
            Log.d("EditRecipeActivity", "TotalTime: " + recipe.getTotalTime());
            Log.d("EditRecipeActivity", "TotalTime type: " + recipe.getTotalTime().getClass().getSimpleName());
            
            // Si les autres temps sont vides, essayons d'utiliser totalTime
            if ((recipe.getPrepTime() == null || recipe.getPrepTime().isEmpty()) && 
                (recipe.getCookTime() == null || recipe.getCookTime().isEmpty())) {
                
                Log.d("EditRecipeActivity", "Other time fields are empty, using totalTime");
                String totalTimeValue = extractMinutesFromDuration(recipe.getTotalTime());
                Log.d("EditRecipeActivity", "Extracted totalTime value: '" + totalTimeValue + "'");
                
                // Utiliser totalTime comme temps de préparation si les autres sont vides
                if (!totalTimeValue.isEmpty()) {
                    Log.d("EditRecipeActivity", "Setting prepTime field to: " + totalTimeValue);
                    binding.etPrepTime.setText(totalTimeValue);
                } else {
                    Log.d("EditRecipeActivity", "TotalTime extraction resulted in empty string");
                }
            } else {
                Log.d("EditRecipeActivity", "Other time fields are not empty, not using totalTime");
            }
        } else {
            Log.d("EditRecipeActivity", "TotalTime is null");
        }
        
        // Remplir les portions
        if (recipe.getRecipeYield() != null) {
            binding.etServings.setText(recipe.getRecipeYield());
        }
        
        // Remplir les ingrédients
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            StringBuilder ingredients = new StringBuilder();
            for (RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                if (ingredients.length() > 0) {
                    ingredients.append("\n");
                }
                ingredients.append(ingredient.toString());
            }
            binding.etIngredients.setText(ingredients.toString());
        }
        
        // Remplir les instructions
        if (recipe.getRecipeInstructions() != null && !recipe.getRecipeInstructions().isEmpty()) {
            StringBuilder instructions = new StringBuilder();
            for (int i = 0; i < recipe.getRecipeInstructions().size(); i++) {
                if (instructions.length() > 0) {
                    instructions.append("\n");
                }
                String instructionText = recipe.getRecipeInstructions().get(i).getText();
                if (instructionText != null) {
                    instructions.append(instructionText);
                }
            }
            binding.etInstructions.setText(instructions.toString());
        }
        
        // Remplir les catégories et tags
        populateCategoryAndTags(recipe);
    }
    
    private String extractMinutesFromDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return "";
        }
        
        Log.d("EditRecipeActivity", "Extracting duration from: " + duration);
        
        // Si c'est déjà un nombre, c'est probablement en minutes
        if (duration.matches("\\d+")) {
            int value = Integer.parseInt(duration);
            Log.d("EditRecipeActivity", "Duration is a number: " + value);
            Log.d("EditRecipeActivity", "Value is already in minutes: " + value);
            return duration;
        }
        
        // Extraire les minutes d'un format ISO 8601 (PT30M, PT1H30M, etc.)
        try {
            if (duration.startsWith("PT")) {
                int totalMinutes = 0;
                
                // Extraire les heures si présentes
                if (duration.contains("H")) {
                    int hIndex = duration.indexOf("H");
                    String hoursStr = duration.substring(2, hIndex);
                    totalMinutes += Integer.parseInt(hoursStr) * 60;
                    duration = "PT" + duration.substring(hIndex + 1); // Enlever la partie heures
                }
                
                // Extraire les minutes si présentes
                if (duration.contains("M")) {
                    int mIndex = duration.indexOf("M");
                    String minutesStr = duration.substring(2, mIndex);
                    if (!minutesStr.isEmpty()) {
                        totalMinutes += Integer.parseInt(minutesStr);
                    }
                }
                
                Log.d("EditRecipeActivity", "Extracted total minutes: " + totalMinutes);
                return String.valueOf(totalMinutes);
            }
            
            // Gérer d'autres formats comme "30 minutes", "1 hour 30 minutes"
            if (duration.toLowerCase().contains("minute")) {
                String[] parts = duration.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i + 1].toLowerCase().contains("minute")) {
                        return parts[i];
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e("EditRecipeActivity", "Error extracting duration: " + e.getMessage());
        }
        
        Log.d("EditRecipeActivity", "Returning original duration: " + duration);
        return duration;
    }
    
    // ===== GESTION DES IMAGES =====
    
    /**
     * Sélectionner une image depuis la galerie
     */
    private void selectImageFromGallery() {
        ImageUtils.openGallery(this, this);
    }
    
    /**
     * Prendre une photo avec l'appareil photo
     */
    private void takePhotoWithCamera() {
        ImageUtils.openCamera(this, this);
    }
    
    /**
     * Supprimer l'image actuelle
     */
    private void removeCurrentImage() {
        currentImageUri = null;
        imagePreview.setImageResource(R.drawable.placeholder_recipe);
        btnRemoveImage.setVisibility(View.GONE);
        Toast.makeText(this, R.string.image_removed, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ImageUtils.REQUEST_CODE_GALLERY:
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();
                        onImageSelected(selectedImageUri, ImageUtils.getRealPathFromUri(this, selectedImageUri));
                    }
                    break;
                    
                case ImageUtils.REQUEST_CODE_CAMERA:
                    if (tempCameraUri != null) {
                        onImageSelected(tempCameraUri, ImageUtils.getRealPathFromUri(this, tempCameraUri));
                    }
                    break;
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        PermissionManager.handlePermissionResult(this, requestCode, permissions, grantResults, new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                Toast.makeText(EditRecipeActivity.this, "Permissions accordées ! Vous pouvez maintenant ajouter des images.", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onPermissionsDenied() {
                Toast.makeText(EditRecipeActivity.this, "Permissions requises pour gérer les images. Réessayez ou accordez les permissions dans les paramètres.", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onPermissionsPermanentlyDenied() {
                // Ce message ne devrait plus apparaître avec la nouvelle logique
                Toast.makeText(EditRecipeActivity.this, "Veuillez activer les permissions dans les paramètres de l'app.", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public void onImageSelected(Uri imageUri, String imagePath) {
        currentImageUri = imageUri;
        
        // Redimensionner et afficher l'image
        Bitmap resizedBitmap = ImageUtils.resizeImageForUpload(this, imageUri, 800, 600);
        if (resizedBitmap != null) {
            imagePreview.setImageBitmap(resizedBitmap);
            btnRemoveImage.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.image_selected, Toast.LENGTH_SHORT).show();
        } else {
            onImageSelectionError(getString(R.string.error_loading_image));
        }
    }
    
    @Override
    public void onImageSelectionError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        Log.e("EditRecipeActivity", "Image selection error: " + error);
    }
    
    // ===== CALLBACKS UPLOAD IMAGE =====
    
    @Override
    public void onUploadSuccess(String imageUrl) {
        runOnUiThread(() -> {
            Log.d("EditRecipeActivity", "Image uploaded successfully: " + imageUrl);
            uploadedImageUrl = imageUrl;
            // Continuer avec la sauvegarde de la recette
            performRecipeSave(imageUrl);
        });
    }
    
    @Override
    public void onUploadError(String error) {
        runOnUiThread(() -> {
            Log.e("EditRecipeActivity", "Image upload error: " + error);
            // Afficher un dialog pour demander si on veut continuer sans image
            showUploadErrorDialog(error);
        });
    }
    
    @Override
    public void onUploadProgress(int progress) {
        runOnUiThread(() -> {
            binding.btnSave.setText("Upload image... " + progress + "%");
        });
    }
    
    private void showUploadErrorDialog(String error) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Erreur upload image")
                .setMessage("Erreur lors de l'upload de l'image: " + error + 
                          "\n\nVoulez-vous sauvegarder la recette sans image ?")
                .setPositiveButton("Sauvegarder sans image", (dialog, which) -> {
                    performRecipeSave(null);
                })
                .setNegativeButton("Annuler", (dialog, which) -> {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText(R.string.save_recipe);
                })
                .show();
    }
}
