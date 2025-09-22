package fr.didictateur.inanutshell.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.cloud.CloudBackupManager;
import fr.didictateur.inanutshell.adapters.BackupsAdapter;

public class CloudBackupActivity extends AppCompatActivity implements CloudBackupManager.BackupListener {
    
    private CloudBackupManager backupManager;
    
    // UI Components
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView textProgress;
    
    // Configuration
    private TextView textCurrentProvider;
    private TextView textLastBackup;
    private TextView textBackupSize;
    private Switch switchAutoBackup;
    private Switch switchEncryption;
    
    // Actions
    private Button buttonSelectProvider;
    private Button buttonCreateBackup;
    private Button buttonSettings;
    
    // Liste des sauvegardes
    private RecyclerView recyclerBackups;
    private BackupsAdapter backupsAdapter;
    private TextView textNoBackups;
    
    // État
    private boolean operationInProgress = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_backup);
        
        initializeComponents();
        setupBackupManager();
        setupUI();
        loadBackups();
    }
    
    private void initializeComponents() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        textProgress = findViewById(R.id.text_progress);
        
        textCurrentProvider = findViewById(R.id.text_current_provider);
        textLastBackup = findViewById(R.id.text_last_backup);
        textBackupSize = findViewById(R.id.text_backup_size);
        switchAutoBackup = findViewById(R.id.switch_auto_backup);
        switchEncryption = findViewById(R.id.switch_encryption);
        
        buttonSelectProvider = findViewById(R.id.button_select_provider);
        buttonCreateBackup = findViewById(R.id.button_create_backup);
        buttonSettings = findViewById(R.id.button_settings);
        
        recyclerBackups = findViewById(R.id.recycler_backups);
        textNoBackups = findViewById(R.id.text_no_backups);
    }
    
    private void setupBackupManager() {
        backupManager = CloudBackupManager.getInstance(this);
    }
    
    private void setupUI() {
        // Configuration du pull-to-refresh
        swipeRefresh.setOnRefreshListener(this::loadBackups);
        
        // Configuration des switches
        switchAutoBackup.setChecked(backupManager.isAutoBackupEnabled());
        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupManager.setAutoBackupEnabled(isChecked);
        });
        
        switchEncryption.setChecked(backupManager.isEncryptionEnabled());
        switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                showEncryptionDisableWarning();
            } else {
                backupManager.setEncryptionEnabled(true);
            }
        });
        
        // Configuration des boutons
        buttonSelectProvider.setOnClickListener(v -> showProviderSelection());
        buttonCreateBackup.setOnClickListener(v -> showCreateBackupDialog());
        buttonSettings.setOnClickListener(v -> showBackupSettings());
        
        // Configuration du RecyclerView
        backupsAdapter = new BackupsAdapter(this);
        // TODO: Réactiver plus tard
        /*backupsAdapter.setOnBackupActionListener(new BackupsAdapter.OnBackupActionListener() {
            @Override
            public void onRestoreBackup(CloudBackupManager.BackupInfo backup) {
                showRestoreDialog(backup);
            }

            @Override
            public void onDeleteBackup(CloudBackupManager.BackupInfo backup) {
                showDeleteConfirmation(backup);
            }

            @Override
            public void onDownloadBackup(CloudBackupManager.BackupInfo backup) {
                // TODO: Implémenter téléchargement local
            }
        });*/        recyclerBackups.setLayoutManager(new LinearLayoutManager(this));
        recyclerBackups.setAdapter(backupsAdapter);
        
        // Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("☁️ Sauvegarde Cloud");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Mise à jour initiale
        updateUI();
    }
    
    private void updateUI() {
        // Provider actuel
        CloudBackupManager.CloudProvider provider = backupManager.getCurrentProvider();
        textCurrentProvider.setText(provider.getDisplayName());
        
        // Dernière sauvegarde
        if (backupManager.getLastBackupDate() != null) {
            String statusText = String.format("%s - %s", 
                backupManager.getLastBackupStatus().getDisplayName(),
                dateFormat.format(backupManager.getLastBackupDate()));
            textLastBackup.setText(statusText);
            
            String sizeText = Formatter.formatFileSize(this, backupManager.getLastBackupSize());
            textBackupSize.setText(sizeText);
        } else {
            textLastBackup.setText("Aucune sauvegarde");
            textBackupSize.setText("-");
        }
        
        // État des boutons
        boolean hasProvider = provider != CloudBackupManager.CloudProvider.NONE;
        buttonCreateBackup.setEnabled(hasProvider && !operationInProgress);
        
        if (!hasProvider) {
            buttonCreateBackup.setText("Sélectionnez un provider d'abord");
        } else {
            buttonCreateBackup.setText(operationInProgress ? "Sauvegarde en cours..." : "Créer une sauvegarde");
        }
    }
    
    private void loadBackups() {
        if (backupManager.getCurrentProvider() == CloudBackupManager.CloudProvider.NONE) {
            showNoBackups();
            swipeRefresh.setRefreshing(false);
            return;
        }
        
        backupManager.listAvailableBackups(new CloudBackupManager.BackupListListener() {
            @Override
            public void onBackupListReady(List<CloudBackupManager.BackupMetadata> backups) {
                runOnUiThread(() -> {
                    if (backups.isEmpty()) {
                        showNoBackups();
                    } else {
                        showBackups(backups);
                    }
                    swipeRefresh.setRefreshing(false);
                });
            }
            
            @Override
            public void onBackupListFailed(String error) {
                runOnUiThread(() -> {
                    showToast("Erreur lors du chargement: " + error);
                    showNoBackups();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }
    
    private void showBackups(List<CloudBackupManager.BackupMetadata> backups) {
        recyclerBackups.setVisibility(View.VISIBLE);
        textNoBackups.setVisibility(View.GONE);
        // TODO: Convertir BackupMetadata en BackupInfo
        // backupsAdapter.updateBackups(backups);
    }
    
    private void showNoBackups() {
        recyclerBackups.setVisibility(View.GONE);
        textNoBackups.setVisibility(View.VISIBLE);
    }
    
    private void showProviderSelection() {
        String[] providers = {
            "Google Drive",
            "Dropbox", 
            "OneDrive",
            "Aucun"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Sélectionner un service cloud")
            .setItems(providers, (dialog, which) -> {
                CloudBackupManager.CloudProvider selectedProvider;
                
                switch (which) {
                    case 0:
                        selectedProvider = CloudBackupManager.CloudProvider.GOOGLE_DRIVE;
                        break;
                    case 1:
                        selectedProvider = CloudBackupManager.CloudProvider.DROPBOX;
                        break;
                    case 2:
                        selectedProvider = CloudBackupManager.CloudProvider.ONEDRIVE;
                        break;
                    default:
                        selectedProvider = CloudBackupManager.CloudProvider.NONE;
                        break;
                }
                
                if (selectedProvider != CloudBackupManager.CloudProvider.NONE) {
                    authenticateWithProvider(selectedProvider);
                } else {
                    updateUI();
                }
            })
            .show();
    }
    
    private void authenticateWithProvider(CloudBackupManager.CloudProvider provider) {
        showLoading(true, "Authentification en cours...");
        
        backupManager.authenticateWithProvider(provider, new CloudBackupManager.AuthenticationListener() {
            @Override
            public void onAuthenticationSuccess(CloudBackupManager.CloudProvider provider) {
                runOnUiThread(() -> {
                    showLoading(false, null);
                    showToast("Authentifié avec " + provider.getDisplayName());
                    updateUI();
                    loadBackups();
                });
            }
            
            @Override
            public void onAuthenticationFailed(CloudBackupManager.CloudProvider provider, String error) {
                runOnUiThread(() -> {
                    showLoading(false, null);
                    showToast("Échec de l'authentification: " + error);
                });
            }
        });
    }
    
    private void showCreateBackupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_backup, null);
        
        TextView editDescription = dialogView.findViewById(R.id.edit_backup_description);
        
        new AlertDialog.Builder(this)
            .setTitle("Nouvelle sauvegarde")
            .setView(dialogView)
            .setPositiveButton("Créer", (dialog, which) -> {
                String description = editDescription.getText().toString().trim();
                if (description.isEmpty()) {
                    description = "Sauvegarde manuelle";
                }
                createBackup(description);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void createBackup(String description) {
        operationInProgress = true;
        updateUI();
        
        backupManager.createBackup(description, this);
    }
    
    private void showRestoreConfirmation(CloudBackupManager.BackupMetadata metadata) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_restore_backup, null);
        
        TextView textBackupInfo = dialogView.findViewById(R.id.text_backup_info);
        Switch switchMergeData = dialogView.findViewById(R.id.switch_merge_data);
        
        String info = String.format("Sauvegarde du %s\n%d recettes, %d commentaires\n%s", 
            dateFormat.format(metadata.createdAt),
            metadata.recipesCount,
            metadata.commentsCount,
            Formatter.formatFileSize(this, metadata.size));
        
        textBackupInfo.setText(info);
        
        new AlertDialog.Builder(this)
            .setTitle("Restaurer la sauvegarde")
            .setView(dialogView)
            .setMessage("⚠️ Cette action peut écraser vos données actuelles.")
            .setPositiveButton("Restaurer", (dialog, which) -> {
                boolean mergeWithExisting = switchMergeData.isChecked();
                restoreBackup(metadata, mergeWithExisting);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void restoreBackup(CloudBackupManager.BackupMetadata metadata, boolean mergeWithExisting) {
        operationInProgress = true;
        updateUI();
        
        backupManager.restoreBackup(metadata, mergeWithExisting, this);
    }
    
    private void showDeleteConfirmation(CloudBackupManager.BackupMetadata metadata) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer la sauvegarde")
            .setMessage(String.format("Êtes-vous sûr de vouloir supprimer la sauvegarde du %s ?", 
                dateFormat.format(metadata.createdAt)))
            .setPositiveButton("Supprimer", (dialog, which) -> {
                // TODO: Implémenter la suppression
                showToast("Sauvegarde supprimée");
                loadBackups();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void showBackupDetails(CloudBackupManager.BackupMetadata metadata) {
        Intent intent = new Intent(this, BackupDetailsActivity.class);
        intent.putExtra("backup_id", metadata.backupId);
        startActivity(intent);
    }
    
    private void showBackupSettings() {
        Intent intent = new Intent(this, BackupSettingsActivity.class);
        startActivity(intent);
    }
    
    private void showEncryptionDisableWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Désactiver le chiffrement")
            .setMessage("⚠️ Vos futures sauvegardes ne seront plus chiffrées. Êtes-vous sûr ?")
            .setPositiveButton("Désactiver", (dialog, which) -> {
                backupManager.setEncryptionEnabled(false);
            })
            .setNegativeButton("Annuler", (dialog, which) -> {
                switchEncryption.setChecked(true);
            })
            .show();
    }
    
    private void showLoading(boolean show, String message) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        textProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null) {
            textProgress.setText(message);
        }
    }
    
    // Implémentation de BackupListener
    @Override
    public void onBackupStarted() {
        runOnUiThread(() -> {
            showLoading(true, "Préparation de la sauvegarde...");
        });
    }
    
    @Override
    public void onBackupProgress(int progress) {
        runOnUiThread(() -> {
            textProgress.setText(String.format("Sauvegarde en cours... %d%%", progress));
        });
    }
    
    @Override
    public void onBackupCompleted(CloudBackupManager.BackupMetadata metadata) {
        runOnUiThread(() -> {
            operationInProgress = false;
            showLoading(false, null);
            showToast("Sauvegarde terminée avec succès");
            updateUI();
            loadBackups();
        });
    }
    
    @Override
    public void onBackupFailed(String error) {
        runOnUiThread(() -> {
            operationInProgress = false;
            showLoading(false, null);
            showToast("Échec de la sauvegarde: " + error);
            updateUI();
        });
    }
    
    @Override
    public void onRestoreStarted() {
        runOnUiThread(() -> {
            showLoading(true, "Téléchargement de la sauvegarde...");
        });
    }
    
    @Override
    public void onRestoreProgress(int progress) {
        runOnUiThread(() -> {
            textProgress.setText(String.format("Restauration en cours... %d%%", progress));
        });
    }
    
    @Override
    public void onRestoreCompleted(CloudBackupManager.BackupMetadata metadata) {
        runOnUiThread(() -> {
            operationInProgress = false;
            showLoading(false, null);
            showToast("Restauration terminée avec succès");
            updateUI();
            
            // Redémarrer l'app pour recharger les données
            showRestartDialog();
        });
    }
    
    @Override
    public void onRestoreFailed(String error) {
        runOnUiThread(() -> {
            operationInProgress = false;
            showLoading(false, null);
            showToast("Échec de la restauration: " + error);
            updateUI();
        });
    }
    
    private void showRestartDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Restauration terminée")
            .setMessage("L'application doit redémarrer pour charger les données restaurées.")
            .setPositiveButton("Redémarrer", (dialog, which) -> {
                restartApp();
            })
            .setCancelable(false)
            .show();
    }
    
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cloud_backup, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_cleanup) {
            showCleanupDialog();
            return true;
        } else if (id == R.id.action_export_settings) {
            exportBackupSettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showCleanupDialog() {
        String[] options = {"Garder 5 sauvegardes", "Garder 10 sauvegardes", "Garder 20 sauvegardes"};
        int[] keepCounts = {5, 10, 20};
        
        new AlertDialog.Builder(this)
            .setTitle("Nettoyer les anciennes sauvegardes")
            .setItems(options, (dialog, which) -> {
                cleanupOldBackups(keepCounts[which]);
            })
            .show();
    }
    
    private void cleanupOldBackups(int keepCount) {
        backupManager.cleanOldBackups(keepCount, new CloudBackupManager.CleanupListener() {
            @Override
            public void onCleanupCompleted(int deletedCount) {
                runOnUiThread(() -> {
                    showToast(String.format("%d anciennes sauvegardes supprimées", deletedCount));
                    loadBackups();
                });
            }
            
            @Override
            public void onCleanupFailed(String error) {
                runOnUiThread(() -> {
                    showToast("Erreur lors du nettoyage: " + error);
                });
            }
        });
    }
    
    private void exportBackupSettings() {
        // TODO: Exporter les paramètres de sauvegarde
        showToast("Paramètres exportés");
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
