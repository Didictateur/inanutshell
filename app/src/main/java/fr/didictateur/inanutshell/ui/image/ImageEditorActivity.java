package fr.didictateur.inanutshell.ui.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityImageEditorBinding;
import fr.didictateur.inanutshell.utils.ImageUtils;

/**
 * Activité pour éditer les images : rotation et ajustements basiques
 */
public class ImageEditorActivity extends AppCompatActivity {
    
    private ActivityImageEditorBinding binding;
    private Uri originalImageUri;
    private Bitmap currentBitmap;
    private ImageView imageView;
    
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_EDITED_IMAGE_URI = "edited_image_uri";
    
    // État de rotation actuelle
    private int currentRotation = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupViews();
        loadImage();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Éditer l'image");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupViews() {
        // Référence à l'ImageView
        imageView = binding.imageView;
        
        // Boutons d'édition
        binding.btnRotateLeft.setOnClickListener(v -> rotateImage(-90));
        binding.btnRotateRight.setOnClickListener(v -> rotateImage(90));
        binding.btnFlipHorizontal.setOnClickListener(v -> flipImage(true));
        binding.btnFlipVertical.setOnClickListener(v -> flipImage(false));
        
        // Boutons de filtres
        binding.btnFilterBrightness.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.BRIGHTNESS_UP));
        binding.btnFilterContrast.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.CONTRAST_UP));
        binding.btnFilterSepia.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.SEPIA));
        binding.btnFilterGrayscale.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.GRAYSCALE));
        
        // Boutons d'actions
        binding.btnReset.setOnClickListener(v -> resetImage());
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveEditedImage());
    }
    
    private void loadImage() {
        Intent intent = getIntent();
        originalImageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
        
        if (originalImageUri != null) {
            try {
                currentBitmap = ImageUtils.resizeImageForUpload(this, originalImageUri, 800, 600);
                if (currentBitmap != null) {
                    imageView.setImageBitmap(currentBitmap);
                } else {
                    throw new Exception("Impossible de charger l'image");
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Erreur: URI d'image manquant", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * Faire une rotation de l'image
     */
    private void rotateImage(int degrees) {
        if (currentBitmap != null) {
            currentRotation = (currentRotation + degrees) % 360;
            if (currentRotation < 0) {
                currentRotation += 360;
            }
            
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, 
                currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
            
            if (rotatedBitmap != currentBitmap) {
                currentBitmap.recycle();
            }
            currentBitmap = rotatedBitmap;
            imageView.setImageBitmap(currentBitmap);
            
            updateRotationIndicator();
        }
    }
    
    /**
     * Retourner l'image horizontalement ou verticalement
     */
    private void flipImage(boolean horizontal) {
        if (currentBitmap != null) {
            Matrix matrix = new Matrix();
            if (horizontal) {
                matrix.preScale(-1, 1);
            } else {
                matrix.preScale(1, -1);
            }
            
            Bitmap flippedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
            
            if (flippedBitmap != currentBitmap) {
                currentBitmap.recycle();
            }
            currentBitmap = flippedBitmap;
            imageView.setImageBitmap(currentBitmap);
        }
    }
    
    /**
     * Appliquer un filtre à l'image
     */
    private void applyFilter(ImageUtils.ImageFilter filter) {
        if (currentBitmap != null) {
            Bitmap filteredBitmap = ImageUtils.applyImageFilter(currentBitmap, filter);
            if (filteredBitmap != null && filteredBitmap != currentBitmap) {
                currentBitmap.recycle();
                currentBitmap = filteredBitmap;
                imageView.setImageBitmap(currentBitmap);
            }
        }
    }
    
    /**
     * Réinitialiser l'image à l'état original
     */
    private void resetImage() {
        if (originalImageUri != null) {
            try {
                if (currentBitmap != null && !currentBitmap.isRecycled()) {
                    currentBitmap.recycle();
                }
                currentBitmap = ImageUtils.resizeImageForUpload(this, originalImageUri, 800, 600);
                if (currentBitmap != null) {
                    imageView.setImageBitmap(currentBitmap);
                    currentRotation = 0;
                    updateRotationIndicator();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors de la réinitialisation", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Mettre à jour l'indicateur de rotation
     */
    private void updateRotationIndicator() {
        binding.tvRotationIndicator.setText(String.format("Rotation: %d°", currentRotation));
    }
    
    /**
     * Sauvegarder l'image éditée
     */
    private void saveEditedImage() {
        if (currentBitmap != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSave.setEnabled(false);
            
            // Sauvegarder l'image éditée dans un fichier temporaire
            new Thread(() -> {
                try {
                    java.io.File tempFile = ImageUtils.saveBitmapToTempFile(this, currentBitmap, 
                        "edited_image_" + System.currentTimeMillis());
                    
                    if (tempFile != null) {
                        Uri editedUri = androidx.core.content.FileProvider.getUriForFile(this,
                            "fr.didictateur.inanutshell.fileprovider", tempFile);
                        
                        runOnUiThread(() -> {
                            // Retourner l'URI de l'image éditée
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(EXTRA_EDITED_IMAGE_URI, editedUri);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnSave.setEnabled(true);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                    });
                }
            }).start();
        } else {
            Toast.makeText(this, "Aucune image à sauvegarder", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les références
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
    }
}
