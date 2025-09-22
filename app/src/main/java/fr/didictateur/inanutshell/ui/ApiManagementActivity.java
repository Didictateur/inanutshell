package fr.didictateur.inanutshell.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.api.ApiServer;
import fr.didictateur.inanutshell.api.ApiClientsAdapter;

public class ApiManagementActivity extends AppCompatActivity {
    
    private ApiServer apiServer;
    
    // UI Components
    private Switch switchServerEnabled;
    private TextView textServerStatus;
    private TextView textServerUrl;
    private EditText editServerPort;
    private Button buttonApplyPort;
    private Button buttonViewDocs;
    private Button buttonAddClient;
    private RecyclerView recyclerClients;
    private TextView textActiveTokens;
    
    private ApiClientsAdapter clientsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_management);
        
        initializeComponents();
        setupApiServer();
        setupUI();
        updateServerStatus();
    }
    
    private void initializeComponents() {
        switchServerEnabled = findViewById(R.id.switch_server_enabled);
        textServerStatus = findViewById(R.id.text_server_status);
        textServerUrl = findViewById(R.id.text_server_url);
        editServerPort = findViewById(R.id.edit_server_port);
        buttonApplyPort = findViewById(R.id.button_apply_port);
        buttonViewDocs = findViewById(R.id.button_view_docs);
        buttonAddClient = findViewById(R.id.button_add_client);
        recyclerClients = findViewById(R.id.recycler_clients);
        textActiveTokens = findViewById(R.id.text_active_tokens);
    }
    
    private void setupApiServer() {
        apiServer = ApiServer.getInstance(this);
    }
    
    private void setupUI() {
        // Configuration du switch serveur
        switchServerEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startApiServer();
            } else {
                stopApiServer();
            }
        });
        
        // Configuration du port
        editServerPort.setText(String.valueOf(apiServer.getServerPort()));
        buttonApplyPort.setOnClickListener(v -> applyPortChange());
        
        // Bouton documentation
        buttonViewDocs.setOnClickListener(v -> showApiDocumentation());
        
        // Bouton ajouter client
        buttonAddClient.setOnClickListener(v -> showAddClientDialog());
        
        // Configuration du RecyclerView pour les clients
        clientsAdapter = new ApiClientsAdapter(this, apiServer.getRegisteredClients());
        recyclerClients.setLayoutManager(new LinearLayoutManager(this));
        recyclerClients.setAdapter(clientsAdapter);
        
        // Rafraîchir l'affichage
        refreshClientsList();
        updateTokensCount();
    }
    
    private void startApiServer() {
        try {
            apiServer.startServer();
            updateServerStatus();
            showToast("Serveur API démarré");
        } catch (Exception e) {
            switchServerEnabled.setChecked(false);
            showToast("Erreur lors du démarrage du serveur: " + e.getMessage());
        }
    }
    
    private void stopApiServer() {
        try {
            apiServer.stopServer();
            updateServerStatus();
            showToast("Serveur API arrêté");
        } catch (Exception e) {
            switchServerEnabled.setChecked(true);
            showToast("Erreur lors de l'arrêt du serveur: " + e.getMessage());
        }
    }
    
    private void applyPortChange() {
        try {
            String portText = editServerPort.getText().toString();
            int port = Integer.parseInt(portText);
            
            if (port < 1024 || port > 65535) {
                showToast("Le port doit être entre 1024 et 65535");
                return;
            }
            
            boolean wasRunning = apiServer.isServerRunning();
            
            if (wasRunning) {
                apiServer.stopServer();
            }
            
            apiServer.setServerPort(port);
            
            if (wasRunning) {
                apiServer.startServer();
            }
            
            updateServerStatus();
            showToast("Port mis à jour");
            
        } catch (NumberFormatException e) {
            showToast("Port invalide");
        }
    }
    
    private void showApiDocumentation() {
        String documentation = apiServer.generateApiDocumentation();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Documentation API");
        
        TextView textView = new TextView(this);
        textView.setText(documentation);
        textView.setPadding(50, 50, 50, 50);
        textView.setTextIsSelectable(true);
        
        builder.setView(textView);
        builder.setPositiveButton("Copier URL", (dialog, which) -> {
            copyToClipboard(apiServer.getBaseUrl());
            showToast("URL copiée dans le presse-papiers");
        });
        builder.setNegativeButton("Fermer", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showAddClientDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_api_client, null);
        
        EditText editName = dialogView.findViewById(R.id.edit_client_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_client_description);
        
        // Checkboxes pour les permissions
        View checkReadRecipes = dialogView.findViewById(R.id.check_read_recipes);
        View checkWriteRecipes = dialogView.findViewById(R.id.check_write_recipes);
        View checkReadComments = dialogView.findViewById(R.id.check_read_comments);
        View checkWriteComments = dialogView.findViewById(R.id.check_write_comments);
        View checkReadNutrition = dialogView.findViewById(R.id.check_read_nutrition);
        View checkReadUsers = dialogView.findViewById(R.id.check_read_users);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nouveau client API");
        builder.setView(dialogView);
        
        builder.setPositiveButton("Créer", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String description = editDescription.getText().toString().trim();
            
            if (name.isEmpty()) {
                showToast("Le nom est requis");
                return;
            }
            
            // Collecter les permissions sélectionnées
            List<String> permissions = new ArrayList<>();
            if (((Switch) checkReadRecipes).isChecked()) permissions.add("recipes:read");
            if (((Switch) checkWriteRecipes).isChecked()) permissions.add("recipes:write");
            if (((Switch) checkReadComments).isChecked()) permissions.add("comments:read");
            if (((Switch) checkWriteComments).isChecked()) permissions.add("comments:write");
            if (((Switch) checkReadNutrition).isChecked()) permissions.add("nutrition:read");
            if (((Switch) checkReadUsers).isChecked()) permissions.add("users:read");
            
            // Créer le client
            ApiServer.ApiClient client = apiServer.registerClient(name, description, permissions);
            
            // Afficher les credentials
            showClientCredentials(client);
            
            // Rafraîchir la liste
            refreshClientsList();
        });
        
        builder.setNegativeButton("Annuler", null);
        builder.create().show();
    }
    
    private void showClientCredentials(ApiServer.ApiClient client) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_client_credentials, null);
        
        TextView textClientId = dialogView.findViewById(R.id.text_client_id);
        TextView textClientSecret = dialogView.findViewById(R.id.text_client_secret);
        Button buttonCopyId = dialogView.findViewById(R.id.button_copy_id);
        Button buttonCopySecret = dialogView.findViewById(R.id.button_copy_secret);
        
        textClientId.setText(client.clientId);
        textClientSecret.setText(client.clientSecret);
        
        buttonCopyId.setOnClickListener(v -> {
            copyToClipboard(client.clientId);
            showToast("Client ID copié");
        });
        
        buttonCopySecret.setOnClickListener(v -> {
            copyToClipboard(client.clientSecret);
            showToast("Client Secret copié");
        });
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Credentials Client API");
        builder.setView(dialogView);
        builder.setMessage("⚠️ Sauvegardez ces informations. Le secret ne sera plus affiché.");
        builder.setPositiveButton("J'ai sauvegardé", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    public void revokeClient(String clientId) {
        new AlertDialog.Builder(this)
            .setTitle("Révoquer client")
            .setMessage("Êtes-vous sûr de vouloir révoquer ce client ? Tous ses tokens seront invalidés.")
            .setPositiveButton("Révoquer", (dialog, which) -> {
                apiServer.revokeClient(clientId);
                refreshClientsList();
                updateTokensCount();
                showToast("Client révoqué");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void updateServerStatus() {
        boolean isRunning = apiServer.isServerRunning();
        
        switchServerEnabled.setChecked(isRunning);
        
        if (isRunning) {
            textServerStatus.setText("🟢 Serveur actif");
            textServerStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            textServerUrl.setText(apiServer.getBaseUrl());
            textServerUrl.setVisibility(View.VISIBLE);
        } else {
            textServerStatus.setText("🔴 Serveur arrêté");
            textServerStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            textServerUrl.setVisibility(View.GONE);
        }
    }
    
    private void refreshClientsList() {
        clientsAdapter.updateClients(apiServer.getRegisteredClients());
    }
    
    private void updateTokensCount() {
        int activeTokens = apiServer.getActiveTokens().size();
        textActiveTokens.setText("Tokens actifs: " + activeTokens);
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("API", text);
        clipboard.setPrimaryClip(clip);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateServerStatus();
        refreshClientsList();
        updateTokensCount();
    }
}
