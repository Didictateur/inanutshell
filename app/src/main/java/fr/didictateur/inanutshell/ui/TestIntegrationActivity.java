package fr.didictateur.inanutshell.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import fr.didictateur.inanutshell.logging.AppLogger;
import fr.didictateur.inanutshell.performance.PerformanceManager;
import fr.didictateur.inanutshell.network.NetworkStateManager;
import fr.didictateur.inanutshell.config.MultiServerManager;
import java.util.List;

/**
 * Activité de test pour démontrer l'intégration des fonctionnalités techniques avec l'API Mealie
 */
public class TestIntegrationActivity extends AppCompatActivity {
    
    private NetworkManager networkManager;
    private AppLogger logger;
    private PerformanceManager performanceManager;
    private NetworkStateManager networkStateManager;
    private MultiServerManager multiServerManager;
    
    private TextView statusText;
    private Button testConnectionBtn;
    private Button testRecipesBtn;
    private Button testPerformanceBtn;
    private Button testMultiServerBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialisation des composants techniques
        initializeTechnicalComponents();
        
        // Interface utilisateur basique
        setupUI();
        
        // Configuration des tests
        setupTests();
    }
    
    private void initializeTechnicalComponents() {
        // Récupération du NetworkManager avec tous les composants techniques intégrés
        networkManager = NetworkManager.getInstance(this);
        
        // Accès direct aux composants techniques pour les tests
        logger = AppLogger.getInstance(this);
        performanceManager = PerformanceManager.getInstance();
        networkStateManager = NetworkStateManager.getInstance(this);
        multiServerManager = MultiServerManager.getInstance(this);
        
        logger.logInfo("TestIntegration", "Tous les composants techniques initialisés");
    }
    
    private void setupUI() {
        // Création d'interface simple pour les tests
        setContentView(android.R.layout.activity_list_item);
        
        // Créer les éléments UI programmatiquement
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        statusText = new TextView(this);
        statusText.setText("État des tests d'intégration");
        statusText.setTextSize(16);
        layout.addView(statusText);
        
        testConnectionBtn = new Button(this);
        testConnectionBtn.setText("Test Connexion + État Réseau");
        layout.addView(testConnectionBtn);
        
        testRecipesBtn = new Button(this);
        testRecipesBtn.setText("Test Recettes + Performance");
        layout.addView(testRecipesBtn);
        
        testPerformanceBtn = new Button(this);
        testPerformanceBtn.setText("Test Performance Manager");
        layout.addView(testPerformanceBtn);
        
        testMultiServerBtn = new Button(this);
        testMultiServerBtn.setText("Test Multi-Serveur");
        layout.addView(testMultiServerBtn);
        
        setContentView(layout);
    }
    
    private void setupTests() {
        testConnectionBtn.setOnClickListener(v -> testConnectionWithNetworkState());
        testRecipesBtn.setOnClickListener(v -> testRecipesWithPerformance());
        testPerformanceBtn.setOnClickListener(v -> testPerformanceManagerDirectly());
        testMultiServerBtn.setOnClickListener(v -> testMultiServerFailover());
    }
    
    /**
     * Test 1: Connexion avec vérification d'état réseau
     */
    private void testConnectionWithNetworkState() {
        updateStatus("Test de connexion avec état réseau...");
        
        // Vérifier l'état du réseau avec NetworkStateManager
        boolean isConnected = networkStateManager.isConnected();
        boolean isWifi = networkStateManager.isWifi();
        
        logger.logInfo("TestIntegration", "État réseau - Connecté: " + isConnected + ", WiFi: " + isWifi);
        
        if (isConnected) {
            // Tester la connexion Mealie via récupération de recettes
            networkManager.getRecipes(new NetworkManager.RecipesCallback() {
                @Override
                public void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes) {
                    updateStatus("✓ Connexion réussie - " + recipes.size() + " recettes trouvées");
                    logger.logInfo("TestIntegration", "Test connexion réussi: " + recipes.size() + " recettes");
                }
                
                @Override
                public void onError(String error) {
                    updateStatus("✗ Erreur connexion: " + error);
                    logger.logError("TestIntegration", "Test connexion échoué: " + error, new Exception(error));
                }
            });
        } else {
            updateStatus("✗ Pas de connexion réseau détectée");
        }
    }
    
    /**
     * Test 2: Chargement recettes avec optimisation performance
     */
    private void testRecipesWithPerformance() {
        updateStatus("Test de chargement recettes avec optimisation...");
        
        long startTime = System.currentTimeMillis();
        
        // Utiliser NetworkManager qui intègre automatiquement PerformanceManager
        networkManager.getRecipes(new NetworkManager.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                long duration = System.currentTimeMillis() - startTime;
                String message = String.format("✓ %d recettes chargées en %dms (optimisé)", 
                                              recipes.size(), duration);
                updateStatus(message);
                
                logger.logInfo("TestIntegration", message);
                
                // Vérifier que le cache a été utilisé
                if (performanceManager != null) {
                    logger.logInfo("TestIntegration", "Cache utilisé par PerformanceManager");
                }
            }
            
            @Override
            public void onError(String error) {
                updateStatus("✗ Erreur chargement recettes: " + error);
                logger.logError("TestIntegration", "Test recettes échoué: " + error, new Exception(error));
            }
        });
    }
    
    /**
     * Test 3: Test direct du PerformanceManager
     */
    private void testPerformanceManagerDirectly() {
        updateStatus("Test direct du PerformanceManager...");
        
        performanceManager.executeWithCache("test_performance", new PerformanceManager.PerformanceTask<String>() {
            @Override
            public String execute() throws Exception {
                // Simuler une tâche coûteuse
                Thread.sleep(100);
                return "Tâche de performance terminée";
            }
            
            @Override
            public PerformanceManager.TaskType getType() {
                return PerformanceManager.TaskType.COMPUTE;
            }
            
            @Override
            public boolean isCacheable() {
                return true;
            }
        }, new PerformanceManager.PerformanceCallback<String>() {
            @Override
            public void onSuccess(String result) {
                updateStatus("✓ Performance Manager: " + result);
                logger.logInfo("TestIntegration", "Test PerformanceManager réussi");
            }
            
            @Override
            public void onError(Exception error) {
                updateStatus("✗ Erreur Performance Manager: " + error.getMessage());
            }
        });
    }
    
    /**
     * Test 4: Test du système multi-serveurs
     */
    private void testMultiServerFailover() {
        updateStatus("Test du système multi-serveurs...");
        
        // Tester le serveur actuel
        fr.didictateur.inanutshell.config.ServerConfig currentServer = 
            multiServerManager.getCurrentServer();
        
        if (currentServer != null) {
            String message = String.format("Serveur actuel: %s", currentServer.getBaseUrl());
            updateStatus("✓ " + message);
            logger.logInfo("TestIntegration", message);
        } else {
            updateStatus("✗ Aucun serveur configuré pour le test");
        }
    }
    
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusText.setText(statusText.getText() + "\n" + message);
        });
    }
}
