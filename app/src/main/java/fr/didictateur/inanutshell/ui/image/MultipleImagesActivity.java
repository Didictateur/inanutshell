package fr.didictateur.inanutshell.ui.image;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityMultipleImagesBinding;
import fr.didictateur.inanutshell.utils.ImageUtils;
import fr.didictateur.inanutshell.utils.RecipeImageManager;

/**
 * Activité pour gérer les images multiples d'une recette
 */
public class MultipleImagesActivity extends AppCompatActivity implements 
    MultipleImagesAdapter.OnImageActionListener,
    ImageUtils.ImageSelectionListener {
    
    private ActivityMultipleImagesBinding binding;
    private MultipleImagesAdapter adapter;
    private RecipeImageManager imageManager;
    
    private String recipeId;
    private String recipeName;
    private List<String> imageUrls;
    
    public static final String EXTRA_RECIPE_ID = "recipe_id";
    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    
    // Codes de requête
    private static final int REQUEST_CODE_ADD_IMAGE_GALLERY = 3001;
    private static final int REQUEST_CODE_ADD_IMAGE_CAMERA = 3002;
    private static final int REQUEST_CODE_EDIT_IMAGE = 3003;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMultipleImagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Récupérer les données de l'intent
        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        recipeName = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        
        if (recipeId == null) {
            Toast.makeText(this, "Erreur: ID de recette manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupToolbar();
        setupViews();
        loadImages();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(recipeName != null ? recipeName : "Images de la recette");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupViews() {
        imageManager = RecipeImageManager.getInstance(this);
        imageUrls = new ArrayList<>();
        
        // Configuration du RecyclerView
        adapter = new MultipleImagesAdapter(this, imageUrls, this);
        binding.recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewImages.setAdapter(adapter);
        
        // FAB pour ajouter une image
        binding.fabAddImage.setOnClickListener(v -> showAddImageDialog());
        
        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadImages);
    }
    
    private void loadImages() {
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.progressBar.setVisibility(View.VISIBLE);
        
        imageManager.getRecipeImages(recipeId, new RecipeImageManager.ImageListCallback() {
            @Override
            public void onImagesLoaded(List<String> images) {
                runOnUiThread(() -> {
                    imageUrls.clear();
                    imageUrls.addAll(images);
                    adapter.notifyDataSetChanged();
                    
                    updateEmptyState();
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.progressBar.setVisibility(View.GONE);
                });
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MultipleImagesActivity.this, 
                        "Erreur lors du chargement: " + message, Toast.LENGTH_LONG).show();
                    updateEmptyState();
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.progressBar.setVisibility(View.GONE);
                });
            }
        });
    }
    
    private void updateEmptyState() {
        if (imageUrls.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewImages.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewImages.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddImageDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Ajouter une image")
            .setMessage("Choisissez la source de votre image")
            .setPositiveButton("Galerie", (dialog, which) -> selectImageFromGallery())
            .setNegativeButton("Appareil photo", (dialog, which) -> takePhotoWithCamera())
            .setNeutralButton("Annuler", null)
            .show();
    }
    
    private void selectImageFromGallery() {
        ImageUtils.openGallery(this, this);
    }
    
    private void takePhotoWithCamera() {
        ImageUtils.openCamera(this, this);
    }
    
    // ===== CALLBACKS ADAPTER =====
    
    @Override
    public void onImageClick(String imageUrl, int position) {
        // Ouvrir l'image en plein écran
        Intent intent = new Intent(this, FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(FullscreenImageActivity.EXTRA_RECIPE_NAME, recipeName);
        startActivity(intent);
    }
    
    @Override
    public void onImageEdit(String imageUrl, int position) {
        // TODO: Implémenter l'édition depuis URL
        Toast.makeText(this, "Édition depuis URL - En développement", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onImageDelete(String imageUrl, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer l'image")
            .setMessage("Êtes-vous sûr de vouloir supprimer cette image ?")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteImage(imageUrl, position))
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void deleteImage(String imageUrl, int position) {
        imageManager.removeImageFromRecipe(recipeId, imageUrl, new RecipeImageManager.ImageOperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    imageUrls.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, imageUrls.size());
                    updateEmptyState();
                    Toast.makeText(MultipleImagesActivity.this, "Image supprimée", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MultipleImagesActivity.this, 
                        "Erreur lors de la suppression: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    // ===== CALLBACKS SÉLECTION D'IMAGE =====
    
    @Override
    public void onImageSelected(Uri imageUri, String imagePath) {
        // Ajouter l'image à la recette
        imageManager.addImageToRecipe(recipeId, imageUri, new RecipeImageManager.ImageOperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MultipleImagesActivity.this, "Image ajoutée", Toast.LENGTH_SHORT).show();
                    loadImages(); // Recharger la liste
                });
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MultipleImagesActivity.this, 
                        "Erreur lors de l'ajout: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    @Override
    public void onImageSelectionError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
    
    // ===== MENU =====
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_multiple_images, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_add_image) {
            showAddImageDialog();
            return true;
        } else if (itemId == R.id.action_clear_cache) {
            clearImageCache();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void clearImageCache() {
        new AlertDialog.Builder(this)
            .setTitle("Vider le cache")
            .setMessage("Vider le cache des images ? Les images seront rechargées depuis le serveur.")
            .setPositiveButton("Vider", (dialog, which) -> {
                imageManager.clearAllCaches();
                loadImages();
                Toast.makeText(this, "Cache vidé", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Pas besoin de nettoyer le singleton ici
    }
}
