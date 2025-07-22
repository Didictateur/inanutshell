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
import android.widget.PopupMenu;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
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
                if (result.getResultCode() == RESULT_OK) {
                    updateToolbarColor();
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

        RecyclerView recyclerView = findViewById(R.id.recipesRecyclerView);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        items = new ArrayList<>();
        adapter = new RecetteAdapter(items, this);

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItemViewType(position) == RecetteAdapter.TYPE_FOLDER ? 1 : 3;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setOnFolderClickListener(folder -> {
            currentFolderId = folder.getId();
            showFolder(currentFolderId);
        });

        adapter.setOnFolderActionListener((anchor, folder) ->
            showFolderContextMenu(anchor, folder));

        adapter.setOnRecetteActionListener((anchor, recette) ->
            showRecetteContextMenu(anchor, recette));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferenceChangeListener = (sharedPreferences, key) -> {
            if ("toolbar_color".equals(key)) {
                adapter.notifyDataSetChanged();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Bouton de recherche
        findViewById(R.id.searchButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });

        // + button
        FloatingActionButton fab = findViewById(R.id.addRecipeButton);
        fab.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.fab_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                // new recipe
                if (id == R.id.action_new_recipe) {
                    Intent intent = new Intent(this, EditRecetteActivity.class);
                    addRecipeLauncher.launch(intent);
                    return true;
                // new folder
                } else if (id == R.id.action_new_folder) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Nom du dossier");
                    final EditText input = new EditText(this);
                    builder.setView(input);
                    builder.setPositiveButton("Créer", (dialog, which) -> {
                        String folderName = input.getText().toString();
                        Folder folder = new Folder(0, folderName, currentFolderId);
                        new Thread(() -> {
                            db.folderDao().insert(folder);
                            runOnUiThread(() -> showFolder(currentFolderId));
                        }).start();
                    });
                    builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());
                    builder.show();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        showFolder(currentFolderId);
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
        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
        int toolbarColor = ContextCompat.getColor(this, colorResId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);

        String statusBarColorName = colorName.replace("toolbar_", "statusbar_");
        int statusBarColorResId = getResources().getIdentifier(
            statusBarColorName,
            "color",
            getPackageName()
        );
        int statusBarColor = ContextCompat.getColor(this, statusBarColorResId);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(statusBarColor);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            settingsLauncher.launch(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (currentFolderId != null) {
            new Thread(() -> {
                Folder current = db.folderDao().getFolderById(currentFolderId);
                runOnUiThread(() -> {
                    if (current != null) {
                        currentFolderId = current.getParentId();
                        showFolder(currentFolderId);
                    } else {
                        super.onBackPressed();
                    }
                });
            }).start();
        } else {
            super.onBackPressed();
        }
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
		
		new AlertDialog.Builder(this)
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
		new AlertDialog.Builder(this)
			.setTitle("Déplacer le dossier")
			.setMessage("Fonctionnalité à implémenter")
			.setPositiveButton("OK", null)
			.show();
	}

	private void confirmDeleteFolder(Folder folder) {
		new AlertDialog.Builder(this)
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
}

