package fr.didictateur.inanutshell.ui.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.ActivityAdvancedImageEditorBinding;
import fr.didictateur.inanutshell.utils.ImageUtils;

/**
 * Éditeur d'images avancé avec recadrage, filtres, ajustements et annotations
 */
public class AdvancedImageEditorActivity extends AppCompatActivity implements View.OnTouchListener {
    
    private ActivityAdvancedImageEditorBinding binding;
    private Uri originalImageUri;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private Bitmap workingBitmap;
    
    // Outils d'édition
    private EditorMode currentMode = EditorMode.TRANSFORM;
    private boolean isCropMode = false;
    private boolean isDrawingMode = false;
    
    // Paramètres d'ajustements
    private float brightness = 0f;      // -100 à 100
    private float contrast = 1f;        // 0.5 à 2.0
    private float saturation = 1f;      // 0 à 2.0
    private float warmth = 0f;          // -100 à 100
    private float highlights = 0f;      // -100 à 100
    private float shadows = 0f;         // -100 à 100
    
    // Recadrage
    private RectF cropRect = new RectF();
    private boolean isDraggingCrop = false;
    private Paint cropPaint;
    
    // Annotation
    private Paint annotationPaint;
    private Path currentPath;
    private int annotationColor = Color.RED;
    private float annotationSize = 10f;
    
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_EDITED_IMAGE_URI = "edited_image_uri";
    
    public enum EditorMode {
        TRANSFORM,  // Rotation, flip
        CROP,       // Recadrage
        ADJUST,     // Ajustements (luminosité, contraste, etc.)
        FILTER,     // Filtres prédéfinis
        ANNOTATE    // Annotations et dessins
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdvancedImageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupViews();
        setupTabs();
        loadImage();
        initializePaints();
        
        // Add entrance animations for better UX
        animateUIEntrance();
        
        // Show feature guide if first time
        showFeatureGuideIfNeeded();
    }
    
    private void animateUIEntrance() {
        // Import our animation helper
        fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(binding.tabLayout);
        fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(binding.imageView);
        
        // Animate toolbar
        binding.toolbar.setAlpha(0f);
        binding.toolbar.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .start();
        
        // Animate bottom panels with stagger
        binding.transformPanel.setAlpha(0f);
        binding.transformPanel.setTranslationY(100f);
        binding.transformPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(200)
            .start();
    }
    
    private void showFeatureGuideIfNeeded() {
        new android.os.Handler().postDelayed(() -> {
            fr.didictateur.inanutshell.ui.onboarding.FeatureGuideHelper guideHelper = 
                new fr.didictateur.inanutshell.ui.onboarding.FeatureGuideHelper(this);
            guideHelper.showAdvancedEditorGuide();
        }, 800); // Attendre que les animations se terminent
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Éditeur Avancé");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void setupViews() {
        originalImageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        
        // Configuration de l'ImageView
        binding.imageView.setOnTouchListener(this);
        
        // Boutons de base
        binding.btnUndo.setOnClickListener(v -> undoLastAction());
        binding.btnRedo.setOnClickListener(v -> redoLastAction());
        binding.btnReset.setOnClickListener(v -> resetToOriginal());
        binding.btnSave.setOnClickListener(v -> saveEditedImage());
        binding.btnCancel.setOnClickListener(v -> finish());
    }
    
