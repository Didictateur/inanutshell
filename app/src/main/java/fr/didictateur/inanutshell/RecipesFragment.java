package fr.didictateur.inanutshell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RecipesFragment extends Fragment {
    
    private AppDatabase db;
    private RecyclerView foldersRecyclerView;
    private RecyclerView recipesRecyclerView;
    private RecetteAdapter foldersAdapter;
    private RecetteAdapter recipesAdapter;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;
    private LinearLayout foldersSection;
    private LinearLayout recipesSection;
    private Long currentFolderId = null; // null = dossier racine
    private List<Item> folderItems; // Liste des dossiers
    private List<Item> recipeItems; // Liste des recettes
    
    // ActivityResultLaunchers pour gérer les retours d'EditRecetteActivity
    private final ActivityResultLauncher<Intent> addRecipeLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Recette recette = new Recette(
                        0,
                        data.getStringExtra("titre"),
                        data.getStringExtra("taille"),
                        data.getStringExtra("tempsPrep"),
                        data.getStringExtra("ingredients"),
                        data.getStringExtra("preparation"),
                        data.getStringExtra("notes"),
                        R.drawable.appicon,
                        data.getStringExtra("photoPath"),
                        currentFolderId // Créer dans le dossier actuel
                    );
                    new Thread(() -> {
                        db.recetteDao().insert(recette);
                        requireActivity().runOnUiThread(this::loadFolderContent);
                    }).start();
                }
            }
        );

    private final ActivityResultLauncher<Intent> editRecipeLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    long recetteId = data.getLongExtra("recetteId", 0);
                    if (recetteId > 0) {
                        // Mise à jour d'une recette existante
                        new Thread(() -> {
                            Recette recette = db.recetteDao().getRecetteById(recetteId);
                            if (recette != null) {
                                recette.titre = data.getStringExtra("titre");
                                recette.taille = data.getStringExtra("taille");
                                recette.tempsPrep = data.getStringExtra("tempsPrep");
                                recette.ingredients = data.getStringExtra("ingredients");
                                recette.preparation = data.getStringExtra("preparation");
                                recette.notes = data.getStringExtra("notes");
                                recette.photoPath = data.getStringExtra("photoPath");
                                recette.parentId = currentFolderId;
                                db.recetteDao().update(recette);
                                requireActivity().runOnUiThread(this::loadFolderContent);
                            }
                        }).start();
                    }
                }
            }
        );
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);
        
        // Initialisation de la base de données
        db = AppDatabase.getInstance(requireContext());
        
        // Configuration des vues
        setupViews(view);
        
        // Chargement du contenu du dossier (dossiers + recettes)
        loadFolderContent();
        
        return view;
    }
    
    private void setupViews(View view) {
        foldersRecyclerView = view.findViewById(R.id.foldersRecyclerView);
        recipesRecyclerView = view.findViewById(R.id.recipesRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        foldersSection = view.findViewById(R.id.foldersSection);
        recipesSection = view.findViewById(R.id.recipesSection);
        
        // Configuration des RecyclerViews
        // Pour les dossiers - grille de 3 colonnes
        GridLayoutManager foldersLayoutManager = new GridLayoutManager(requireContext(), 3);
        foldersRecyclerView.setLayoutManager(foldersLayoutManager);
        
        // Pour les recettes - grille de 2 colonnes
        GridLayoutManager recipesLayoutManager = new GridLayoutManager(requireContext(), 2);
        recipesRecyclerView.setLayoutManager(recipesLayoutManager);
        
        // Configuration des adapters
        folderItems = new ArrayList<>();
        recipeItems = new ArrayList<>();
        foldersAdapter = new RecetteAdapter((ArrayList<Item>) folderItems, requireContext());
        recipesAdapter = new RecetteAdapter((ArrayList<Item>) recipeItems, requireContext());
        foldersRecyclerView.setAdapter(foldersAdapter);
        recipesRecyclerView.setAdapter(recipesAdapter);
        
        // Configuration des listeners pour les dossiers
        foldersAdapter.setOnFolderClickListener(this::navigateToFolder);
        
        // Configuration du listener pour les actions sur les recettes (clic long)
        recipesAdapter.setOnRecetteActionListener((anchor, recette) -> {
            // Pour l'instant, on lance directement l'édition. 
            // Plus tard on peut ajouter un menu contextuel
            Intent intent = new Intent(requireContext(), EditRecetteActivity.class);
            intent.putExtra("recetteId", recette.id);
            editRecipeLauncher.launch(intent);
        });
        
        // FAB pour ajouter une recette ou un dossier
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddMenu(v));
    }
    
    private void loadFolderContent() {
        new Thread(() -> {
            List<Folder> folders = db.folderDao().getFoldersByParent(currentFolderId);
            List<Recette> recettes = db.recetteDao().getRecettesByParent(currentFolderId);

            // Tri alphabétique des dossiers
            folders.sort((f1, f2) -> {
                String nom1 = f1.name != null ? f1.name : "";
                String nom2 = f2.name != null ? f2.name : "";
                return nom1.compareToIgnoreCase(nom2);
            });

            // Tri alphabétique des recettes
            recettes.sort((r1, r2) -> {
                String titre1 = r1.titre != null ? r1.titre : "";
                String titre2 = r2.titre != null ? r2.titre : "";
                return titre1.compareToIgnoreCase(titre2);
            });

            requireActivity().runOnUiThread(() -> {
                // Mise à jour des dossiers
                folderItems.clear();
                folderItems.addAll(folders);
                foldersAdapter.notifyDataSetChanged();
                
                // Mise à jour des recettes
                recipeItems.clear();
                recipeItems.addAll(recettes);
                recipesAdapter.notifyDataSetChanged();
                
                // Gestion de la visibilité des sections
                if (!folderItems.isEmpty()) {
                    foldersSection.setVisibility(View.VISIBLE);
                } else {
                    foldersSection.setVisibility(View.GONE);
                }
                
                if (!recipeItems.isEmpty()) {
                    recipesSection.setVisibility(View.VISIBLE);
                } else {
                    recipesSection.setVisibility(View.GONE);
                }
                
                // État vide si aucun contenu
                if (folderItems.isEmpty() && recipeItems.isEmpty()) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                }
            });
        }).start();
    }
    
    private void navigateToFolder(Folder folder) {
        currentFolderId = folder.id;
        loadFolderContent();
    }
    
    private void showAddMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add("Nouvelle recette");
        popup.getMenu().add("Nouveau dossier");
        popup.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if (title.equals("Nouvelle recette")) {
                Intent intent = new Intent(requireContext(), EditRecetteActivity.class);
                addRecipeLauncher.launch(intent);
            } else if (title.equals("Nouveau dossier")) {
                showCreateFolderDialog();
            }
            return true;
        });
        popup.show();
    }
    
    private void showCreateFolderDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Nouveau dossier");
        
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Nom du dossier");
        builder.setView(input);
        
        builder.setPositiveButton("Créer", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                createFolder(folderName);
            }
        });
        
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
    
    private void createFolder(String name) {
        new Thread(() -> {
            Folder folder = new Folder(0, name, currentFolderId);
            db.folderDao().insert(folder);
            requireActivity().runOnUiThread(this::loadFolderContent);
        }).start();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadFolderContent(); // Recharger quand on revient au fragment
    }
    
    public boolean onBackPressed() {
        // Gérer la navigation arrière dans les dossiers
        if (currentFolderId != null) {
            new Thread(() -> {
                Folder current = db.folderDao().getFolderById(currentFolderId);
                requireActivity().runOnUiThread(() -> {
                    if (current != null && current.getParentId() != null) {
                        // Naviguer vers le dossier parent
                        currentFolderId = current.getParentId();
                        loadFolderContent();
                    } else {
                        // Retourner à la racine
                        currentFolderId = null;
                        loadFolderContent();
                    }
                });
            }).start();
            return true; // Indique que nous avons géré le retour arrière
        }
        return false; // Indique que nous n'avons pas géré le retour arrière
    }
}
