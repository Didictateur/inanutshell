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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recette);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        Button btnSave = findViewById(R.id.btnSave);

        adapter = new RecettePagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("Aperçu"); break;
                    case 1: tab.setText("Ingrédients"); break;
                    case 2: tab.setText("Préparation"); break;
                    case 3: tab.setText("Notes"); break;
                }
            }
        ).attach();

        btnSave.setOnClickListener(v -> {
            // Récupère les valeurs de chaque fragment
            String titre = adapter.getApercuFragment().getTitre();
            String taille = adapter.getApercuFragment().getTaille();
            String tempsPrep = adapter.getApercuFragment().getTempsPrep();
            String ingredients = adapter.getIngredientsFragment().getIngredients();
            String preparation = adapter.getPreparationFragment().getPreparation();
            String notes = adapter.getNotesFragment().getNotes();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("titre", titre);
            resultIntent.putExtra("taille", taille);
            resultIntent.putExtra("tempsPrep", tempsPrep);
            resultIntent.putExtra("ingredients", ingredients);
            resultIntent.putExtra("preparation", preparation);
            resultIntent.putExtra("notes", notes);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}

