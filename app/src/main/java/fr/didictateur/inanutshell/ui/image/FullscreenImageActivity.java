package fr.didictateur.inanutshell.ui.image;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import fr.didictateur.inanutshell.databinding.ActivityFullscreenImageBinding;
import fr.didictateur.inanutshell.R;

/**
 * Activité pour afficher une image en plein écran
 */
public class FullscreenImageActivity extends AppCompatActivity {
    
    private ActivityFullscreenImageBinding binding;
    
    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Récupérer les données de l'intent
        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
        String recipeName = intent.getStringExtra(EXTRA_RECIPE_NAME);
        
        // Configuration en plein écran
        setupFullscreen();
        
        // Charger l'image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImage(imageUrl);
        } else {
            // Image par défaut si pas d'URL
            binding.imageView.setImageResource(R.drawable.placeholder_recipe);
        }
        
        // Titre de la recette
        if (recipeName != null && !recipeName.isEmpty()) {
            binding.textTitle.setText(recipeName);
        } else {
            binding.textTitle.setVisibility(View.GONE);
        }
        
        // Bouton de fermeture
        binding.buttonClose.setOnClickListener(v -> finish());
        
        // Clic sur l'image pour masquer/afficher les contrôles
        binding.imageView.setOnClickListener(v -> toggleUI());
        
        // Masquer les contrôles après 3 secondes
        binding.getRoot().postDelayed(this::hideUI, 3000);
    }
    
    private void setupFullscreen() {
        // Masquer la barre de navigation et la barre d'état
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    
    private void loadImage(String imageUrl) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.placeholder_recipe)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter(); // Ajuster l'image sans rogner
        
        Glide.with(this)
                .load(imageUrl)
                .apply(options)
                .into(binding.imageView);
    }
    
    private void toggleUI() {
        if (binding.overlayTop.getVisibility() == View.VISIBLE) {
            hideUI();
        } else {
            showUI();
        }
    }
    
    private void hideUI() {
        binding.overlayTop.setVisibility(View.GONE);
        binding.overlayBottom.setVisibility(View.GONE);
    }
    
    private void showUI() {
        binding.overlayTop.setVisibility(View.VISIBLE);
        binding.overlayBottom.setVisibility(View.VISIBLE);
        
        // Masquer automatiquement après 3 secondes
        binding.getRoot().postDelayed(this::hideUI, 3000);
    }
}
