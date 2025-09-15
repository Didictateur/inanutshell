package fr.didictateur.inanutshell.ui.recipes;

import android.content.Intent;
import android.os.Bundle;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.databinding.FragmentRecipesBinding;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.ui.main.MainActivity;
import fr.didictateur.inanutshell.ui.search.SearchFilters;
import fr.didictateur.inanutshell.ui.search.SearchFilterListener;
import fr.didictateur.inanutshell.ui.image.FullscreenImageActivity;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.ui.setup.SetupActivity;
import fr.didictateur.inanutshell.utils.MealiePreferences;
import fr.didictateur.inanutshell.ui.dialogs.RatingDialog;
import fr.didictateur.inanutshell.utils.RatingManager;
import fr.didictateur.inanutshell.utils.OfflineManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import fr.didictateur.inanutshell.utils.FavoritesManager;
import fr.didictateur.inanutshell.utils.AccessibilityHelper;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener, SearchFilterListener, RecipeSwipeHelper.OnRecipeSwipeListener {
    
    private FragmentRecipesBinding binding;
    private RecipeAdapter adapter;
    private MealiePreferences preferences;
    private OfflineManager offlineManager;
    private List<Recipe> allRecipes = new ArrayList<>(); // Toutes les recettes chargées
    private List<Recipe> filteredRecipes = new ArrayList<>(); // Recettes filtrées
    private SearchFilters currentFilters = new SearchFilters();
    
    // Variables pour la pagination et scroll infini
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 20;
    
    // Gesture navigation
    private RecipeSwipeHelper swipeHelper;
    private FavoritesManager favoritesManager;
    
    // Device modes
    private boolean isTabletMode = false;
    private boolean isLandscapeMode = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipes, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        preferences = new MealiePreferences(requireContext());
        offlineManager = OfflineManager.getInstance(requireContext());
        favoritesManager = FavoritesManager.getInstance(requireContext());
        
        // Détecter le mode de l'appareil
        isTabletMode = isTabletDevice();
        isLandscapeMode = isLandscapeOrientation();
        
        setupRecyclerView();
        setupSwipeRefresh();
        
        if (isTabletMode) {
            setupTabletPanel();
        } else if (isLandscapeMode) {
            setupLandscapeMode();
        } else {
            setupFAB(view);
        }
        
        checkSetupAndLoadRecipes();
        
        // Register this fragment as search filter listener
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSearchFilterListener(this);
        }
    }
    

    
    private void setupRecyclerView() {
        adapter = new RecipeAdapter(filteredRecipes, this);
        
        // Use grid layout for better recipe display
        int spanCount = getResources().getInteger(R.integer.recipe_grid_span_count);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
        
        // Ajouter le listener pour le scroll infini
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Détecter si on approche de la fin de la liste
                int totalItemCount = layoutManager.getItemCount();
                int visibleItemCount = layoutManager.getChildCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Charger plus d'éléments quand on arrive à 5 éléments de la fin
                if (!isLoading && !isLastPage && 
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && 
                    firstVisibleItemPosition >= 0 && 
                    totalItemCount >= ITEMS_PER_PAGE) {
                    
                    loadMoreRecipes();
                }
            }
        });
        
        // Configurer les gestures de swipe
        swipeHelper = new RecipeSwipeHelper(adapter, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(binding.recyclerView);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadRecipes);
        
        // Configuration des couleurs Material Design pour l'animation
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.primary_variant,
            R.color.secondary,
            R.color.info
        );
        
        // Configuration de la distance de déclenchement
        binding.swipeRefresh.setDistanceToTriggerSync(300);
        
        // Configuration du style de progression
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface);
        binding.swipeRefresh.setSize(SwipeRefreshLayout.LARGE);
        
        // Configuration de l'accessibilité
        binding.swipeRefresh.setContentDescription("Glisser vers le bas pour actualiser les recettes");
    }
    
    private com.google.android.material.floatingactionbutton.FloatingActionButton mainFab;
    
    private void setupFAB(View view) {
        // Créer le FAB programmatiquement pour être sûr qu'il apparaisse
        if (view instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout coordinator = 
                (androidx.coordinatorlayout.widget.CoordinatorLayout) view;
            
            // Créer le FAB principal
            mainFab = new com.google.android.material.floatingactionbutton.FloatingActionButton(requireContext());
            
            // Configuration du FAB
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT
                );
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            params.setMargins(0, 0, 48, 48); // margins en dp
            
            mainFab.setLayoutParams(params);
            updateFabState();
            
            // Ajouter au layout
            coordinator.addView(mainFab);
            
            android.util.Log.d("RecipesFragment", "FAB créé et ajouté programmatiquement");
        }
    }
    
    /**
     * Met à jour l'état du FAB selon le contexte
     */
    private void updateFabState() {
        if (mainFab == null) return;
        
        boolean hasRecipes = !filteredRecipes.isEmpty();
        boolean isSearching = currentFilters.hasActiveFilters();
        
        if (isSearching) {
            // Mode recherche - FAB pour effacer les filtres
            mainFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            mainFab.setContentDescription("Effacer les filtres");
            setFabBackground(R.color.secondary);
            mainFab.setOnClickListener(v -> {
                clearFilters();
                Toast.makeText(getContext(), "Filtres effacés", Toast.LENGTH_SHORT).show();
            });
        } else if (hasRecipes) {
            // Mode normal avec recettes - FAB pour ajouter
            mainFab.setImageResource(android.R.drawable.ic_input_add);
            mainFab.setContentDescription("Ajouter une recette");
            setFabBackground(R.color.primary);
            mainFab.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
                startActivity(intent);
            });
        } else {
            // Mode vide - FAB pour recharger
            mainFab.setImageResource(android.R.drawable.ic_popup_sync);
            mainFab.setContentDescription("Recharger les recettes");
            setFabBackground(R.color.info);
            mainFab.setOnClickListener(v -> {
                loadRecipes();
                Toast.makeText(getContext(), "Rechargement...", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void setFabBackground(int colorRes) {
        try {
            mainFab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
            ));
        } catch (Exception e) {
            // Couleur par défaut si problème
            mainFab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
        }
    }
    
    /**
     * Efface tous les filtres actifs
     */
    private void clearFilters() {
        currentFilters = new SearchFilters();
        
        // Notifier l'activité principale pour effacer les champs de recherche
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearSearchFields();
        }
        
        // Réappliquer les filtres (maintenant vides)
        applyFilters();
    }
    
    /**
     * Détermine si l'appareil est une tablette
     */
    private boolean isTabletDevice() {
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        
        float widthDp = displayMetrics.widthPixels / displayMetrics.density;
        float heightDp = displayMetrics.heightPixels / displayMetrics.density;
        float screenSizeInches = (float) Math.sqrt(Math.pow(widthDp / 160, 2) + Math.pow(heightDp / 160, 2));
        
        // Considérer comme tablette si l'écran fait plus de 7 pouces
        return screenSizeInches >= 7.0;
    }
    
    /**
     * Configure le panneau latéral spécifique aux tablettes
     */
    private void setupTabletPanel() {
        // Vérifier si les vues du panneau tablette existent
        if (binding.btnAddRecipe != null) {
            binding.btnAddRecipe.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
                startActivity(intent);
            });
        }
        
        if (binding.btnAdvancedSearch != null) {
            binding.btnAdvancedSearch.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.search.AdvancedSearchActivity.class);
                startActivity(intent);
            });
        }
        
        if (binding.btnFavorites != null) {
            binding.btnFavorites.setOnClickListener(v -> {
                // Basculer vers l'onglet favoris
                if (getActivity() instanceof MainActivity) {
                    // Simuler un clic sur l'onglet favoris
                    Toast.makeText(getContext(), "Basculer vers les favoris", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Mettre à jour les statistiques
        updateTabletStats();
    }
    
    /**
     * Met à jour les statistiques affichées dans le panneau tablette
     */
    private void updateTabletStats() {
        if (!isTabletMode) return;
        
        if (binding.tvTotalRecipes != null) {
            binding.tvTotalRecipes.setText("Total: " + allRecipes.size() + " recettes");
        }
        
        if (binding.tvFavoritesCount != null && favoritesManager != null) {
            int favoriteCount = favoritesManager.getFavoriteCount();
            binding.tvFavoritesCount.setText("Favoris: " + favoriteCount);
        }
        
        if (binding.tvConnectionStatus != null && offlineManager != null) {
            boolean isOnline = offlineManager.isOnline();
            binding.tvConnectionStatus.setText(isOnline ? "En ligne" : "Hors ligne");
            binding.tvConnectionStatus.setTextColor(androidx.core.content.ContextCompat.getColor(
                requireContext(), 
                isOnline ? R.color.success : R.color.warning
            ));
            binding.tvConnectionStatus.setCompoundDrawablesWithIntrinsicBounds(
                isOnline ? android.R.drawable.presence_online : android.R.drawable.presence_busy,
                0, 0, 0
            );
        }
    }
    
    /**
     * Détermine si l'appareil est en mode paysage
     */
    private boolean isLandscapeOrientation() {
        android.content.res.Configuration configuration = getResources().getConfiguration();
        return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }
    
    /**
     * Configure l'interface pour le mode paysage
     */
    private void setupLandscapeMode() {
        // En mode paysage, le FAB est déjà dans le layout
        if (binding.fabAdd != null) {
            binding.fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
                startActivity(intent);
            });
            
            // Ajuster l'apparence selon le contexte
            updateLandscapeFAB();
        }
    }
    
    /**
     * Met à jour le FAB en mode paysage selon le contexte
     */
    private void updateLandscapeFAB() {
        if (!isLandscapeMode || binding.fabAdd == null) return;
        
        boolean hasRecipes = !filteredRecipes.isEmpty();
        boolean isSearching = currentFilters.hasActiveFilters();
        
        if (isSearching) {
            // Mode recherche - FAB pour effacer les filtres
            binding.fabAdd.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.fabAdd.setContentDescription("Effacer les filtres");
            setLandscapeFabBackground(R.color.secondary);
            binding.fabAdd.setOnClickListener(v -> {
                clearFilters();
                Toast.makeText(getContext(), "Filtres effacés", Toast.LENGTH_SHORT).show();
            });
        } else {
            // Mode normal - FAB pour ajouter une recette
            binding.fabAdd.setImageResource(android.R.drawable.ic_input_add);
            binding.fabAdd.setContentDescription("Ajouter une recette");
            setLandscapeFabBackground(R.color.primary);
            binding.fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
                startActivity(intent);
            });
        }
    }
    
    private void setLandscapeFabBackground(int colorRes) {
        if (binding.fabAdd == null) return;
        
        try {
            binding.fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
            ));
        } catch (Exception e) {
            // Couleur par défaut si problème
            binding.fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
        }
    }
    

    
    private void checkSetupAndLoadRecipes() {
        if (!preferences.hasValidCredentials()) {
            showSetupRequired();
        } else {
            loadRecipes();
        }
    }
    
    private void showSetupRequired() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(R.string.setup_required);
        binding.tvEmptyMessage.setText(R.string.setup_required_message);
        binding.btnAction.setText(R.string.setup);
        binding.btnAction.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SetupActivity.class));
        });
    }
    
    private void loadRecipes() {
        if (!preferences.hasValidCredentials()) {
            binding.swipeRefresh.setRefreshing(false);
            showSetupRequired();
            return;
        }
        
        // Réinitialiser la pagination lors d'un refresh
        currentPage = 1;
        isLoading = false;
        isLastPage = false;
        
        binding.swipeRefresh.setRefreshing(true);
        showLoading(true);
        
        // Si hors ligne, charger depuis le cache
        if (!offlineManager.isOnline()) {
            loadFromCache();
            return;
        }
        
        // Si en ligne, charger depuis le réseau et mettre en cache
        NetworkManager.getInstance().getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipeList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allRecipes.clear();
                        allRecipes.addAll(recipeList);
                        
                        // Mettre en cache les recettes récupérées
                        cacheRecipes(recipeList);
                        
                        // Sync favorites status with local storage
                        syncFavoritesStatus();
                        
                        // Sync user ratings with local storage
                        syncRatingsStatus();
                        
                        // Apply current filters
                        applyFilters();
                        
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                        
                        if (filteredRecipes.isEmpty()) {
                            if (allRecipes.isEmpty()) {
                                showEmptyState();
                            } else {
                                showNoResultsState();
                            }
                        } else {
                            showRecipes();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), 
                            getString(R.string.error_loading_recipes, error), 
                            Toast.LENGTH_LONG).show();
                        
                        if (allRecipes.isEmpty()) {
                            showErrorState(error);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Charger les recettes depuis le cache (mode hors ligne)
     */
    private void loadFromCache() {
        offlineManager.getAllCachedRecipes(new OfflineManager.CacheCallback() {
            @Override
            public void onSuccess(List<Recipe> cachedRecipeList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allRecipes.clear();
                        allRecipes.addAll(cachedRecipeList);
                        
                        // Sync favorites status with local storage
                        syncFavoritesStatus();
                        
                        // Sync user ratings with local storage  
                        syncRatingsStatus();
                        
                        // Apply current filters
                        applyFilters();
                        
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                        
                        if (filteredRecipes.isEmpty()) {
                            if (allRecipes.isEmpty()) {
                                showOfflineEmptyState();
                            } else {
                                showNoResultsState();
                            }
                        } else {
                            showRecipes();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        binding.swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), 
                            "Mode hors ligne - Erreur de cache: " + error, 
                            Toast.LENGTH_LONG).show();
                        showOfflineEmptyState();
                    });
                }
            }
        });
    }
    
    /**
     * Mettre en cache les recettes récupérées
     */
    private void cacheRecipes(List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            offlineManager.cacheRecipe(recipe);
        }
    }
    
    /**
     * Charger plus de recettes pour le scroll infini
     */
    private void loadMoreRecipes() {
        if (isLoading || isLastPage || !offlineManager.isOnline()) {
            return;
        }
        
        isLoading = true;
        currentPage++;
        
        // Afficher l'indicateur de chargement en bas de la liste
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter.setLoadingMoreEnabled(true);
            });
        }
        
        // Charger la page suivante depuis le réseau
        NetworkManager.getInstance().getRecipesPage(currentPage, ITEMS_PER_PAGE, new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> newRecipes) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        adapter.setLoadingMoreEnabled(false);
                        
                        if (newRecipes == null || newRecipes.isEmpty()) {
                            // Pas plus de recettes à charger
                            isLastPage = true;
                            return;
                        }
                        
                        // Ajouter les nouvelles recettes aux listes
                        int previousSize = allRecipes.size();
                        allRecipes.addAll(newRecipes);
                        
                        // Mettre en cache les nouvelles recettes
                        cacheRecipes(newRecipes);
                        
                        // Sync favorites and ratings for new recipes
                        syncFavoritesStatus();
                        syncRatingsStatus();
                        
                        // Appliquer les filtres et notifier l'adapter
                        applyFilters();
                        
                        // Marquer comme dernière page si on a reçu moins que prévu
                        if (newRecipes.size() < ITEMS_PER_PAGE) {
                            isLastPage = true;
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        adapter.setLoadingMoreEnabled(false);
                        currentPage--; // Réessayer la même page la prochaine fois
                        Toast.makeText(getContext(), 
                            "Erreur lors du chargement: " + error, 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            AccessibilityHelper.configureProgressAccessibility(
                binding.progressBar, 
                "chargement des recettes"
            );
            AccessibilityHelper.announceForAccessibility(
                binding.progressBar, 
                "Chargement des recettes en cours"
            );
        }
    }
    
    private void showRecipes() {
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        
        // Garder le bouton de création visible même avec des recettes
        if (binding.btnAction != null) {
            binding.btnAction.setVisibility(View.VISIBLE);
        }
        
        // Mettre à jour l'interface selon le mode
        if (isTabletMode) {
            updateTabletStats();
        } else if (isLandscapeMode) {
            updateLandscapeFAB();
        } else {
            updateFabState();
        }
    }
    
    private void showEmptyState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(R.string.no_recipes);
        binding.tvEmptyMessage.setText(R.string.no_recipes_message);
        binding.btnAction.setText(R.string.refresh);
        
        // Configuration de l'accessibilité pour l'état vide
        AccessibilityHelper.configureEmptyStateAccessibility(
            binding.emptyState,
            getString(R.string.no_recipes),
            getString(R.string.no_recipes_message)
        );
        
        binding.btnAction.setContentDescription("Actualiser pour charger les recettes");
        binding.btnAction.setOnClickListener(v -> loadRecipes());
        
        if (isTabletMode) {
            updateTabletStats();
        } else if (isLandscapeMode) {
            updateLandscapeFAB();
        } else {
            updateFabState();
        }
    }
    
    private void showErrorState(String error) {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(R.string.error_loading_recipes_title);
        binding.tvEmptyMessage.setText(getString(R.string.error_loading_recipes, error));
        binding.btnAction.setText(R.string.retry);
        binding.btnAction.setOnClickListener(v -> loadRecipes());
        
        if (isTabletMode) {
            updateTabletStats();
        } else if (isLandscapeMode) {
            updateLandscapeFAB();
        } else {
            updateFabState();
        }
    }
    
    private void showOfflineEmptyState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText("Mode hors ligne");
        binding.tvEmptyMessage.setText("Aucune recette mise en cache. Connectez-vous pour voir vos recettes.");
        binding.btnAction.setText("Réessayer");
        binding.btnAction.setOnClickListener(v -> loadRecipes());
    }
    
    @Override
    public void onRecipeClick(Recipe recipe) {
        // Naviguer vers le détail de la recette
        Intent intent = new Intent(getContext(), fr.didictateur.inanutshell.ui.recipe.RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("recipe_name", recipe.getName());
        startActivity(intent);
    }
    
    @Override
    public void onRecipeFavoriteClick(Recipe recipe) {
        // Toggle favorite status using FavoritesManager
        fr.didictateur.inanutshell.utils.FavoritesManager favoritesManager = 
            fr.didictateur.inanutshell.utils.FavoritesManager.getInstance(requireContext());
        
        favoritesManager.toggleFavorite(recipe.getId());
        
        // Update the recipe object
        recipe.setFavorite(favoritesManager.isFavorite(recipe.getId()));
        
        // Synchroniser avec le cache offline
        offlineManager.updateFavoriteStatusInCache(recipe.getId(), recipe.isFavorite());
        
        // Update UI
        adapter.notifyDataSetChanged();
        
        // Show feedback to user
        String message = recipe.isFavorite() ? 
            getString(R.string.added_to_favorites) : getString(R.string.removed_from_favorites);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        
        // Re-apply filters if needed (if showing favorites only and recipe was removed)
        if (currentFilters.isFavoritesOnly()) {
            applyFilters();
        }
    }
    
    @Override
    public void onRecipeImageClick(Recipe recipe) {
        // Ouvrir l'image en plein écran
        String imageUrl = NetworkManager.getInstance().getRecipeImageUrl(recipe.getId());
        
        Intent intent = new Intent(getContext(), FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(FullscreenImageActivity.EXTRA_RECIPE_NAME, recipe.getName());
        startActivity(intent);
    }
    
    @Override
    public void onRecipeRatingClick(Recipe recipe) {
        // Show rating dialog for the recipe
        RatingDialog ratingDialog = new RatingDialog(requireContext(), recipe, new RatingDialog.OnRatingSetListener() {
            @Override
            public void onRatingSet(Recipe updatedRecipe, float newRating) {
                // Update recipe in our list
                for (Recipe r : allRecipes) {
                    if (r.getId().equals(updatedRecipe.getId())) {
                        r.setUserRating(newRating);
                        break;
                    }
                }
                
                // Update filtered recipes if recipe is currently shown
                for (Recipe r : filteredRecipes) {
                    if (r.getId().equals(updatedRecipe.getId())) {
                        r.setUserRating(newRating);
                        break;
                    }
                }
                
                // Synchroniser avec le cache offline
                offlineManager.updateRatingInCache(updatedRecipe.getId(), newRating);
                
                // Refresh adapter to show new rating
                adapter.notifyDataSetChanged();
                
                // Show feedback
                String message = newRating > 0 ? 
                    "Note sauvegardée !" : "Note supprimée !";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        
        ratingDialog.show();
    }
    

    
    // Implementation of SearchFilterListener
    @Override
    public void onFiltersChanged(SearchFilters filters) {
        this.currentFilters = filters;
        applyFilters();
    }
    
    @Override
    public void onClearFilters() {
        this.currentFilters = new SearchFilters();
        applyFilters();
    }
    
    /**
     * Apply current search filters to the recipe list
     */
    private void applyFilters() {
        filteredRecipes.clear();
        
        if (currentFilters.isEmpty()) {
            // No filters, show all recipes
            filteredRecipes.addAll(allRecipes);
        } else {
            // Apply filters
            for (Recipe recipe : allRecipes) {
                if (matchesFilters(recipe)) {
                    filteredRecipes.add(recipe);
                }
            }
        }
        
        // Update UI
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // Update empty state
        if (filteredRecipes.isEmpty()) {
            if (allRecipes.isEmpty()) {
                showEmptyState();
            } else {
                showNoResultsState();
            }
        } else {
            showRecipes();
        }
    }
    
    /**
     * Check if a recipe matches current search filters
     */
    private boolean matchesFilters(Recipe recipe) {
        // Text search (titre, description)
        if (!currentFilters.getTextQuery().isEmpty()) {
            String query = currentFilters.getTextQuery().toLowerCase();
            String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
            String recipeDescription = recipe.getDescription() != null ? recipe.getDescription().toLowerCase() : "";
            
            if (!recipeName.contains(query) && !recipeDescription.contains(query)) {
                return false;
            }
        }
        
        // Ingredient search
        if (!currentFilters.getIngredient().isEmpty()) {
            String ingredientQuery = currentFilters.getIngredient().toLowerCase();
            boolean ingredientFound = false;
            
            if (recipe.getRecipeIngredient() != null) {
                for (fr.didictateur.inanutshell.data.model.RecipeIngredient ingredient : recipe.getRecipeIngredient()) {
                    // Check in food name
                    String food = ingredient.getFood() != null ? ingredient.getFood().toLowerCase() : "";
                    // Check in display text
                    String display = ingredient.getDisplay() != null ? ingredient.getDisplay().toLowerCase() : "";
                    // Check in original text
                    String original = ingredient.getOriginalText() != null ? ingredient.getOriginalText().toLowerCase() : "";
                    
                    if (food.contains(ingredientQuery) || display.contains(ingredientQuery) || original.contains(ingredientQuery)) {
                        ingredientFound = true;
                        break;
                    }
                }
            }
            
            if (!ingredientFound) {
                return false;
            }
        }
        
        // Category filters
        if (currentFilters.getCategories() != null && !currentFilters.getCategories().isEmpty()) {
            boolean categoryMatches = false;
            
            if (recipe.getCategories() != null) {
                for (fr.didictateur.inanutshell.data.model.Category category : recipe.getCategories()) {
                    if (category.getName() != null && currentFilters.getCategories().contains(category.getName())) {
                        categoryMatches = true;
                        break;
                    }
                }
            }
            
            if (!categoryMatches) {
                return false;
            }
        }
        
        // Tag filters
        if (currentFilters.getTags() != null && !currentFilters.getTags().isEmpty()) {
            boolean tagMatches = false;
            
            if (recipe.getTags() != null) {
                for (fr.didictateur.inanutshell.data.model.Tag tag : recipe.getTags()) {
                    if (tag.getName() != null && currentFilters.getTags().contains(tag.getName())) {
                        tagMatches = true;
                        break;
                    }
                }
            }
            
            if (!tagMatches) {
                return false;
            }
        }
        
        // Time filters
        if (currentFilters.getMaxPrepTime() != null) {
            Integer recipePrepTime = parseTimeToMinutes(recipe.getPrepTime());
            if (recipePrepTime != null && recipePrepTime > currentFilters.getMaxPrepTime()) {
                return false;
            }
        }
        
        if (currentFilters.getMaxCookTime() != null) {
            Integer recipeCookTime = parseTimeToMinutes(recipe.getCookTime());
            if (recipeCookTime != null && recipeCookTime > currentFilters.getMaxCookTime()) {
                return false;
            }
        }
        
        // Favorites filter
        if (currentFilters.isFavoritesOnly() && !recipe.isFavorite()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Show state when no search results are found
     */
    private void showNoResultsState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText("Aucun résultat");
        binding.tvEmptyMessage.setText("Aucune recette ne correspond à votre recherche. Essayez avec d'autres mots-clés.");
        binding.btnAction.setText(R.string.retry);
        
        if (isTabletMode) {
            updateTabletStats();
        } else if (isLandscapeMode) {
            updateLandscapeFAB();
        } else {
            updateFabState();
        }
        binding.btnAction.setOnClickListener(v -> {
            // Clear search and reload
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                // Clear current search - this will trigger onClearFilters()
                // For now, just reload recipes
                loadRecipes();
            }
        });
    }
    
    @Override
    public void onRecipeLongClick(Recipe recipe, View view) {
        showRecipeContextMenu(recipe, view);
    }
    
    private void showRecipeContextMenu(Recipe recipe, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.recipe_context_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                editRecipe(recipe);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteConfirmationDialog(recipe);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void editRecipe(Recipe recipe) {
        Intent intent = new Intent(getActivity(), fr.didictateur.inanutshell.ui.edit.EditRecipeActivity.class);
        // TODO: Pass recipe data to edit activity
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("recipe_name", recipe.getName());
        intent.putExtra("recipe_description", recipe.getDescription());
        startActivity(intent);
    }
    
    private void showDeleteConfirmationDialog(Recipe recipe) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Supprimer la recette")
            .setMessage("Êtes-vous sûr de vouloir supprimer \"" + recipe.getName() + "\" ?")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteRecipe(recipe))
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void deleteRecipe(Recipe recipe) {
        // Afficher un indicateur de chargement
        Toast.makeText(getContext(), "Suppression de \"" + recipe.getName() + "\"...", Toast.LENGTH_SHORT).show();
        
        NetworkManager.getInstance().deleteRecipe(recipe.getId(), new NetworkManager.DeleteRecipeCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allRecipes.remove(recipe);
                        applyFilters(); // Reapply filters after deletion
                        
                        Toast.makeText(getContext(), "Recette supprimée avec succès", Toast.LENGTH_SHORT).show();
                        
                        if (allRecipes.isEmpty()) {
                            showEmptyState();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erreur lors de la suppression: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    /**
     * Parse time string to minutes
     * Handles formats like "PT30M", "30M", "PT1H30M", "1:30", etc.
     */
    private Integer parseTimeToMinutes(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            String time = timeString.trim().toUpperCase();
            
            // Handle ISO 8601 duration format (PT30M, PT1H30M)
            if (time.startsWith("PT")) {
                int minutes = 0;
                time = time.substring(2); // Remove "PT"
                
                // Extract hours
                if (time.contains("H")) {
                    int hIndex = time.indexOf("H");
                    String hoursStr = time.substring(0, hIndex);
                    minutes += Integer.parseInt(hoursStr) * 60;
                    time = time.substring(hIndex + 1);
                }
                
                // Extract minutes
                if (time.contains("M")) {
                    int mIndex = time.indexOf("M");
                    String minutesStr = time.substring(0, mIndex);
                    if (!minutesStr.isEmpty()) {
                        minutes += Integer.parseInt(minutesStr);
                    }
                }
                
                return minutes;
            }
            
            // Handle simple formats (30, 30M, 1:30)
            if (time.endsWith("M")) {
                time = time.substring(0, time.length() - 1);
            }
            
            // Handle HH:MM format
            if (time.contains(":")) {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return hours * 60 + minutes;
            }
            
            // Handle simple number (assume minutes)
            return Integer.parseInt(time);
            
        } catch (Exception e) {
            // If parsing fails, return null
            return null;
        }
    }
    
    private void syncFavoritesStatus() {
        syncUserDataStatus();
    }
    
    private void syncRatingsStatus() {
        syncUserDataStatus();
    }
    
    private void syncUserDataStatus() {
        fr.didictateur.inanutshell.utils.FavoritesManager favoritesManager = 
            fr.didictateur.inanutshell.utils.FavoritesManager.getInstance(requireContext());
        
        RatingManager ratingManager = RatingManager.getInstance(requireContext());
        
        for (Recipe recipe : allRecipes) {
            // Sync favorite status
            recipe.setFavorite(favoritesManager.isFavorite(recipe.getId()));
            
            // Sync user rating
            float userRating = ratingManager.getRating(recipe.getId());
            recipe.setUserRating(userRating);
        }
    }
    
    // Callback pour les gestures de swipe
    @Override
    public void onRecipeSwipeRight(Recipe recipe, int position) {
        // Swipe à droite = ajouter/retirer des favoris
        boolean wasSelected = recipe.isFavorite();
        recipe.setFavorite(!wasSelected);
        
        if (!wasSelected) {
            favoritesManager.addToFavorites(recipe.getId());
            String message = "Ajouté aux favoris ❤️";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            AccessibilityHelper.announceForAccessibility(binding.recyclerView, 
                recipe.getName() + " ajouté aux favoris");
        } else {
            favoritesManager.removeFromFavorites(recipe.getId());
            String message = "Retiré des favoris 💔";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            AccessibilityHelper.announceForAccessibility(binding.recyclerView,
                recipe.getName() + " retiré des favoris");
        }
        
        // Mettre à jour l'affichage
        adapter.notifyItemChanged(position);
        
        // Synchroniser avec le cache offline si disponible
        if (offlineManager != null) {
            offlineManager.cacheRecipe(recipe);
        }
    }
    
    @Override
    public void onRecipeSwipeLeft(Recipe recipe, int position) {
        // Swipe à gauche = ouvrir la boîte de dialogue de notation
        RatingDialog dialog = new RatingDialog(requireContext(), recipe, (r, rating) -> {
            RatingManager.getInstance(requireContext()).setRating(recipe.getId(), rating);
            recipe.setUserRating(rating);
            adapter.notifyItemChanged(position);
            
            // Synchroniser avec le cache offline si disponible
            if (offlineManager != null) {
                offlineManager.cacheRecipe(recipe);
            }
            
            String message = rating > 0 ? 
                "Note: " + rating + "/5 ⭐" : 
                "Note supprimée";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            AccessibilityHelper.announceForAccessibility(binding.recyclerView,
                recipe.getName() + " " + message);
        });
        dialog.show();
    }
}
