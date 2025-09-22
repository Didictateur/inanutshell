package fr.didictateur.inanutshell.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.adapters.RecipeImportExportAdapter;
import fr.didictateur.inanutshell.data.importing.RecipeImporter;
import fr.didictateur.inanutshell.data.export.RecipeExporter;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.adapters.RecipeImportExportAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activité dédiée à l'import/export de recettes
 * - Import depuis URLs de sites web
 * - Import depuis fichiers
 * - Export en différents formats
 * - Partage vers autres applications
 * - Sauvegarde/restauration complète
 */
public class ImportExportActivity extends AppCompatActivity {

    private static final String TAG = "ImportExportActivity";
    private static final int REQUEST_PICK_FILE = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    
    // Vues principales
    private RecyclerView recyclerViewImportExport;
    private RecipeImportExportAdapter adapter;
    private FloatingActionButton fabActions;
    private ProgressBar progressBar;
    private TextView textViewStatus;
    
    // Nouveaux composants UI avancés
    private CheckBox checkboxSelectAll;
    private TextView textViewSelectedCount;
    private MaterialButton buttonBatchActions;
    private LinearLayout layoutProgress;
    private TextView textViewProgressTitle;
    private TextView textViewProgressPercent;
    private TextView textViewProgressDetail;
    private TabLayout tabLayoutMode;
    private LinearLayout layoutPreviewPanel;
    private LinearLayout layoutPreviewContent;
    private MaterialButton buttonClosePreview;
    private ImageView imageViewStatusIcon;
    private TextView textViewLastUpdate;
    
    // Gestionnaires
    private RecipeImporter recipeImporter;
    private RecipeExporter recipeExporter;
    private NetworkManager networkManager;
    
