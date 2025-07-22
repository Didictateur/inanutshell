package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DetailRecetteActivity extends AppCompatActivity {
    
    private AppDatabase database;
    private long recetteId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recette);
        
        database = AppDatabase.getInstance(this);
        
        // Récupérer l'ID de la recette
        recetteId = getIntent().getLongExtra("recetteId", -1);
        if (recetteId == -1) {
            finish();
            return;
        }
        
        setupToolbar();
        loadRecette();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détail de la recette");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void loadRecette() {
        new Thread(() -> {
            Recette recette = database.recetteDao().getRecetteById(recetteId);
            
            if (recette != null) {
                runOnUiThread(() -> displayRecette(recette));
            } else {
                runOnUiThread(this::finish);
            }
        }).start();
    }
    
    private void displayRecette(Recette recette) {
        // Utiliser le layout de ViewRecetteActivity
        TextView titleText = findViewById(R.id.titleEditText);
        TextView sizeText = findViewById(R.id.sizeEditText);
        TextView prepTimeText = findViewById(R.id.prepTimeEditText);
        TextView ingredientsText = findViewById(R.id.ingredientsEditText);
        TextView preparationText = findViewById(R.id.preparationEditText);
        TextView notesText = findViewById(R.id.notesEditText);
        ImageView photoImageView = findViewById(R.id.photoImageView);
        
        if (titleText != null) titleText.setText(recette.getTitre());
        if (sizeText != null) sizeText.setText(recette.getTaille());
        if (prepTimeText != null) prepTimeText.setText(recette.getTempsPrep());
        if (ingredientsText != null) ingredientsText.setText(recette.getIngredients());
        if (preparationText != null) preparationText.setText(recette.getPreparation());
        if (notesText != null) notesText.setText(recette.getNotes());
        
        // Charger la photo si disponible
        if (photoImageView != null && recette.getCheminImage() != null && !recette.getCheminImage().isEmpty()) {
            // TODO: Charger l'image avec Glide ou Picasso
            photoImageView.setImageResource(R.drawable.appicon);
        }
    }
}
