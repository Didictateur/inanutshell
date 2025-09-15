package fr.didictateur.inanutshell.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import fr.didictateur.inanutshell.AppDatabase;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.sync.MealPlanSyncManager;

/**
 * Activité pour gérer la synchronisation des meal plans avec Mealie
 */
public class MealPlanSyncActivity extends AppCompatActivity {
    
    private MealPlanSyncManager syncManager;
    
    private TextView statusText;
    private ProgressBar progressBar;
    private Button syncButton;
    private Button cancelButton;
    
    private boolean isSyncing = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout simple programmatique 
        setupUI();
        
        // Initialiser le gestionnaire de synchronisation
        AppDatabase database = AppDatabase.getInstance(this);
        syncManager = MealPlanSyncManager.getInstance(this, database.mealPlanDao());
        
        updateUI();
    }
    
    private void setupUI() {
        // Layout vertical principal
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        
        // Titre
        TextView titleText = new TextView(this);
        titleText.setText("Synchronisation Meal Plans");
        titleText.setTextSize(24);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 32);
        mainLayout.addView(titleText);
        
        // Status
        statusText = new TextView(this);
        statusText.setText("Prêt pour la synchronisation avec Mealie");
        statusText.setTextSize(16);
        statusText.setPadding(0, 0, 0, 24);
        mainLayout.addView(statusText);
        
        // Progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setVisibility(View.GONE);
        progressBar.setPadding(0, 0, 0, 24);
        mainLayout.addView(progressBar);
        
        // Bouton sync
        syncButton = new Button(this);
        syncButton.setText("Synchroniser avec Mealie");
        syncButton.setOnClickListener(v -> startSync());
        syncButton.setPadding(0, 0, 0, 16);
        mainLayout.addView(syncButton);
        
        // Bouton cancel
        cancelButton = new Button(this);
        cancelButton.setText("Annuler");
        cancelButton.setOnClickListener(v -> finish());
        cancelButton.setEnabled(false);
        mainLayout.addView(cancelButton);
        
        // Info
        TextView infoText = new TextView(this);
        infoText.setText("\nLa synchronisation permet de :\n" +
                "• Télécharger vos meal plans depuis Mealie\n" +
                "• Uploader vos meal plans locaux vers Mealie\n" +
                "• Maintenir la cohérence entre l'app et le serveur\n\n" +
                "Note : Vous devez être connecté à votre serveur Mealie");
        infoText.setTextSize(14);
        infoText.setPadding(0, 24, 0, 0);
        mainLayout.addView(infoText);
        
        setContentView(mainLayout);
        
        // Toolbar simple
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Synchronisation Meal Plans");
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void updateUI() {
        runOnUiThread(() -> {
            syncButton.setEnabled(!isSyncing);
            cancelButton.setEnabled(isSyncing);
            
            if (!isSyncing) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private void startSync() {
        isSyncing = true;
        updateUI();
        
        statusText.setText("Démarrage de la synchronisation...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        
        syncManager.syncAllMealPlans(new MealPlanSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    isSyncing = false;
                    updateUI();
                    statusText.setText("✓ Synchronisation terminée avec succès !");
                    progressBar.setProgress(100);
                    Toast.makeText(MealPlanSyncActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isSyncing = false;
                    updateUI();
                    statusText.setText("✗ Erreur de synchronisation : " + error);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MealPlanSyncActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> {
                    if (total > 0) {
                        int percentage = (current * 100) / total;
                        progressBar.setProgress(percentage);
                        statusText.setText("Synchronisation en cours... " + current + "/" + total + " (" + percentage + "%)");
                    }
                });
            }
        });
    }
}
