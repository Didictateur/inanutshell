package fr.didictateur.inanutshell.ui.sync;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.sync.SyncManager;
import fr.didictateur.inanutshell.sync.model.SyncStatus;

/**
 * Activité de gestion de la synchronisation
 */
public class SyncActivity extends AppCompatActivity {
    
    private SyncViewModel viewModel;
    private SyncManager syncManager;
    
    // Vues principales
    private TextView statusText;
    private ProgressBar syncProgress;
    private Button syncButton;
    private Switch autoSyncSwitch;
    private TextView lastSyncText;
    private TextView pendingCountText;
    
    // RecyclerView pour les conflits et éléments pending
    private RecyclerView conflictsRecyclerView;
    private RecyclerView pendingItemsRecyclerView;
    
    // Adapteurs
    private ConflictsAdapter conflictsAdapter;
    private PendingItemsAdapter pendingItemsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        
        setupToolbar();
        initializeViews();
        initializeViewModels();
        setupObservers();
        setupClickListeners();
        
        // Add smooth animations for sync interface
        animateSyncUIEntrance();
    }
    
    private void animateSyncUIEntrance() {
        // Animate status elements
        fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(statusText);
        
        // Animate progress bar
        if (syncProgress != null) {
            fr.didictateur.inanutshell.utils.AnimationHelper.animateViewEntrance(syncProgress);
        }
        
        // Animate buttons with stagger
        findViewById(R.id.btn_sync_now).setAlpha(0f);
        findViewById(R.id.btn_sync_now).animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(200)
            .start();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Synchronisation");
        }
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.tv_sync_status);
        syncProgress = findViewById(R.id.progress_sync);
        syncButton = findViewById(R.id.btn_sync_now);
        autoSyncSwitch = findViewById(R.id.switch_auto_sync);
        lastSyncText = findViewById(R.id.tv_last_sync);
        pendingCountText = findViewById(R.id.tv_pending_count);
        
        conflictsRecyclerView = findViewById(R.id.rv_conflicts);
        pendingItemsRecyclerView = findViewById(R.id.rv_pending_items);
        
        // Configuration des RecyclerViews
        conflictsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pendingItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialement masquer la progress bar
        syncProgress.setVisibility(View.GONE);
    }
    
    private void initializeViewModels() {
        syncManager = SyncManager.getInstance(this);
        viewModel = new ViewModelProvider(this).get(SyncViewModel.class);
        
        // Initialiser les adapteurs
        conflictsAdapter = new ConflictsAdapter(conflict -> {
            // Ouvrir l'activité de résolution de conflit
            Intent intent = new Intent(this, ConflictResolutionActivity.class);
            intent.putExtra("conflict_id", conflict.getId());
            startActivity(intent);
        });
        
        pendingItemsAdapter = new PendingItemsAdapter(item -> {
            // Actions sur les éléments pending (retry, delete, etc.)
            showPendingItemActions(item);
        });
        
        conflictsRecyclerView.setAdapter(conflictsAdapter);
        pendingItemsRecyclerView.setAdapter(pendingItemsAdapter);
    }
    
    private void setupObservers() {
        // Observer le statut de synchronisation
        syncManager.getSyncStatus().observe(this, this::updateSyncStatus);
        
        // Observer les éléments pending
        syncManager.getPendingItems().observe(this, items -> {
            pendingItemsAdapter.updateItems(items);
            updatePendingCount(items.size());
        });
        
        // Observer les conflits via le ViewModel
        viewModel.getConflicts().observe(this, conflicts -> {
            conflictsAdapter.updateConflicts(conflicts);
            updateConflictsVisibility(conflicts.size());
        });
        
        // Observer les erreurs
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
    }
    
    private void setupClickListeners() {
        syncButton.setOnClickListener(v -> {
            syncManager.startFullSync();
        });
        
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            syncManager.setSyncEnabled(isChecked);
            updateAutoSyncStatus(isChecked);
        });
        
        // Bouton pour nettoyer les éléments terminés
        findViewById(R.id.btn_clear_completed).setOnClickListener(v -> {
            viewModel.clearCompletedItems();
        });
        
        // Bouton pour forcer la synchronisation des éléments pending
        findViewById(R.id.btn_retry_pending).setOnClickListener(v -> {
            viewModel.retryPendingItems();
        });
    }
    
    private void updateSyncStatus(SyncStatus status) {
        if (status == null) return;
        
        // Mettre à jour le texte de statut
        String statusMessage = getStatusMessage(status);
        statusText.setText(statusMessage);
        
        // Mettre à jour la barre de progression
        if (status.isInProgress()) {
            syncProgress.setVisibility(View.VISIBLE);
            syncProgress.setIndeterminate(status.getTotalItems() == 0);
            if (status.getTotalItems() > 0) {
                syncProgress.setProgress(status.getProgress());
            }
            syncButton.setEnabled(false);
            syncButton.setText("Synchronisation...");
        } else {
            syncProgress.setVisibility(View.GONE);
            syncButton.setEnabled(true);
            syncButton.setText("Synchroniser maintenant");
        }
        
        // Mettre à jour la dernière synchronisation
        if (status.getLastSync() > 0) {
            String lastSync = "Dernière sync: " + formatTimestamp(status.getLastSync());
            lastSyncText.setText(lastSync);
        } else {
            lastSyncText.setText("Jamais synchronisé");
        }
        
        // Gestion des erreurs
        if (status.hasError()) {
            showError(status.getMessage());
        }
        
        // Gestion des conflits
        if (status.hasConflicts()) {
            showConflictsDetected(status.getMessage());
        }
    }
    
    private String getStatusMessage(SyncStatus status) {
        switch (status.getState()) {
            case IDLE:
                return "Prêt à synchroniser";
            case SYNCING:
                return status.getMessage() != null ? status.getMessage() : "Synchronisation en cours...";
            case COMPLETED:
                return "Synchronisation terminée avec succès";
            case ERROR:
                return "Erreur: " + (status.getMessage() != null ? status.getMessage() : "Inconnue");
            case CONFLICTS:
                return "Conflits détectés: " + status.getMessage();
            case DISABLED:
                return "Synchronisation désactivée";
            default:
                return "Statut inconnu";
        }
    }
    
    private void updatePendingCount(int count) {
        String text = count == 0 ? "Aucun élément en attente" : 
                     count == 1 ? "1 élément en attente" :
                     count + " éléments en attente";
        pendingCountText.setText(text);
        
        // Afficher/masquer la section pending
        View pendingSection = findViewById(R.id.section_pending);
        pendingSection.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }
    
    private void updateConflictsVisibility(int conflictCount) {
        View conflictsSection = findViewById(R.id.section_conflicts);
        TextView conflictsTitle = findViewById(R.id.tv_conflicts_title);
        
        if (conflictCount > 0) {
            conflictsSection.setVisibility(View.VISIBLE);
            String title = conflictCount == 1 ? "1 conflit à résoudre" : 
                          conflictCount + " conflits à résoudre";
            conflictsTitle.setText(title);
        } else {
            conflictsSection.setVisibility(View.GONE);
        }
    }
    
    private void updateAutoSyncStatus(boolean enabled) {
        TextView autoSyncStatus = findViewById(R.id.tv_auto_sync_status);
        if (enabled) {
            autoSyncStatus.setText("Synchronisation automatique activée");
            autoSyncStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            autoSyncStatus.setText("Synchronisation automatique désactivée");
            autoSyncStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }
    
    private void showPendingItemActions(Object item) {
        // TODO: Implémenter le dialog d'actions pour les éléments pending
        Toast.makeText(this, "Actions pour élément pending", Toast.LENGTH_SHORT).show();
    }
    
    private void showError(String error) {
        Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_LONG).show();
    }
    
    private void showConflictsDetected(String message) {
        Toast.makeText(this, "Conflits détectés: " + message, Toast.LENGTH_LONG).show();
    }
    
    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", 
                                                                        java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les données
        viewModel.refreshData();
    }
}