    private void setupTabs() {
        // Configuration des onglets pour les différents outils
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Transform").setIcon(R.drawable.ic_rotate));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Recadrer").setIcon(R.drawable.ic_crop));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Ajuster").setIcon(R.drawable.ic_tune));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Filtres").setIcon(R.drawable.ic_filter));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Annoter").setIcon(R.drawable.ic_edit));
        
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchEditorMode(EditorMode.values()[tab.getPosition()]);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void loadImage() {
        if (originalImageUri != null) {
            try {
                originalBitmap = ImageUtils.resizeImageForUpload(this, originalImageUri, 1024, 1024);
                if (originalBitmap != null) {
                    currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    workingBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    binding.imageView.setImageBitmap(currentBitmap);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors du chargement: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initializePaints() {
        // Paint pour le recadrage
        cropPaint = new Paint();
        cropPaint.setColor(Color.WHITE);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(3f);
        
        // Paint pour les annotations
        annotationPaint = new Paint();
        annotationPaint.setAntiAlias(true);
        annotationPaint.setColor(annotationColor);
        annotationPaint.setStyle(Paint.Style.STROKE);
        annotationPaint.setStrokeWidth(annotationSize);
        annotationPaint.setStrokeCap(Paint.Cap.ROUND);
        annotationPaint.setStrokeJoin(Paint.Join.ROUND);
        
        currentPath = new Path();
    }
    
    private void switchEditorMode(EditorMode mode) {
        currentMode = mode;
        hideAllToolPanels();
        
        switch (mode) {
            case TRANSFORM:
                showTransformTools();
                break;
            case CROP:
                showCropTools();
                break;
            case ADJUST:
                showAdjustmentTools();
                break;
            case FILTER:
                showFilterTools();
                break;
            case ANNOTATE:
                showAnnotationTools();
                break;
        }
    }
    
    private void hideAllToolPanels() {
        binding.transformPanel.setVisibility(View.GONE);
        binding.cropPanel.setVisibility(View.GONE);
        binding.adjustPanel.setVisibility(View.GONE);
        binding.filterPanel.setVisibility(View.GONE);
        binding.annotationPanel.setVisibility(View.GONE);
        isCropMode = false;
        isDrawingMode = false;
    }
    
    private void showTransformTools() {
        binding.transformPanel.setVisibility(View.VISIBLE);
        
        binding.btnRotateLeft.setOnClickListener(v -> rotateImage(-90));
        binding.btnRotateRight.setOnClickListener(v -> rotateImage(90));
        binding.btnFlipHorizontal.setOnClickListener(v -> flipImage(true));
        binding.btnFlipVertical.setOnClickListener(v -> flipImage(false));
    }
    
    private void showCropTools() {
        binding.cropPanel.setVisibility(View.VISIBLE);
        isCropMode = true;
        
        // Initialiser le rectangle de recadrage
        if (currentBitmap != null) {
            int width = currentBitmap.getWidth();
            int height = currentBitmap.getHeight();
            cropRect.set(width * 0.1f, height * 0.1f, width * 0.9f, height * 0.9f);
        }
        
        binding.btnCropApply.setOnClickListener(v -> applyCrop());
        binding.btnCropReset.setOnClickListener(v -> resetCrop());
        binding.btnCropSquare.setOnClickListener(v -> setCropRatio(1, 1));
        binding.btnCrop16x9.setOnClickListener(v -> setCropRatio(16, 9));
        binding.btnCrop4x3.setOnClickListener(v -> setCropRatio(4, 3));
    }
    
    private void showAdjustmentTools() {
        binding.adjustPanel.setVisibility(View.VISIBLE);
        
        setupAdjustmentSliders();
    }
    
    private void setupAdjustmentSliders() {
        // Slider de luminosité
        binding.sliderBrightness.setValueFrom(-100f);
        binding.sliderBrightness.setValueTo(100f);
        binding.sliderBrightness.setValue(brightness);
        binding.sliderBrightness.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                brightness = value;
                applyAdjustments();
            }
        });
        
        // Slider de contraste
        binding.sliderContrast.setValueFrom(0.5f);
        binding.sliderContrast.setValueTo(2f);
        binding.sliderContrast.setValue(contrast);
        binding.sliderContrast.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                contrast = value;
                applyAdjustments();
            }
        });
        
        // Slider de saturation
        binding.sliderSaturation.setValueFrom(0f);
        binding.sliderSaturation.setValueTo(2f);
        binding.sliderSaturation.setValue(saturation);
        binding.sliderSaturation.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                saturation = value;
                applyAdjustments();
            }
        });
        
        // Slider de température (warmth)
        binding.sliderWarmth.setValueFrom(-100f);
        binding.sliderWarmth.setValueTo(100f);
        binding.sliderWarmth.setValue(warmth);
        binding.sliderWarmth.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                warmth = value;
                applyAdjustments();
            }
        });
        
        // Reset adjustments
        binding.btnResetAdjustments.setOnClickListener(v -> resetAdjustments());
    }
    
    private void showFilterTools() {
        binding.filterPanel.setVisibility(View.VISIBLE);
        
        // Setup filter buttons
        binding.btnFilterNone.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.NONE));
        binding.btnFilterSepia.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.SEPIA));
        binding.btnFilterGrayscale.setOnClickListener(v -> applyFilter(ImageUtils.ImageFilter.GRAYSCALE));
        binding.btnFilterVintage.setOnClickListener(v -> applyVintageFilter());
        binding.btnFilterWarm.setOnClickListener(v -> applyWarmFilter());
        binding.btnFilterCool.setOnClickListener(v -> applyCoolFilter());
        binding.btnFilterDramatic.setOnClickListener(v -> applyDramaticFilter());
        binding.btnFilterSoft.setOnClickListener(v -> applySoftFilter());
    }
    
    private void showAnnotationTools() {
        binding.annotationPanel.setVisibility(View.VISIBLE);
        isDrawingMode = true;
        
        // Color picker
        binding.btnColorRed.setOnClickListener(v -> setAnnotationColor(Color.RED));
        binding.btnColorBlue.setOnClickListener(v -> setAnnotationColor(Color.BLUE));
        binding.btnColorGreen.setOnClickListener(v -> setAnnotationColor(Color.GREEN));
        binding.btnColorYellow.setOnClickListener(v -> setAnnotationColor(Color.YELLOW));
        binding.btnColorBlack.setOnClickListener(v -> setAnnotationColor(Color.BLACK));
        binding.btnColorWhite.setOnClickListener(v -> setAnnotationColor(Color.WHITE));
        
        // Size picker
        binding.sliderBrushSize.setValueFrom(5f);
        binding.sliderBrushSize.setValueTo(50f);
        binding.sliderBrushSize.setValue(annotationSize);
        binding.sliderBrushSize.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                annotationSize = value;
                annotationPaint.setStrokeWidth(annotationSize);
            }
        });
        
        // Tool buttons
        binding.btnAddText.setOnClickListener(v -> showTextDialog());
        binding.btnAddArrow.setOnClickListener(v -> setAnnotationMode("arrow"));
        binding.btnAddCircle.setOnClickListener(v -> setAnnotationMode("circle"));
        binding.btnAddRectangle.setOnClickListener(v -> setAnnotationMode("rectangle"));
        binding.btnClearAnnotations.setOnClickListener(v -> clearAllAnnotations());
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isDrawingMode && currentMode == EditorMode.ANNOTATE) {
            return handleAnnotationTouch(event);
        } else if (isCropMode && currentMode == EditorMode.CROP) {
            return handleCropTouch(event);
        }
        return false;
    }
    
    private boolean handleAnnotationTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                drawPathOnBitmap();
                return true;
            case MotionEvent.ACTION_UP:
                // Finaliser le tracé
                return true;
        }
        return false;
    }
    
    private boolean handleCropTouch(MotionEvent event) {
        // Gérer le redimensionnement du rectangle de recadrage
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDraggingCrop = cropRect.contains(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isDraggingCrop) {
                    // Ajuster la taille du rectangle de recadrage
                    updateCropRect(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                isDraggingCrop = false;
                return true;
        }
        return false;
    }
    
    // === MÉTHODES DE TRANSFORMATION ===
    
    private void rotateImage(int degrees) {
        if (currentBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, 
                currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
            
            updateCurrentBitmap(rotatedBitmap);
        }
    }
    
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
            
            updateCurrentBitmap(flippedBitmap);
        }
    }
    
    // === MÉTHODES DE RECADRAGE ===
    
    private void setCropRatio(int widthRatio, int heightRatio) {
        if (currentBitmap != null) {
            float imageWidth = currentBitmap.getWidth();
            float imageHeight = currentBitmap.getHeight();
            
            float targetRatio = (float) widthRatio / heightRatio;
            float currentRatio = imageWidth / imageHeight;
            
            if (currentRatio > targetRatio) {
                // L'image est plus large que le ratio cible
                float newWidth = imageHeight * targetRatio;
                float offsetX = (imageWidth - newWidth) / 2;
                cropRect.set(offsetX, 0, offsetX + newWidth, imageHeight);
            } else {
                // L'image est plus haute que le ratio cible
                float newHeight = imageWidth / targetRatio;
                float offsetY = (imageHeight - newHeight) / 2;
                cropRect.set(0, offsetY, imageWidth, offsetY + newHeight);
            }
            
            updateImageView();
        }
    }
    
    private void applyCrop() {
        if (currentBitmap != null && !cropRect.isEmpty()) {
            try {
                Bitmap croppedBitmap = Bitmap.createBitmap(currentBitmap, 
                    (int) cropRect.left, (int) cropRect.top,
                    (int) cropRect.width(), (int) cropRect.height());
                
                updateCurrentBitmap(croppedBitmap);
                isCropMode = false;
                
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors du recadrage", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void resetCrop() {
        if (currentBitmap != null) {
            int width = currentBitmap.getWidth();
            int height = currentBitmap.getHeight();
            cropRect.set(0, 0, width, height);
            updateImageView();
        }
    }
    
    private void updateCropRect(float x, float y) {
        // Logique pour redimensionner le rectangle de recadrage
        // Simplifiée pour cet exemple
        cropRect.right = Math.max(cropRect.left + 100, x);
        cropRect.bottom = Math.max(cropRect.top + 100, y);
        updateImageView();
    }
    
    // === MÉTHODES D'AJUSTEMENT ===
    
    private void applyAdjustments() {
        if (workingBitmap != null) {
            Bitmap adjustedBitmap = applyColorAdjustments(workingBitmap, 
                brightness, contrast, saturation, warmth);
            updateCurrentBitmap(adjustedBitmap);
        }
    }
    
    private Bitmap applyColorAdjustments(Bitmap original, float brightness, 
                                        float contrast, float saturation, float warmth) {
        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), 
            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        // Créer une matrice de couleur pour les ajustements
        ColorMatrix colorMatrix = new ColorMatrix();
        
        // Appliquer la saturation
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(saturation);
        colorMatrix.postConcat(satMatrix);
        
        // Appliquer la luminosité
        ColorMatrix brightMatrix = new ColorMatrix(new float[] {
            1, 0, 0, 0, brightness,
            0, 1, 0, 0, brightness,
            0, 0, 1, 0, brightness,
            0, 0, 0, 1, 0
        });
        colorMatrix.postConcat(brightMatrix);
        
        // Appliquer le contraste
        float contrastScale = contrast;
        float contrastOffset = (1f - contrast) * 127.5f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
            contrastScale, 0, 0, 0, contrastOffset,
            0, contrastScale, 0, 0, contrastOffset,
            0, 0, contrastScale, 0, contrastOffset,
            0, 0, 0, 1, 0
        });
        colorMatrix.postConcat(contrastMatrix);
        
        // Appliquer la température (warmth)
        if (warmth != 0) {
            float warmthFactor = warmth / 100f;
            ColorMatrix warmthMatrix = new ColorMatrix(new float[] {
                1 + warmthFactor * 0.1f, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1 - warmthFactor * 0.1f, 0, 0,
                0, 0, 0, 1, 0
            });
            colorMatrix.postConcat(warmthMatrix);
        }
        
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        
        return result;
    }
    
    private void resetAdjustments() {
        brightness = 0f;
        contrast = 1f;
        saturation = 1f;
        warmth = 0f;
        
        binding.sliderBrightness.setValue(brightness);
        binding.sliderContrast.setValue(contrast);
        binding.sliderSaturation.setValue(saturation);
        binding.sliderWarmth.setValue(warmth);
        
        updateCurrentBitmap(workingBitmap.copy(Bitmap.Config.ARGB_8888, true));
    }
    
    // === MÉTHODES DE FILTRES ===
    
    private void applyFilter(ImageUtils.ImageFilter filter) {
        if (currentBitmap != null) {
            Bitmap filteredBitmap = ImageUtils.applyImageFilter(workingBitmap, filter);
            if (filteredBitmap != null) {
                updateCurrentBitmap(filteredBitmap);
            }
        }
    }
    
    private void applyVintageFilter() {
        // Filtre vintage personnalisé
        if (workingBitmap != null) {
            Bitmap vintageBitmap = applyColorAdjustments(workingBitmap, 10f, 1.2f, 0.8f, 20f);
            updateCurrentBitmap(vintageBitmap);
        }
    }
    
    private void applyWarmFilter() {
        if (workingBitmap != null) {
            Bitmap warmBitmap = applyColorAdjustments(workingBitmap, 5f, 1.1f, 1.1f, 40f);
            updateCurrentBitmap(warmBitmap);
        }
    }
    
    private void applyCoolFilter() {
        if (workingBitmap != null) {
            Bitmap coolBitmap = applyColorAdjustments(workingBitmap, -5f, 1.1f, 1.1f, -40f);
            updateCurrentBitmap(coolBitmap);
        }
    }
    
    private void applyDramaticFilter() {
        if (workingBitmap != null) {
            Bitmap dramaticBitmap = applyColorAdjustments(workingBitmap, -10f, 1.5f, 1.3f, 0f);
            updateCurrentBitmap(dramaticBitmap);
        }
    }
    
    private void applySoftFilter() {
        if (workingBitmap != null) {
            Bitmap softBitmap = applyColorAdjustments(workingBitmap, 15f, 0.8f, 0.9f, 10f);
            updateCurrentBitmap(softBitmap);
        }
    }
    
    // === MÉTHODES D'ANNOTATION ===
    
    private void setAnnotationColor(int color) {
        annotationColor = color;
        annotationPaint.setColor(color);
    }
    
    private void setAnnotationMode(String mode) {
        // Changer le mode d'annotation (arrow, circle, etc.)
        Toast.makeText(this, "Mode " + mode + " activé", Toast.LENGTH_SHORT).show();
    }
    
    private void showTextDialog() {
        // Afficher une boîte de dialogue pour ajouter du texte
        // TODO: Implémenter la boîte de dialogue de texte
        Toast.makeText(this, "Ajout de texte - à implémenter", Toast.LENGTH_SHORT).show();
    }
    
    private void drawPathOnBitmap() {
        if (currentBitmap != null) {
            Canvas canvas = new Canvas(currentBitmap);
            canvas.drawPath(currentPath, annotationPaint);
            binding.imageView.setImageBitmap(currentBitmap);
        }
    }
    
    private void clearAllAnnotations() {
        if (workingBitmap != null) {
            updateCurrentBitmap(workingBitmap.copy(Bitmap.Config.ARGB_8888, true));
            currentPath.reset();
        }
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    private void updateCurrentBitmap(Bitmap newBitmap) {
        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
        currentBitmap = newBitmap;
        workingBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        binding.imageView.setImageBitmap(currentBitmap);
    }
    
    private void updateImageView() {
        binding.imageView.setImageBitmap(currentBitmap);
        binding.imageView.invalidate();
    }
    
    private void undoLastAction() {
        // TODO: Implémenter l'historique des actions
        Toast.makeText(this, "Annuler - à implémenter", Toast.LENGTH_SHORT).show();
    }
    
    private void redoLastAction() {
        // TODO: Implémenter l'historique des actions
        Toast.makeText(this, "Refaire - à implémenter", Toast.LENGTH_SHORT).show();
    }
    
    private void resetToOriginal() {
        if (originalBitmap != null) {
            updateCurrentBitmap(originalBitmap.copy(Bitmap.Config.ARGB_8888, true));
            resetAdjustments();
            currentPath.reset();
            
            // Reset crop
            if (currentBitmap != null) {
                cropRect.set(0, 0, currentBitmap.getWidth(), currentBitmap.getHeight());
            }
            
            Toast.makeText(this, "Image réinitialisée", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveEditedImage() {
        if (currentBitmap != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSave.setEnabled(false);
            
            new Thread(() -> {
                try {
                    java.io.File tempFile = ImageUtils.saveBitmapToTempFile(this, currentBitmap, 
                        "advanced_edited_" + System.currentTimeMillis());
                    
                    if (tempFile != null) {
                        Uri editedUri = androidx.core.content.FileProvider.getUriForFile(this,
                            "fr.didictateur.inanutshell.fileprovider", tempFile);
                        
                        runOnUiThread(() -> {
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
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les bitmaps
        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
        if (workingBitmap != null && !workingBitmap.isRecycled() && workingBitmap != originalBitmap) {
            workingBitmap.recycle();
        }
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
    }
}