    // Données
    private List<Recipe> selectedRecipes;
    private List<String> importUrls;
    private Set<Integer> selectedIndices;
    private int currentMode = 0; // 0=Import, 1=Export, 2=Backup
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export);
        
        initializeViews();
        initializeManagers();
        setupRecyclerView();
        setupFloatingActionButton();
        checkStoragePermissions();
    }
    
    private void initializeViews() {
        recyclerViewImportExport = findViewById(R.id.recyclerViewImportExport);
        fabActions = findViewById(R.id.fabActions);
        progressBar = findViewById(R.id.progressBar);
        textViewStatus = findViewById(R.id.textViewStatus);
        
        // Interface basique - les composants avancés seront ajoutés progressivement
        
        setupAdvancedUI();
        
        // Configuration de la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Import / Export");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initializeManagers() {
        recipeImporter = RecipeImporter.getInstance(this);
        recipeExporter = RecipeExporter.getInstance(this);
        networkManager = NetworkManager.getInstance(this);
        
        selectedRecipes = new ArrayList<>();
        importUrls = new ArrayList<>();
        selectedIndices = new HashSet<>();
    }
    
    private void setupRecyclerView() {
        adapter = new RecipeImportExportAdapter(this, RecipeImportExportAdapter.Mode.EXPORT);
        recyclerViewImportExport.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewImportExport.setAdapter(adapter);
        
        // Callback pour les actions sur les recettes
        // adapter.setOnRecipeActionListener(new RecipeImportExportAdapter.OnRecipeActionListener() {
        //     @Override
        //     public void onExportRecipe(Recipe recipe, RecipeExporter.ExportFormat format) {
        //         exportSingleRecipe(recipe, format);
        //     }
        //     
        //     @Override
        //     public void onShareRecipe(Recipe recipe) {
        //         shareRecipe(recipe);
        //     }
        //     
        //     @Override
        //     public void onRemoveRecipe(Recipe recipe) {
        //         selectedRecipes.remove(recipe);
        //         adapter.notifyDataSetChanged();
        //         updateStatus();
        //     }
        // });
    }
    
    private void setupFloatingActionButton() {
        fabActions.setOnClickListener(v -> showActionsDialog());
    }
    
    // ===== CONFIGURATION UI AVANCÉE =====
    
    private void setupAdvancedUI() {
        // Interface simplifiée - fonctions de base seulement
        updateLastUpdate();
    }
    
    // ===== GESTION SIMPLIFIÉE =====
    
    private void clearSelection() {
        selectedIndices.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    private void showActionsDialog() {
        String[] options = {
            "Importer depuis URL",
            "Importer depuis fichier", 
            "Exporter les recettes sélectionnées",
            "Charger toutes les recettes",
            "Sauvegarde complète",
            "Partager toutes les recettes"
        };
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Actions Import/Export")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showImportFromUrlDialog();
                        break;
                    case 1:
                        pickFileToImport();
                        break;
                    case 2:
                        showExportSelectedDialog();
                        break;
                    case 3:
                        loadAllRecipes();
                        break;
                    case 4:
                        performFullBackup();
                        break;
                    case 5:
                        shareAllRecipes();
                        break;
                }
            })
            .show();
    }
    
    // ===== IMPORT DEPUIS URL =====
    
    private void showImportFromUrlDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_url, null);
        EditText editTextUrl = dialogView.findViewById(R.id.editTextUrl);
        EditText editTextUrls = dialogView.findViewById(R.id.editTextUrls);
        RadioGroup radioGroupMode = dialogView.findViewById(R.id.radioGroupMode);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Importer depuis URL(s)")
            .setView(dialogView)
            .setPositiveButton("Importer", (dialog, which) -> {
                int selectedMode = radioGroupMode.getCheckedRadioButtonId();
                
                if (selectedMode == R.id.radioSingleUrl) {
                    String url = editTextUrl.getText().toString().trim();
                    if (!TextUtils.isEmpty(url)) {
                        importFromSingleUrl(url);
                    } else {
                        showSnackbar("Veuillez saisir une URL");
                    }
                } else {
                    String urlsText = editTextUrls.getText().toString().trim();
                    if (!TextUtils.isEmpty(urlsText)) {
                        List<String> urls = Arrays.asList(urlsText.split("\\n"));
                        importFromMultipleUrls(urls);
                    } else {
                        showSnackbar("Veuillez saisir au moins une URL");
                    }
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void importFromSingleUrl(String url) {
        showProgress("Import en cours...");
        
        recipeImporter.importFromUrl(url, new RecipeImporter.ImportCallback() {
            @Override
            public void onSuccess(Recipe recipe) {
                runOnUiThread(() -> {
                    hideProgress();
                    selectedRecipes.add(recipe);
                    adapter.notifyDataSetChanged();
                    updateStatus();
                    showSnackbar("Recette importée: " + recipe.getName());
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Erreur import: " + error);
                });
            }
        });
    }
    
    private void importFromMultipleUrls(List<String> urls) {
        showProgress("Import de " + urls.size() + " URLs...");
        
        recipeImporter.importFromUrls(urls, new RecipeImporter.MultipleImportCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                runOnUiThread(() -> {
                    hideProgress();
                    selectedRecipes.addAll(recipes);
                    adapter.notifyDataSetChanged();
                    updateStatus();
                    showSnackbar(recipes.size() + " recettes importées");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Erreur import multiple: " + error);
                });
            }
            
            @Override
            public void onProgress(int processed, int total) {
                runOnUiThread(() -> {
                    textViewStatus.setText("Import: " + processed + "/" + total + " URLs");
                });
            }
        });
    }
    
    // ===== IMPORT DEPUIS FICHIER =====
    
    private void pickFileToImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Sélectionner un fichier"), REQUEST_PICK_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            showSnackbar("Aucune application de gestion de fichiers trouvée");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                importFromFile(data.getData());
            }
        }
    }
    
    private void importFromFile(Uri fileUri) {
        showProgress("Import du fichier...");
        
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            
            // Auto-détection du format
            recipeImporter.importFromFile(inputStream, RecipeImporter.ImportFormat.AUTO_DETECT, 
                new RecipeImporter.ImportCallback() {
                    @Override
                    public void onSuccess(Recipe recipe) {
                        runOnUiThread(() -> {
                            hideProgress();
                            selectedRecipes.add(recipe);
                            adapter.notifyDataSetChanged();
                            updateStatus();
                            showSnackbar("Fichier importé: " + recipe.getName());
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            hideProgress();
                            showSnackbar("Erreur import fichier: " + error);
                        });
                    }
                });
                
        } catch (Exception e) {
            hideProgress();
            showSnackbar("Erreur ouverture fichier: " + e.getMessage());
            Log.e(TAG, "Erreur import fichier", e);
        }
    }
    
    // ===== EXPORT =====
    
    private void exportSingleRecipe(Recipe recipe, RecipeExporter.ExportFormat format) {
        showProgress("Export en cours...");
        
        recipeExporter.exportRecipe(recipe, format, new RecipeExporter.ExportCallback() {
            @Override
            public void onSuccess(File exportedFile) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Export réussi: " + exportedFile.getName());
                    offerToShareFile(exportedFile);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Erreur export: " + error);
                });
            }
            
            @Override
            public void onProgress(int processed, int total) {
                // Pour export simple, pas besoin de progress
            }
        });
    }
    
    private void showExportSelectedDialog() {
        if (selectedRecipes.isEmpty()) {
            showSnackbar("Aucune recette sélectionnée");
            return;
        }
        
        String[] formats = {"JSON", "PDF", "Texte", "HTML", "CSV", "Format Mealie"};
        RecipeExporter.ExportFormat[] exportFormats = {
            RecipeExporter.ExportFormat.JSON,
            RecipeExporter.ExportFormat.PDF,
            RecipeExporter.ExportFormat.TEXT,
            RecipeExporter.ExportFormat.HTML,
            RecipeExporter.ExportFormat.CSV,
            RecipeExporter.ExportFormat.MEALIE_JSON
        };
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Format d'export")
            .setItems(formats, (dialog, which) -> {
                exportSelectedRecipes(exportFormats[which]);
            })
            .show();
    }
    
    private void exportSelectedRecipes(RecipeExporter.ExportFormat format) {
        showProgress("Export de " + selectedRecipes.size() + " recettes...");
        
        recipeExporter.exportRecipes(selectedRecipes, format, new RecipeExporter.ExportCallback() {
            @Override
            public void onSuccess(File exportedFile) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Export réussi: " + exportedFile.getName());
                    offerToShareFile(exportedFile);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Erreur export: " + error);
                });
            }
            
            @Override
            public void onProgress(int processed, int total) {
                runOnUiThread(() -> {
                    textViewStatus.setText("Export: " + processed + "/" + total + " recettes");
                });
            }
        });
    }
    
    // ===== PARTAGE =====
    
    private void shareRecipe(Recipe recipe) {
        // Export temporaire en texte pour partage
        recipeExporter.exportRecipe(recipe, RecipeExporter.ExportFormat.TEXT, 
            new RecipeExporter.ExportCallback() {
                @Override
                public void onSuccess(File exportedFile) {
                    shareFile(exportedFile, "Partager la recette");
                }
                
                @Override
                public void onError(String error) {
                    // Fallback: partage texte simple
                    shareRecipeAsText(recipe);
                }
                
                @Override
                public void onProgress(int processed, int total) {}
            });
    }
    
    private void shareRecipeAsText(Recipe recipe) {
        StringBuilder text = new StringBuilder();
        text.append("🍽️ ").append(recipe.getName()).append("\n\n");
        
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            text.append(recipe.getDescription()).append("\n\n");
        }
        
        if (recipe.getRecipeIngredient() != null && !recipe.getRecipeIngredient().isEmpty()) {
            text.append("📝 Ingrédients:\n");
            for (int i = 0; i < recipe.getRecipeIngredient().size(); i++) {
                text.append("• ").append(recipe.getRecipeIngredient().get(i).getFood()).append("\n");
            }
            text.append("\n");
        }
        
        text.append("Partagé depuis InANutshell 🥜");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recette: " + recipe.getName());
        
        startActivity(Intent.createChooser(shareIntent, "Partager la recette"));
    }
    
    private void shareAllRecipes() {
        if (selectedRecipes.isEmpty()) {
            showSnackbar("Aucune recette à partager");
            return;
        }
        
        // Export en format le plus universel (PDF)
        exportSelectedRecipes(RecipeExporter.ExportFormat.PDF);
    }
    
    private void offerToShareFile(File file) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Export terminé")
            .setMessage("Fichier exporté: " + file.getName() + "\n\nVoulez-vous le partager?")
            .setPositiveButton("Partager", (dialog, which) -> shareFile(file, "Partager l'export"))
            .setNegativeButton("Garder local", null)
            .show();
    }
    
    private void shareFile(File file, String title) {
        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
            this, getPackageName() + ".fileprovider", file);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, title));
    }
    
    // ===== CHARGEMENT RECETTES =====
    
    private void loadAllRecipes() {
        showProgress("Chargement des recettes...");
        
        networkManager.getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                runOnUiThread(() -> {
                    hideProgress();
                    selectedRecipes.clear();
                    selectedRecipes.addAll(recipes);
                    adapter.notifyDataSetChanged();
                    updateStatus();
                    showSnackbar(recipes.size() + " recettes chargées");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showSnackbar("Erreur chargement: " + error);
                });
            }
        });
    }
    
    // ===== SAUVEGARDE COMPLÈTE =====
    
    private void performFullBackup() {
        showProgress("Sauvegarde en cours...");
        
        // Pour l'instant, export toutes les recettes en JSON
        loadAllRecipes(); // Puis export automatique
        
        // TODO: Ajouter sauvegarde des meal plans, listes de courses, etc.
        showSnackbar("Fonctionnalité de sauvegarde complète en développement");
    }
    
    // ===== PERMISSIONS =====
    
    private void checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                REQUEST_STORAGE_PERMISSION);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("Permission accordée");
            } else {
                showSnackbar("Permission de stockage nécessaire pour l'export");
            }
        }
    }
    
    // ===== MÉTHODES UTILITAIRES =====
    
    private void updateLastUpdate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        // Mise à jour basique sans composant UI avancé
        Log.d("ImportExport", "Dernière mise à jour: " + sdf.format(new Date()));
    }
    
    // ===== UTILITAIRES UI =====
    
    private void showProgress(String message) {
        progressBar.setVisibility(View.VISIBLE);
        textViewStatus.setText(message);
        fabActions.setEnabled(false);
    }
    
    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        fabActions.setEnabled(true);
        updateStatus();
    }
    
    private void updateStatus() {
        if (selectedRecipes.isEmpty()) {
            textViewStatus.setText("Aucune recette sélectionnée");
        } else {
            textViewStatus.setText(selectedRecipes.size() + " recette(s) prête(s) pour export");
        }
        updateLastUpdate();
    }
    
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
