package fr.didictateur.inanutshell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.content.Intent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private ArrayList<Item> items;
    private RecetteAdapter adapter;
    private Long currentFolderId = null;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private AppDatabase db;
    
    // Vues pour les onglets
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private LinearLayout tabRecipes, tabPlanner;
    private ImageView iconRecipes, iconPlanner;
    private TextView labelRecipes, labelPlanner;
    private int currentTab = 0; // 0 = recettes, 1 = planificateur
    
    // Variables pour le planificateur de repas
    private TextView currentWeekText;
    private RecyclerView weekRecyclerView;
    private MealPlanWeekAdapter weekAdapter;
    private Calendar currentWeekStart;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;

    private final ActivityResultLauncher<Intent> addRecipeLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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
                        currentFolderId
                    );
                    new Thread(() -> {
                        db.recetteDao().insert(recette);
                        runOnUiThread(() -> showFolder(currentFolderId));
                    }).start();
                }
            }
        );

    private final ActivityResultLauncher<Intent> editRecipeLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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
                                db.recetteDao().update(recette);
                                runOnUiThread(() -> showFolder(currentFolderId));
                            }
                        }).start();
                    }
                }
            }
        );

    private final ActivityResultLauncher<Intent> settingsLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                android.util.Log.d("MainActivity", "Settings result received: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    android.util.Log.d("MainActivity", "Result OK, calling updateToolbarColor()");
                    updateToolbarColor();
                } else {
                    android.util.Log.d("MainActivity", "Result not OK: " + result.getResultCode());
                }
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateToolbarColor();

        // Configuration des vues
        setupViews();
        
        // Configuration des onglets
        setupBottomNavigation();
        
        // Initialiser avec l'onglet des recettes
        viewPager.setCurrentItem(0, false);

        findViewById(R.id.searchButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferenceChangeListener = (sharedPreferences, key) -> {
            if ("toolbar_color".equals(key)) {
                updateTabColors();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Bouton de recherche
        findViewById(R.id.searchButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });

        // Le FAB est maintenant géré dans les fragments
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    // Affiche le contenu du dossier courant (dossiers + recettes)
    public void showFolder(Long folderId) {
        new Thread(() -> {
            List<Folder> folders = db.folderDao().getFoldersByParent(folderId);
            List<Recette> recettes = db.recetteDao().getRecettesByParent(folderId);

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

            runOnUiThread(() -> {
                items.clear();
                items.addAll(folders);
                items.addAll(recettes);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    public Long getCurrentFolderId() {
        return currentFolderId;
    }

    private void showFolderContextMenu(View anchor, Folder folder) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Renommer");
        popup.getMenu().add("Déplacer");
        popup.getMenu().add("Supprimer");
        popup.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if (title.equals("Renommer")) {
                showRenameFolderDialog(folder);
            } else if (title.equals("Déplacer")) {
                showMoveFolderDialog(folder);
            } else if (title.equals("Supprimer")) {
                confirmDeleteFolder(folder);
            }
            return true;
        });
        popup.show();
    }

    private void showRecetteContextMenu(View anchor, Recette recette) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Modifier");
        popup.getMenu().add("Déplacer");
        popup.getMenu().add("Supprimer");
        popup.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if (title.equals("Modifier")) {
                Intent intent = new Intent(this, EditRecetteActivity.class);
                intent.putExtra("recetteId", recette.id);
                editRecipeLauncher.launch(intent);
                return true;
            } else if (title.equals("Déplacer")) {
                showMoveRecetteDialog(recette);
                return true;
            } else if (title.equals("Supprimer")) {
                confirmDeleteRecette(recette);
                return true;
            }
            return false;
        });
        popup.show();
    }
    private void updateToolbarColor() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        android.util.Log.d("MainActivity", "updateToolbarColor: colorName = " + colorName);
        
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        android.util.Log.d("MainActivity", "updateToolbarColor: colorResId = " + colorResId);
        
        if (colorResId == 0) {
            android.util.Log.e("MainActivity", "Color resource not found: " + colorName);
            return;
        }
        
        int toolbarColor = ContextCompat.getColor(this, colorResId);
        android.util.Log.d("MainActivity", "updateToolbarColor: toolbarColor = " + Integer.toHexString(toolbarColor));

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(toolbarColor);
            android.util.Log.d("MainActivity", "Toolbar color updated successfully");
        } else {
            android.util.Log.e("MainActivity", "Toolbar not found!");
        }

        String statusBarColorName = colorName.replace("toolbar_", "statusbar_");
        android.util.Log.d("MainActivity", "updateToolbarColor: statusBarColorName = " + statusBarColorName);
        
        int statusBarColorResId = getResources().getIdentifier(
            statusBarColorName,
            "color",
            getPackageName()
        );
        android.util.Log.d("MainActivity", "updateToolbarColor: statusBarColorResId = " + statusBarColorResId);
        
        if (statusBarColorResId == 0) {
            android.util.Log.e("MainActivity", "Status bar color resource not found: " + statusBarColorName);
            return;
        }
        
        int statusBarColor = ContextCompat.getColor(this, statusBarColorResId);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(statusBarColor);
            android.util.Log.d("MainActivity", "Status bar color updated successfully");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            settingsLauncher.launch(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Déléguer la gestion du retour arrière au fragment actif
        if (viewPager.getCurrentItem() == 0) {
            // Fragment Recettes - vérifier s'il peut gérer le retour arrière
            RecipesFragment recipesFragment = (RecipesFragment) pagerAdapter.getFragment(0);
            if (recipesFragment != null && recipesFragment.onBackPressed()) {
                // Le fragment a géré le retour arrière
                return;
            }
        }
        // Sinon, comportement par défaut (fermer l'app)
        super.onBackPressed();
    }

		public void showMoveRecetteDialog(Recette recette) {
		}

		public void confirmDeleteRecette(Recette recette) {
    		new androidx.appcompat.app.AlertDialog.Builder(this)
        		.setTitle("Supprimer la recette")
        		.setMessage("Voulez-vous vraiment supprimer cette recette ?")
        		.setPositiveButton("Supprimer", (dialog, which) -> {
            		new Thread(() -> {
                		db.recetteDao().delete(recette);
                		runOnUiThread(() -> showFolder(currentFolderId));
            		}).start();
        		})
        		.setNegativeButton("Annuler", null)
        		.show();
	}

	private void showRenameFolderDialog(Folder folder) {
		EditText editText = new EditText(this);
		editText.setText(folder.name);
		
		new androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle("Renommer le dossier")
			.setView(editText)
			.setPositiveButton("Renommer", (dialog, which) -> {
				String newName = editText.getText().toString().trim();
				if (!newName.isEmpty()) {
					new Thread(() -> {
						folder.name = newName;
						db.folderDao().update(folder);
						runOnUiThread(() -> showFolder(currentFolderId));
					}).start();
				}
			})
			.setNegativeButton("Annuler", null)
			.show();
	}

	private void showMoveFolderDialog(Folder folder) {
		// Pour l'instant, juste un message - on pourrait implémenter une vraie fonctionnalité plus tard
		new androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle("Déplacer le dossier")
			.setMessage("Fonctionnalité à implémenter")
			.setPositiveButton("OK", null)
			.show();
	}

	private void confirmDeleteFolder(Folder folder) {
		new androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle("Supprimer le dossier")
			.setMessage("Voulez-vous vraiment supprimer ce dossier et tout son contenu ?")
			.setPositiveButton("Supprimer", (dialog, which) -> {
				new Thread(() -> {
					// Supprimer d'abord toutes les recettes du dossier
					List<Recette> recettes = db.recetteDao().getRecettesByParent(folder.getId());
					for (Recette recette : recettes) {
						db.recetteDao().delete(recette);
					}
					// Puis supprimer le dossier lui-même
					db.folderDao().delete(folder);
					runOnUiThread(() -> showFolder(currentFolderId));
				}).start();
			})
			.setNegativeButton("Annuler", null)
			.show();
    }
    
    private void setupViews() {
        // Configuration du ViewPager2 et des fragments
        viewPager = findViewById(R.id.viewPager);
        
        // Les vues spécifiques sont maintenant gérées dans leurs fragments respectifs
    }
    
    private void setupBottomNavigation() {
        tabRecipes = findViewById(R.id.tabRecipes);
        tabPlanner = findViewById(R.id.tabPlanner);
        iconRecipes = findViewById(R.id.iconRecipes);
        iconPlanner = findViewById(R.id.iconPlanner);
        labelRecipes = findViewById(R.id.labelRecipes);
        labelPlanner = findViewById(R.id.labelPlanner);
        
        // Configuration du ViewPager2
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Listener pour synchroniser les tabs avec le ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentTab = position;
                updateTabSelection();
            }
        });
        
        tabRecipes.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        tabPlanner.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        
        updateTabColors();
    }
    
    // Supprimer les anciennes méthodes showRecipesTab et showPlannerTab
    // car maintenant on utilise ViewPager2 pour la navigation

    private void initializePlanner() {
        // Initialiser les vues du planificateur
        currentWeekText = findViewById(R.id.current_week_text);
        weekRecyclerView = findViewById(R.id.week_recycler_view);
        Button previousWeekBtn = findViewById(R.id.previous_week_btn);
        Button nextWeekBtn = findViewById(R.id.next_week_btn);
        
        // Initialiser les formats de date
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        // Configuration du calendrier
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);
        
        // Configuration du RecyclerView
        weekRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        weekAdapter = new MealPlanWeekAdapter(new ArrayList<>(), this::onMealPlanClick, this::onAddMealClick, this::onDeleteMealClick);
        weekRecyclerView.setAdapter(weekAdapter);
        
        // Appliquer les couleurs du thème
        applyThemeColorsToButtons(previousWeekBtn, nextWeekBtn);
        
        // Listeners pour les boutons
        previousWeekBtn.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            loadCurrentWeek();
        });
        
        nextWeekBtn.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            loadCurrentWeek();
        });
        
        // Affichage initial
        loadCurrentWeek();
    }
    
    private void loadCurrentWeek() {
        // Calculer la fin de la semaine
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        String startDate = dateFormat.format(currentWeekStart.getTime());
        String endDate = dateFormat.format(weekEnd.getTime());
        
        // Mettre à jour l'affichage de la semaine
        String weekDisplay = displayDateFormat.format(currentWeekStart.getTime()) + 
                           " - " + displayDateFormat.format(weekEnd.getTime());
        currentWeekText.setText(weekDisplay);
        
        // Charger les repas de la semaine
        new Thread(() -> {
            List<MealPlanWithRecette> mealPlans = db.mealPlanDao().getMealPlansWithRecetteForWeek(startDate, endDate);
            
            // Organiser les repas par jour
            List<DayMealPlan> dayMealPlans = organizeMealsByDay(mealPlans);
            
            runOnUiThread(() -> {
                weekAdapter.updateMealPlans(dayMealPlans);
            });
        }).start();
    }
    
    private List<DayMealPlan> organizeMealsByDay(List<MealPlanWithRecette> mealPlans) {
        List<DayMealPlan> dayMealPlans = new ArrayList<>();
        
        // Créer 7 jours de la semaine
        Calendar dayCalendar = (Calendar) currentWeekStart.clone();
        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(dayCalendar.getTime());
            String dayName = new SimpleDateFormat("EEEE", Locale.getDefault()).format(dayCalendar.getTime());
            
            DayMealPlan dayMealPlan = new DayMealPlan(date, dayName);
            
            // Ajouter les repas pour ce jour
            for (MealPlanWithRecette mealPlan : mealPlans) {
                if (mealPlan.getMealPlan().getDate().equals(date)) {
                    dayMealPlan.addMeal(mealPlan);
                }
            }
            
            dayMealPlans.add(dayMealPlan);
            dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        return dayMealPlans;
    }
    
    private void onMealPlanClick(MealPlanWithRecette mealPlan) {
        // Ouvrir la recette associée
        if (mealPlan.getMealPlan().getRecetteId() != null) {
            new Thread(() -> {
                Recette recette = db.recetteDao().getRecetteById(mealPlan.getMealPlan().getRecetteId());
                runOnUiThread(() -> {
                    if (recette != null) {
                        Intent intent = new Intent(this, ViewRecetteActivity.class);
                        intent.putExtra("recette_id", recette.id);
                        startActivity(intent);
                    }
                });
            }).start();
        }
    }
    
    private void onAddMealClick(String date, String mealType) {
        showSelectRecipeDialog(date, mealType);
    }
    
    private void showSelectRecipeDialog(String date, String mealType) {
        // Charger toutes les recettes
        new Thread(() -> {
            List<Recette> recettes = db.recetteDao().getAllRecettes();
            
            runOnUiThread(() -> {
                if (recettes.isEmpty()) {
                    Toast.makeText(this, "Aucune recette disponible. Créez d'abord des recettes.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Créer un dialog avec spinner pour sélectionner la recette
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("Sélectionner une recette");
                
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_recipe, null);
                Spinner recipeSpinner = dialogView.findViewById(R.id.recipeSpinner);
                EditText portionsEditText = dialogView.findViewById(R.id.portionsEditText);
                ImageButton decreaseButton = dialogView.findViewById(R.id.decreasePortionsButton);
                ImageButton increaseButton = dialogView.findViewById(R.id.increasePortionsButton);
                
                // Configuration des boutons de portions
                decreaseButton.setOnClickListener(v -> {
                    try {
                        double currentValue = Double.parseDouble(portionsEditText.getText().toString());
                        double newValue = Math.max(0.5, currentValue - 0.5);
                        portionsEditText.setText(formatPortions(newValue));
                    } catch (NumberFormatException e) {
                        portionsEditText.setText("1");
                    }
                });
                
                increaseButton.setOnClickListener(v -> {
                    try {
                        double currentValue = Double.parseDouble(portionsEditText.getText().toString());
                        double newValue = Math.min(20, currentValue + 0.5);
                        portionsEditText.setText(formatPortions(newValue));
                    } catch (NumberFormatException e) {
                        portionsEditText.setText("1");
                    }
                });
                
                // Adapter pour le spinner
                List<String> recipeNames = new ArrayList<>();
                for (Recette recette : recettes) {
                    recipeNames.add(recette.titre);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recipeNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                recipeSpinner.setAdapter(adapter);
                
                builder.setView(dialogView);
                builder.setPositiveButton("Ajouter", (dialog, which) -> {
                    int selectedIndex = recipeSpinner.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < recettes.size()) {
                        Recette selectedRecette = recettes.get(selectedIndex);
                        String portions = portionsEditText.getText().toString();
                        addMealPlan(date, mealType, selectedRecette.id, portions);
                    }
                });
                
                builder.setNegativeButton("Annuler", null);
                builder.show();
            });
        }).start();
    }
    
    private void addMealPlan(String date, String mealType, Long recetteId, String portions) {
        new Thread(() -> {
            // Ajouter le nouveau repas avec les portions spécifiées
            MealPlan newMealPlan = new MealPlan(date, mealType, recetteId, null, portions);
            db.mealPlanDao().insert(newMealPlan);
            runOnUiThread(() -> {
                Toast.makeText(this, "Repas ajouté au planning !", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private String formatPortions(double portions) {
        if (portions == Math.floor(portions)) {
            return String.valueOf((int) portions);
        } else {
            return String.format("%.1f", portions).replace(".0", "");
        }
    }
    
    private void onDeleteMealClick(MealPlanWithRecette mealPlan) {
        // Afficher une confirmation avant de supprimer
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Supprimer le repas");
        builder.setMessage("Voulez-vous vraiment supprimer \"" + mealPlan.getRecetteTitre() + "\" ?");
        builder.setPositiveButton("Supprimer", (dialog, which) -> {
            // Supprimer de la base de données
            new Thread(() -> {
                db.mealPlanDao().delete(mealPlan.getMealPlan());
                runOnUiThread(() -> {
                    loadCurrentWeek();
                    Toast.makeText(this, "Repas supprimé", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }
    
    private void applyThemeColorsToButtons(Button... buttons) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        if (colorResId != 0) {
            int themeColor = ContextCompat.getColor(this, colorResId);
            for (Button button : buttons) {
                button.setTextColor(themeColor);
            }
        }
}
    
    private void updateTabSelection() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorName = prefs.getString("toolbar_color", "toolbar_bg_brown");
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        int selectedColor = ContextCompat.getColor(this, colorResId);
        int unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        
        if (currentTab == 0) {
            // Onglet Recettes sélectionné
            iconRecipes.setColorFilter(selectedColor);
            labelRecipes.setTextColor(selectedColor);
            iconPlanner.setColorFilter(unselectedColor);
            labelPlanner.setTextColor(unselectedColor);
        } else {
            // Onglet Planificateur sélectionné
            iconRecipes.setColorFilter(unselectedColor);
            labelRecipes.setTextColor(unselectedColor);
            iconPlanner.setColorFilter(selectedColor);
            labelPlanner.setTextColor(selectedColor);
        }
    }
    
    private void updateTabColors() {
        updateTabSelection();
    }
}

