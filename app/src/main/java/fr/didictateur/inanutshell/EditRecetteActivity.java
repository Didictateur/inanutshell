package fr.didictateur.inanutshell;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class EditRecetteActivity extends BaseActivity {

    private RecettePagerAdapter adapter;
    private AppDatabase db;
    private long recetteId = 0; // 0 = nouvelle recette, > 0 = modification
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recette);

        db = AppDatabase.getInstance(this);

        // Récupération de l'ID de la recette si on modifie une recette existante
        recetteId = getIntent().getLongExtra("recetteId", 0);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        Button btnSave = findViewById(R.id.btnSave);

        this.viewPager = viewPager;

        adapter = new RecettePagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Forcer la création de tous les fragments
        viewPager.setOffscreenPageLimit(5);

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("Aperçu"); break;
                    case 1: tab.setText("Ingrédients"); break;
                    case 2: tab.setText("Préparation"); break;
                    case 3: tab.setText("Notes"); break;
                    case 4: tab.setText("Photo"); break;
                }
            }
        ).attach();

        // Si on modifie une recette existante, charger ses données
        if (recetteId > 0) {
            loadRecetteData();
        }

        btnSave.setOnClickListener(v -> {
            // Récupère les valeurs de chaque fragment
            String titre = adapter.getApercuFragment().getTitre();
            String taille = adapter.getApercuFragment().getTaille();
            String tempsPrep = adapter.getApercuFragment().getTempsPrep();
            String ingredients = adapter.getIngredientsFragment().getIngredients();
            String preparation = adapter.getPreparationFragment().getPreparation();
            String notes = adapter.getNotesFragment().getNotes();
            String photoPath = adapter.getPhotoFragment().getPhotoPath();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("recetteId", recetteId);
            resultIntent.putExtra("titre", titre);
            resultIntent.putExtra("taille", taille);
            resultIntent.putExtra("tempsPrep", tempsPrep);
            resultIntent.putExtra("ingredients", ingredients);
            resultIntent.putExtra("preparation", preparation);
            resultIntent.putExtra("notes", notes);
            resultIntent.putExtra("photoPath", photoPath);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void loadRecetteData() {
        if (recetteId <= 0) {
            // Nouvelle recette, rien à charger
            return;
        }
        
        new Thread(() -> {
            try {
                Recette recette = db.recetteDao().getRecetteById(recetteId);
                if (recette != null) {
                    runOnUiThread(() -> {
                        // Stocker les données dans l'adapter pour qu'il les applique quand les fragments sont prêts
                        adapter.setRecetteData(recette);
                        
                        // Aussi essayer d'appliquer directement si les fragments sont déjà créés
                        trySetFragmentData(recette);
                    });
                } else {
                    runOnUiThread(() -> {
                        // Recette non trouvée, peut-être afficher un message d'erreur
                        android.widget.Toast.makeText(this, "Recette non trouvée", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Erreur lors du chargement de la recette", android.widget.Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    
    private void trySetFragmentData(Recette recette) {
        // Attendre un peu et essayer plusieurs fois
        viewPager.postDelayed(() -> {
            try {
                if (adapter.getApercuFragment() != null) {
                    adapter.getApercuFragment().setTitre(recette.titre);
                    adapter.getApercuFragment().setTaille(recette.taille);
                    adapter.getApercuFragment().setTempsPrep(recette.tempsPrep);
                }
                if (adapter.getIngredientsFragment() != null) {
                    adapter.getIngredientsFragment().setIngredients(recette.ingredients);
                }
                if (adapter.getPreparationFragment() != null) {
                    adapter.getPreparationFragment().setPreparation(recette.preparation);
                }
                if (adapter.getNotesFragment() != null) {
                    adapter.getNotesFragment().setNotes(recette.notes);
                }
                if (adapter.getPhotoFragment() != null) {
                    adapter.getPhotoFragment().setPhotoPath(recette.photoPath);
                }
            } catch (Exception e) {
                // Si ça échoue, réessayer dans 100ms
                viewPager.postDelayed(() -> trySetFragmentData(recette), 100);
            }
        }, 100);
    }
}

