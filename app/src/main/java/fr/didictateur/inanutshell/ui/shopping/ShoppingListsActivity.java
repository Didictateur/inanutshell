package fr.didictateur.inanutshell.ui.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.shopping.ShoppingList;
import fr.didictateur.inanutshell.data.shopping.ShoppingManager;

import java.util.List;

/**
 * Activité principale pour la gestion des listes de courses
 */
public class ShoppingListsActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ShoppingListsAdapter adapter;
    private ShoppingManager shoppingManager;
    private FloatingActionButton fabCreateList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_lists);
        
        initViews();
        initShoppingManager();
        setupRecyclerView();
        observeShoppingLists();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewShoppingLists);
        fabCreateList = findViewById(R.id.fabCreateList);
        
        fabCreateList.setOnClickListener(v -> showCreateListOptions());
        
        // Configuration de la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Listes de courses");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initShoppingManager() {
        shoppingManager = new ShoppingManager(this);
    }
    
    private void setupRecyclerView() {
        adapter = new ShoppingListsAdapter(this, new ShoppingListsAdapter.ShoppingListListener() {
            @Override
            public void onListClicked(ShoppingList shoppingList) {
                openShoppingList(shoppingList);
            }
            
            @Override
            public void onDeleteClicked(ShoppingList shoppingList) {
                deleteShoppingList(shoppingList);
            }
            
            @Override
            public void onToggleCompletedClicked(ShoppingList shoppingList) {
                toggleListCompletion(shoppingList);
            }
            
            @Override
            public void onShareClicked(ShoppingList shoppingList) {
                shareShoppingList(shoppingList);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void observeShoppingLists() {
        shoppingManager.getAllLists().observe(this, new Observer<List<ShoppingList>>() {
            @Override
            public void onChanged(List<ShoppingList> shoppingLists) {
                adapter.updateLists(shoppingLists);
            }
        });
    }
    
    private void showCreateListOptions() {
        // TODO: Implémenter un dialog avec les options :
        // - Liste manuelle
        // - À partir d'une recette
        // - À partir du planning des repas
        createManualList();
    }
    
    private void createManualList() {
        // TODO: Intent vers EditShoppingListActivity (à créer)
        // Intent intent = new Intent(this, EditShoppingListActivity.class);
        // startActivity(intent);
        Toast.makeText(this, "Création de liste manuelle à implémenter", Toast.LENGTH_SHORT).show();
    }
    
    private void openShoppingList(ShoppingList shoppingList) {
        // TODO: Intent vers ShoppingItemsActivity (à créer)
        // Intent intent = new Intent(this, ShoppingItemsActivity.class);
        // intent.putExtra("LIST_ID", shoppingList.id);
        // intent.putExtra("LIST_NAME", shoppingList.name);
        // startActivity(intent);
        Toast.makeText(this, "Ouverture de liste: " + shoppingList.name, Toast.LENGTH_SHORT).show();
    }
    
    private void deleteShoppingList(ShoppingList shoppingList) {
        shoppingManager.deleteShoppingList(shoppingList.id, new ShoppingManager.ShoppingCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(ShoppingListsActivity.this, 
                        "Liste supprimée", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(ShoppingListsActivity.this, 
                        "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void toggleListCompletion(ShoppingList shoppingList) {
        if (shoppingList.isCompleted) {
            shoppingManager.reopenShoppingList(shoppingList.id, new ShoppingManager.ShoppingCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShoppingListsActivity.this, 
                            "Liste réactivée", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShoppingListsActivity.this, 
                            "Erreur", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            shoppingManager.completeShoppingList(shoppingList.id, new ShoppingManager.ShoppingCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShoppingListsActivity.this, 
                            "Liste terminée", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShoppingListsActivity.this, 
                            "Erreur", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
    
    private void shareShoppingList(ShoppingList shoppingList) {
        // TODO: Implémenter le partage de la liste
        Toast.makeText(this, "Partage à implémenter", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shopping_lists, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_settings) {
            // TODO: Ouvrir les paramètres des listes de courses
            Toast.makeText(this, "Paramètres à implémenter", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_clear_completed) {
            // TODO: Supprimer toutes les listes terminées
            Toast.makeText(this, "Suppression des listes terminées à implémenter", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shoppingManager != null) {
            shoppingManager.shutdown();
        }
    }
}
