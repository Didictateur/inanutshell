package fr.didictateur.inanutshell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RecipesFragment extends Fragment {
    
    private AppDatabase db;
    private RecyclerView recyclerView;
    private RecetteAdapter adapter;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);
        
        // Initialisation de la base de données
        db = AppDatabase.getInstance(requireContext());
        
        // Configuration des vues
        setupViews(view);
        
        // Chargement des recettes
        loadRecettes();
        
        return view;
    }
    
    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        
        // Configuration du RecyclerView avec une grille de 2 colonnes
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        
        // Configuration de l'adapter
        adapter = new RecetteAdapter(new ArrayList<>(), requireContext());
        recyclerView.setAdapter(adapter);
        
        // FAB pour ajouter une recette
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditRecetteActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadRecettes() {
        new Thread(() -> {
            List<Recette> recettes = db.recetteDao().getAllRecettes();
            
            // Conversion en liste d'Items pour l'adapter
            List<Item> items = new ArrayList<>();
            for (Recette recette : recettes) {
                items.add(recette);
            }
            
            requireActivity().runOnUiThread(() -> {
                adapter.updateItems(items);
                if (items.isEmpty()) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadRecettes(); // Recharger quand on revient au fragment
    }
}
