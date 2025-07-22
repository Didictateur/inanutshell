package fr.didictateur.inanutshell;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {
    
    private static final String TAG = "SearchActivity";
    
    private EditText searchEditText;
    private ImageButton filterButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout noResultsLayout;
    private SearchResultsAdapter adapter;
    private List<Recette> allRecettes;
    private List<Recette> filteredRecettes;
    private String currentSearchQuery = "";
    
    // Boutons de filtres (remplacent les chips Material)
    private Button chipFavorites;
    
    // Base de données
    private AppDatabase database;
    private RecetteDao recetteDao;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_search);
            Log.d(TAG, "setContentView successful");
            
            // Initialiser la base de données
            database = AppDatabase.getInstance(this);
            recetteDao = database.recetteDao();
            Log.d(TAG, "Database initialized");
            
            initializeViews();
            Log.d(TAG, "initializeViews successful");
            
            setupRecyclerView();
            Log.d(TAG, "setupRecyclerView successful");
            
            setupSearchFunctionality();
            Log.d(TAG, "setupSearchFunctionality successful");
            
            setupFilterChips();
            Log.d(TAG, "setupFilterChips successful");
            
            loadRealData();
            Log.d(TAG, "loadRealData successful");
            
            Log.d(TAG, "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Fallback en cas d'erreur
            setContentView(R.layout.activity_search_simple);
        }
    }
    
    private void initializeViews() {
        try {
            searchEditText = findViewById(R.id.searchEditText);
            filterButton = findViewById(R.id.filterButton);
            searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
            noResultsLayout = findViewById(R.id.noResultsLayout);
            
            chipFavorites = findViewById(R.id.chipFavorites);
            
            Log.d(TAG, "All views found successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error finding views", e);
        }
    }
    
    private void setupRecyclerView() {
        try {
            filteredRecettes = new ArrayList<>();
            adapter = new SearchResultsAdapter(filteredRecettes);
            searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            searchResultsRecyclerView.setAdapter(adapter);
            Log.d(TAG, "RecyclerView setup successful");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }
    
    private void setupSearchFunctionality() {
        try {
            // Gestionnaire pour le champ de recherche
            if (searchEditText != null) {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        currentSearchQuery = s.toString();
                        performSearch(currentSearchQuery);
                    }
                    
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
                
                // Gestionnaire pour la touche "Rechercher" 
                searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                    currentSearchQuery = searchEditText.getText().toString();
                    performSearch(currentSearchQuery);
                    // Empêcher le retour à la ligne
                    return true;
                });
                
                // Configuration pour empêcher les retours à la ligne
                searchEditText.setSingleLine(true);
                searchEditText.setMaxLines(1);
            }
            
            // Gestionnaire pour le bouton de filtres
            if (filterButton != null) {
                filterButton.setOnClickListener(v -> {
                    Log.d(TAG, "Filter button clicked");
                    // Ici on pourrait ouvrir un dialogue de filtres avancés
                });
            }
            
            Log.d(TAG, "Search functionality setup successful");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up search functionality", e);
        }
    }
    
    private void setupFilterChips() {
        try {
            if (chipFavorites != null) {
                chipFavorites.setOnClickListener(v -> {
                    // Toggle behavior pour les boutons
                    chipFavorites.setSelected(!chipFavorites.isSelected());
                    applyFilters();
                });
            }
            Log.d(TAG, "Filter chips setup successful");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter chips", e);
        }
    }
    
    private void loadRealData() {
        try {
            // Charger toutes les vraies recettes de la base de données
            allRecettes = recetteDao.getAllRecettes();
            Log.d(TAG, "Loaded " + allRecettes.size() + " recipes from database");
            
            // Tri alphabétique des recettes
            if (allRecettes != null) {
                allRecettes.sort((r1, r2) -> {
                    String titre1 = r1.titre != null ? r1.titre : "";
                    String titre2 = r2.titre != null ? r2.titre : "";
                    return titre1.compareToIgnoreCase(titre2);
                });
            }
            
            filteredRecettes.clear();
            filteredRecettes.addAll(allRecettes);
            
            if (adapter != null) {
                adapter.updateData(filteredRecettes, "");
                Log.d(TAG, "Adapter updated with " + filteredRecettes.size() + " recipes");
            }
            updateUI();
            Log.d(TAG, "Real data loaded successfully");
            
            // Si aucune recette dans la base, charger les données de test
            if (allRecettes.isEmpty()) {
                Log.d(TAG, "No recipes in database, loading test data");
                loadTestDataAsFallback();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading real data", e);
            // Fallback vers les données de test si erreur
            loadTestDataAsFallback();
        }
    }
    
    private void loadTestDataAsFallback() {
        try {
            allRecettes = createTestData();
            
            // Tri alphabétique des recettes de test
            if (allRecettes != null) {
                allRecettes.sort((r1, r2) -> {
                    String titre1 = r1.titre != null ? r1.titre : "";
                    String titre2 = r2.titre != null ? r2.titre : "";
                    return titre1.compareToIgnoreCase(titre2);
                });
            }
            
            filteredRecettes.clear();
            filteredRecettes.addAll(allRecettes);
            if (adapter != null) {
                adapter.updateData(filteredRecettes, "");
                Log.d(TAG, "Test data adapter updated with " + filteredRecettes.size() + " recipes");
            }
            updateUI();
            Log.d(TAG, "Fallback test data loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading fallback data", e);
        }
    }
    
    private List<Recette> createTestData() {
        List<Recette> testRecettes = new ArrayList<>();
        
        try {
            Recette recette1 = new Recette();
            recette1.titre = "Salade César (test)";
            recette1.ingredients = "Salade, croûtons, parmesan, sauce césar";
            recette1.tempsPrep = "15";
            testRecettes.add(recette1);
            
            Recette recette2 = new Recette();
            recette2.titre = "Pâtes Carbonara (test)";
            recette2.ingredients = "Pâtes, œufs, lardons, parmesan";
            recette2.tempsPrep = "20";
            testRecettes.add(recette2);
            
            Recette recette3 = new Recette();
            recette3.titre = "Tarte aux pommes (test)";
            recette3.ingredients = "Pommes, pâte brisée, sucre, cannelle";
            recette3.tempsPrep = "45";
            testRecettes.add(recette3);
            
            Log.d(TAG, "Test data created as fallback: " + testRecettes.size() + " recipes");
        } catch (Exception e) {
            Log.e(TAG, "Error creating test data", e);
        }
        
        return testRecettes;
    }
    
    private void performSearch(String query) {
        try {
            currentSearchQuery = query;
            filteredRecettes.clear();
            
            if (query.trim().isEmpty()) {
                // Si pas de recherche, afficher toutes les recettes
                if (allRecettes != null) {
                    filteredRecettes.addAll(allRecettes);
                }
            } else {
                // Utiliser la recherche de la base de données
                String searchQuery = "%" + query + "%";
                List<Recette> searchResults = recetteDao.searchAll(searchQuery);
                filteredRecettes.addAll(searchResults);
            }
            
            applyFilters();
            Log.d(TAG, "Search performed for: " + query + ", results: " + filteredRecettes.size());
        } catch (Exception e) {
            Log.e(TAG, "Error performing search", e);
            // Fallback vers recherche locale
            performLocalSearch(query);
        }
    }
    
    private void performLocalSearch(String query) {
        try {
            filteredRecettes.clear();
            
            // Vérifier si allRecettes est null ou vide
            if (allRecettes == null || allRecettes.isEmpty()) {
                Log.d(TAG, "No recipes available for search");
                adapter.updateData(filteredRecettes, query);
                return;
            }
            
            if (query.trim().isEmpty()) {
                filteredRecettes.addAll(allRecettes);
            } else {
                String lowerQuery = query.toLowerCase();
                for (Recette recette : allRecettes) {
                    if ((recette.titre != null && containsSubsequence(recette.titre.toLowerCase(), lowerQuery)) ||
                        (recette.ingredients != null && containsSubsequence(recette.ingredients.toLowerCase(), lowerQuery))) {
                        filteredRecettes.add(recette);
                    }
                }
            }
            
            applyFilters();
        } catch (Exception e) {
            Log.e(TAG, "Error in local search", e);
        }
    }
    
    /**
     * Vérifie si le texte contient toutes les lettres de la requête dans l'ordre
     * Par exemple: "abc" contient "ac" (a puis c)
     */
    private boolean containsSubsequence(String text, String query) {
        if (query == null || query.isEmpty()) return true;
        if (text == null || text.isEmpty()) return false;
        
        int textIndex = 0;
        int queryIndex = 0;
        
        while (textIndex < text.length() && queryIndex < query.length()) {
            if (text.charAt(textIndex) == query.charAt(queryIndex)) {
                queryIndex++;
            }
            textIndex++;
        }
        
        // Si on a trouvé toutes les lettres de la requête
        return queryIndex == query.length();
    }
    
    private void applyFilters() {
        try {
            List<Recette> filtered = new ArrayList<>(filteredRecettes);
            
            // Tri alphabétique des recettes
            filtered.sort((r1, r2) -> {
                String titre1 = r1.titre != null ? r1.titre : "";
                String titre2 = r2.titre != null ? r2.titre : "";
                return titre1.compareToIgnoreCase(titre2);
            });
            
            // Mettre à jour l'adapter avec les résultats filtrés et triés
            if (adapter != null) {
                adapter.updateData(filtered, currentSearchQuery);
            }
            updateUI();
            
            Log.d(TAG, "Filters applied, final results: " + filtered.size());
        } catch (Exception e) {
            Log.e(TAG, "Error applying filters", e);
        }
    }
    
    private void updateUI() {
        try {
            if (searchResultsRecyclerView != null && noResultsLayout != null) {
                if (filteredRecettes.isEmpty()) {
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    noResultsLayout.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Showing no results layout - 0 recipes to display");
                } else {
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    noResultsLayout.setVisibility(View.GONE);
                    Log.d(TAG, "Showing results - " + filteredRecettes.size() + " recipes to display");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }
}
