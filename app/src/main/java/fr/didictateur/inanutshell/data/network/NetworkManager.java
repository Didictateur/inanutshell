package fr.didictateur.inanutshell.data.network;

import fr.didictateur.inanutshell.MealieApplication;
import fr.didictateur.inanutshell.data.api.MealieApiService;
import fr.didictateur.inanutshell.config.ServerConfig;
import fr.didictateur.inanutshell.config.MultiServerManager;
import fr.didictateur.inanutshell.network.RetryInterceptor;
import fr.didictateur.inanutshell.network.NetworkStateManager;
import fr.didictateur.inanutshell.cache.SmartCacheInterceptor;
import fr.didictateur.inanutshell.network.ErrorHandler;
import fr.didictateur.inanutshell.performance.PerformanceManager;
import fr.didictateur.inanutshell.logging.AppLogger;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Cache;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;

import java.util.concurrent.TimeUnit;

public class NetworkManager {
    
    private static NetworkManager instance;
    private MealieApiService apiService;
    private Retrofit retrofit;
    
    // Nouveaux gestionnaires techniques intégrés
    private MultiServerManager multiServerManager;
    private RetryInterceptor retryInterceptor;
    private SmartCacheInterceptor cacheInterceptor;
    private NetworkStateManager networkStateManager;
    private ErrorHandler errorHandler;
    private PerformanceManager performanceManager;
    private AppLogger logger;
    private Context context;
    
    private NetworkManager() {
        // L'initialisation sera faite dans initialize()
    }
    
    /**
     * Initialise NetworkManager avec le contexte (à appeler depuis l'Application)
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialiser tous les gestionnaires techniques avec fallback sécurisé
        try {
            this.logger = AppLogger.getInstance(context);
            this.performanceManager = PerformanceManager.getInstance();
            this.networkStateManager = NetworkStateManager.getInstance(context);
            this.errorHandler = new ErrorHandler(context);
            this.multiServerManager = MultiServerManager.getInstance(context);
            
            // Intercepteurs avec constructeurs corrects
            this.retryInterceptor = new RetryInterceptor(); // Constructor par défaut
            this.cacheInterceptor = new SmartCacheInterceptor(context, SmartCacheInterceptor.CacheStrategy.BALANCED);
            
            logger.logInfo("NetworkManager", "Tous les composants techniques initialisés");
        } catch (Exception e) {
            // Fallback si les composants techniques ne sont pas disponibles
            android.util.Log.w("NetworkManager", "Composants techniques non disponibles, mode legacy: " + e.getMessage());
            this.logger = null;
            this.performanceManager = null;
            this.networkStateManager = null;
            this.errorHandler = null;
            this.multiServerManager = null;
            this.retryInterceptor = null;
            this.cacheInterceptor = null;
        }
        
        // Observer les changements de serveur pour reconfigurer
        multiServerManager.getCurrentServerLiveData().observeForever(serverConfig -> {
            if (serverConfig != null) {
                logger.logInfo("NetworkManager", "Server changed to: " + serverConfig.getName());
                setupRetrofitWithServer(serverConfig);
            }
        });
        
        setupRetrofit();
        logger.logInfo("NetworkManager", "NetworkManager initialized with technical infrastructure");
    }
    
    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    /**
     * Obtient une instance initialisée avec le contexte
     */
    public static NetworkManager getInstance(Context context) {
        NetworkManager manager = getInstance();
        if (manager.context == null) {
            manager.initialize(context);
        }
        return manager;
    }
    
    private void setupRetrofit() {
        ServerConfig currentServer = null;
        if (multiServerManager != null) {
            currentServer = multiServerManager.getCurrentServer();
        }
        
        if (currentServer != null) {
            setupRetrofitWithServer(currentServer);
        } else {
            // Fallback à l'ancienne méthode pour compatibilité
            setupRetrofitLegacy();
        }
    }
    
    /**
     * Configuration de Retrofit avec un serveur spécifique (nouvelle méthode intégrée)
     */
    private void setupRetrofitWithServer(ServerConfig server) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Setting up Retrofit with server: " + server.getName());
        }
        
        // Configuration du client HTTP avec tous les intercepteurs techniques
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(server.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(server.getTimeoutSeconds(), TimeUnit.SECONDS)
            .writeTimeout(server.getTimeoutSeconds(), TimeUnit.SECONDS);
        
        // Ajouter le cache intelligent
        if (cacheInterceptor != null && context != null) {
            Cache cache = SmartCacheInterceptor.createOptimizedCache(context);
            httpClient.cache(cache);
            httpClient.addInterceptor(cacheInterceptor);
        }
        
        // Ajouter le retry automatique
        if (retryInterceptor != null) {
            httpClient.addInterceptor(retryInterceptor);
        }
        
        // Logging (uniquement en debug)
        if (android.util.Log.isLoggable("NetworkManager", android.util.Log.DEBUG)) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(loggingInterceptor);
        }
        
        // Certificats auto-signés si autorisés
        if (server.isAllowSelfSigned()) {
            // TODO: Ajouter la configuration SSL pour certificats auto-signés
        }
        
        String baseUrl = server.getApiUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        if (logger != null) {
            logger.logDebug("NetworkManager", "Base URL: " + baseUrl);
        }
        
        // Create Gson with custom deserializer for Recipe
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(fr.didictateur.inanutshell.data.model.Recipe.class, 
                new fr.didictateur.inanutshell.data.model.RecipeDeserializer())
            .create();
        
        // Configuration de Retrofit
        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        
        apiService = retrofit.create(MealieApiService.class);
        
        if (logger != null) {
            logger.logInfo("NetworkManager", "Retrofit configured successfully for server: " + server.getName());
        }
    }
    
    /**
     * Configuration Retrofit legacy pour compatibilité
     */
    private void setupRetrofitLegacy() {
        // Configuration du client HTTP
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);
        
        // Logging (uniquement en debug)
        if (android.util.Log.isLoggable("NetworkManager", android.util.Log.DEBUG)) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(loggingInterceptor);
        }
        
        // URL de base
        String baseUrl = MealieApplication.getInstance().getMealieServerUrl();
        if (baseUrl.isEmpty()) {
            baseUrl = "http://localhost:9000/"; // URL par défaut
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        android.util.Log.d("NetworkManager", "Base URL (legacy): " + baseUrl);
        
        // Create Gson with custom deserializer for Recipe
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(fr.didictateur.inanutshell.data.model.Recipe.class, 
                new fr.didictateur.inanutshell.data.model.RecipeDeserializer())
            .create();
        
        // Configuration de Retrofit
        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        
        apiService = retrofit.create(MealieApiService.class);
    }
    
    public MealieApiService getApiService() {
        // Reconfigurer si l'URL a changé
        String currentUrl = MealieApplication.getInstance().getMealieServerUrl();
        if (retrofit == null || !retrofit.baseUrl().toString().startsWith(currentUrl)) {
            setupRetrofit();
        }
        return apiService;
    }
    
    public void reconfigure() {
        setupRetrofit();
    }
    
    public String getAuthHeader() {
        // Utiliser le serveur actuel si disponible
        if (multiServerManager != null) {
            ServerConfig currentServer = multiServerManager.getCurrentServer();
            if (currentServer != null && currentServer.getApiKey() != null) {
                String authHeader = "Bearer " + currentServer.getApiKey();
                if (logger != null) {
                    logger.logDebug("NetworkManager", "Auth header from current server: " + 
                        authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
                }
                return authHeader;
            }
        }
        
        // Fallback à l'ancienne méthode
        String token = MealieApplication.getInstance().getMealieAuthToken();
        String authHeader = token.isEmpty() ? "" : "Bearer " + token;
        android.util.Log.d("NetworkManager", "Auth header (legacy): " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
        return authHeader;
    }
    
    public String getAuthToken() {
        // Utiliser le serveur actuel si disponible
        if (multiServerManager != null) {
            ServerConfig currentServer = multiServerManager.getCurrentServer();
            if (currentServer != null && currentServer.getApiKey() != null) {
                return currentServer.getApiKey();
            }
        }
        
        // Fallback
        return MealieApplication.getInstance().getMealieAuthToken();
    }
    
    public String getBaseUrl() {
        // Utiliser le serveur actuel si disponible
        if (multiServerManager != null) {
            ServerConfig currentServer = multiServerManager.getCurrentServer();
            if (currentServer != null) {
                return currentServer.getBaseUrl();
            }
        }
        
        // Fallback
        return MealieApplication.getInstance().getMealieServerUrl();
    }
    
    public boolean isConnected() {
        // Utiliser NetworkStateManager si disponible
        if (networkStateManager != null) {
            boolean networkConnected = networkStateManager.getCurrentNetworkState().isConnected;
            boolean hasValidServer = false;
            
            if (multiServerManager != null) {
                ServerConfig currentServer = multiServerManager.getCurrentServer();
                hasValidServer = currentServer != null && 
                    currentServer.getStatus() == ServerConfig.ServerStatus.ONLINE;
            } else {
                // Fallback à l'ancienne vérification
                String url = getBaseUrl();
                String token = getAuthToken();
                hasValidServer = url != null && !url.isEmpty() && token != null && !token.isEmpty();
            }
            
            return networkConnected && hasValidServer;
        }
        
        // Fallback à l'ancienne méthode
        String url = getBaseUrl();
        String token = getAuthToken();
        return url != null && !url.isEmpty() && token != null && !token.isEmpty();
    }
    
    public OkHttpClient getOkHttpClient() {
        // Récupérer le client OkHttp de Retrofit
        if (retrofit != null) {
            return retrofit.callFactory() instanceof OkHttpClient ? (OkHttpClient) retrofit.callFactory() : null;
        }
        return null;
    }
    
    public String getRecipeImageUrl(String recipeId) {
        String baseUrl = MealieApplication.getInstance().getMealieServerUrl();
        if (baseUrl.isEmpty()) {
            return "";
        }
        
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        return baseUrl + "api/media/recipes/" + recipeId + "/images/min-original.webp";
    }
    
    // Callback interfaces
    public interface LoginCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface RecipesCallback {
        void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes);
        void onError(String error);
    }
    
    public interface CreateRecipeCallback {
        void onSuccess(fr.didictateur.inanutshell.data.model.Recipe recipe);
        void onError(String error);
    }
    
    public interface DeleteRecipeCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface UpdateRecipeCallback {
        void onSuccess(fr.didictateur.inanutshell.data.model.Recipe recipe);
        void onError(String error);
    }
    
    public interface RecipeCallback {
        void onSuccess(fr.didictateur.inanutshell.data.model.Recipe recipe);
        void onError(String error);
    }
    
    public interface ToolsCallback {
        void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.Tool> tools);
        void onError(String error);
    }
    
    // Login method
    public void login(String serverUrl, String email, String password, LoginCallback callback) {
        // Store server URL
        MealieApplication.getInstance().setMealieServerUrl(serverUrl);
        
        // Reconfigure with new URL
        reconfigure();
        
        // Make actual API call
        retrofit2.Call<fr.didictateur.inanutshell.data.response.LoginResponse> call = 
            apiService.login(email, password);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.response.LoginResponse>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.response.LoginResponse> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.response.LoginResponse> response) {
                android.util.Log.d("NetworkManager", "Login response code: " + response.code());
                android.util.Log.d("NetworkManager", "Response body: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    fr.didictateur.inanutshell.data.response.LoginResponse loginResponse = response.body();
                    String token = loginResponse.getAccessToken();
                    String tokenType = loginResponse.getTokenType();
                    android.util.Log.d("NetworkManager", "Token type: " + tokenType);
                    android.util.Log.d("NetworkManager", "Access token length: " + (token != null ? token.length() : "null"));
                    if (token != null && token.length() > 10) {
                        android.util.Log.d("NetworkManager", "Token preview: " + token.substring(0, 10) + "...");
                    }
                    MealieApplication.getInstance().setMealieAuthToken(token != null ? token : "");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Erreur de connexion: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.response.LoginResponse> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Get recipes method with technical infrastructure
    public void getRecipes(RecipesCallback callback) {
        // Utiliser PerformanceManager pour optimiser l'exécution
        if (performanceManager != null) {
            performanceManager.executeWithCache("get_recipes", new PerformanceManager.PerformanceTask<java.util.List<fr.didictateur.inanutshell.data.model.Recipe>>() {
                @Override
                public java.util.List<fr.didictateur.inanutshell.data.model.Recipe> execute() throws Exception {
                    return getRecipesSynchronous();
                }
                
                @Override
                public PerformanceManager.TaskType getType() {
                    return PerformanceManager.TaskType.IO;
                }
                
                @Override
                public boolean isCacheable() {
                    return true; // Les recettes peuvent être mises en cache
                }
            }, new PerformanceManager.PerformanceCallback<java.util.List<fr.didictateur.inanutshell.data.model.Recipe>>() {
                @Override
                public void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes) {
                    callback.onSuccess(recipes);
                }
                
                @Override
                public void onError(Exception error) {
                    String errorMsg = handleError(error, "Erreur lors du chargement des recettes");
                    callback.onError(errorMsg);
                }
            });
        } else {
            // Fallback à l'ancienne méthode
            getRecipesLegacy(callback);
        }
    }
    
    /**
     * Version synchrone pour PerformanceManager
     */
    private java.util.List<fr.didictateur.inanutshell.data.model.Recipe> getRecipesSynchronous() throws Exception {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            throw new Exception("Non authentifié");
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call = 
            apiService.getRecipes(authHeader, 1, 50, "name", "asc", "");
        
        retrofit2.Response<fr.didictateur.inanutshell.data.response.RecipeListResponse> response = call.execute();
        
        if (response.isSuccessful() && response.body() != null) {
            java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes = response.body().getItems();
            if (recipes == null) {
                recipes = new java.util.ArrayList<>();
            }
            if (logger != null) {
                logger.logInfo("NetworkManager", "Loaded " + recipes.size() + " recipes");
            }
            return recipes;
        } else {
            throw new Exception("Erreur HTTP: " + response.code() + " " + response.message());
        }
    }
    
    /**
     * Version legacy pour compatibilité
     */
    private void getRecipesLegacy(RecipesCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call = 
            apiService.getRecipes(authHeader, 1, 50, "name", "asc", "");
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.response.RecipeListResponse>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.response.RecipeListResponse> response) {
                android.util.Log.d("NetworkManager", "Recipes response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes = response.body().getItems();
                    if (recipes == null) {
                        recipes = new java.util.ArrayList<>();
                    }
                    android.util.Log.d("NetworkManager", "Loaded " + recipes.size() + " recipes");
                    callback.onSuccess(recipes);
                } else {
                    String errorMsg = "Erreur lors du chargement: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Get recipes with pagination
    public void getRecipesPage(int page, int perPage, RecipesCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call = 
            apiService.getRecipes(authHeader, page, perPage, "name", "asc", "");
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.response.RecipeListResponse>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.response.RecipeListResponse> response) {
                android.util.Log.d("NetworkManager", "Recipes page " + page + " response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<fr.didictateur.inanutshell.data.model.Recipe> recipes = response.body().getItems();
                    if (recipes == null) {
                        recipes = new java.util.ArrayList<>();
                    }
                    android.util.Log.d("NetworkManager", "Loaded " + recipes.size() + " recipes for page " + page);
                    callback.onSuccess(recipes);
                } else {
                    String errorMsg = "Erreur lors du chargement de la page " + page + ": " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.response.RecipeListResponse> call, Throwable t) {
                String errorMsg = "Erreur réseau page " + page + ": " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Create recipe method with technical infrastructure
    public void createRecipe(fr.didictateur.inanutshell.data.model.Recipe recipe, CreateRecipeCallback callback) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Creating recipe: " + recipe.getName());
        }
        
        // Utiliser PerformanceManager pour optimiser l'exécution
        if (performanceManager != null) {
            performanceManager.executeWithCache("create_recipe", new PerformanceManager.PerformanceTask<fr.didictateur.inanutshell.data.model.Recipe>() {
                @Override
                public fr.didictateur.inanutshell.data.model.Recipe execute() throws Exception {
                    return createRecipeSynchronous(recipe);
                }
                
                @Override
                public PerformanceManager.TaskType getType() {
                    return PerformanceManager.TaskType.IO;
                }
                
                @Override
                public boolean isCacheable() {
                    return false; // Création n'est pas cacheable
                }
            }, new PerformanceManager.PerformanceCallback<fr.didictateur.inanutshell.data.model.Recipe>() {
                @Override
                public void onSuccess(fr.didictateur.inanutshell.data.model.Recipe createdRecipe) {
                    if (logger != null) {
                        logger.logInfo("NetworkManager", "Recipe created successfully");
                    }
                    callback.onSuccess(createdRecipe);
                }
                
                @Override
                public void onError(Exception error) {
                    String errorMsg = handleError(error, "Erreur lors de la création de recette");
                    callback.onError(errorMsg);
                }
            });
        } else {
            // Fallback à l'ancienne méthode
            createRecipeLegacy(recipe, callback);
        }
    }
    
    /**
     * Version synchrone pour PerformanceManager
     */
    private fr.didictateur.inanutshell.data.model.Recipe createRecipeSynchronous(fr.didictateur.inanutshell.data.model.Recipe recipe) throws Exception {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            throw new Exception("Non authentifié");
        }
        
        retrofit2.Call<okhttp3.ResponseBody> call = 
            apiService.createRecipe(authHeader, recipe);
        
        retrofit2.Response<okhttp3.ResponseBody> response = call.execute();
        
        if (response.isSuccessful()) {
            // Créer une recette fictive pour indiquer le succès
            fr.didictateur.inanutshell.data.model.Recipe dummyRecipe = new fr.didictateur.inanutshell.data.model.Recipe();
            dummyRecipe.setName("Recette créée avec succès");
            return dummyRecipe;
        } else {
            throw new Exception("Erreur HTTP: " + response.code() + " " + response.message());
        }
    }
    
    /**
     * Version legacy pour compatibilité
     */
    private void createRecipeLegacy(fr.didictateur.inanutshell.data.model.Recipe recipe, CreateRecipeCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<okhttp3.ResponseBody> call = 
            apiService.createRecipe(authHeader, recipe);
        
        call.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, 
                                 retrofit2.Response<okhttp3.ResponseBody> response) {
                android.util.Log.d("NetworkManager", "Create recipe response code: " + response.code());
                
                if (response.isSuccessful()) {
                    // Log la réponse pour déboguer
                    String responseBodyString = "";
                    if (response.body() != null) {
                        try {
                            responseBodyString = response.body().string();
                            android.util.Log.d("NetworkManager", "Response body content: " + responseBodyString);
                        } catch (Exception e) {
                            android.util.Log.w("NetworkManager", "Could not read response body: " + e.getMessage());
                        }
                    }
                    
                    // Succès - créer une recette fictive pour indiquer le succès
                    fr.didictateur.inanutshell.data.model.Recipe dummyRecipe = new fr.didictateur.inanutshell.data.model.Recipe();
                    dummyRecipe.setName("Recette créée avec succès");
                    android.util.Log.d("NetworkManager", "Recipe created successfully!");
                    callback.onSuccess(dummyRecipe);
                } else {
                    String errorMsg = "Erreur lors de la création: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Delete recipe method
    public void deleteRecipe(String recipeId, DeleteRecipeCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<Void> call = apiService.deleteRecipe(authHeader, recipeId);
        
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                android.util.Log.d("NetworkManager", "Delete recipe response code: " + response.code());
                
                if (response.isSuccessful()) {
                    android.util.Log.d("NetworkManager", "Recipe deleted successfully!");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Erreur lors de la suppression: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Update recipe method
    public void updateRecipe(String recipeId, fr.didictateur.inanutshell.data.model.Recipe recipe, UpdateRecipeCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call = 
            apiService.updateRecipe(authHeader, recipeId, recipe);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.model.Recipe>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.model.Recipe> response) {
                android.util.Log.d("NetworkManager", "Update recipe response code: " + response.code());
                
                if (response.isSuccessful()) {
                    fr.didictateur.inanutshell.data.model.Recipe updatedRecipe = response.body();
                    if (updatedRecipe != null) {
                        android.util.Log.d("NetworkManager", "Recipe updated successfully: " + updatedRecipe.getName());
                        callback.onSuccess(updatedRecipe);
                    } else {
                        // Créer une recette fictive pour indiquer le succès
                        fr.didictateur.inanutshell.data.model.Recipe dummyRecipe = new fr.didictateur.inanutshell.data.model.Recipe();
                        dummyRecipe.setName("Recette modifiée avec succès");
                        android.util.Log.d("NetworkManager", "Recipe updated successfully!");
                        callback.onSuccess(dummyRecipe);
                    }
                } else {
                    String errorMsg = "Erreur lors de la modification: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Get single recipe method
    public void getRecipe(String recipeId, RecipeCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call = 
            apiService.getRecipe(authHeader, recipeId);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.model.Recipe>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.model.Recipe> response) {
                android.util.Log.d("NetworkManager", "Get recipe response code: " + response.code());
                
                // Log the raw JSON response for debugging
                try {
                    String rawJson = response.raw().body() != null ? response.raw().body().string() : "null";
                    android.util.Log.d("NetworkManager", "Raw JSON response: " + rawJson);
                } catch (Exception e) {
                    android.util.Log.w("NetworkManager", "Could not log raw response: " + e.getMessage());
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    fr.didictateur.inanutshell.data.model.Recipe recipe = response.body();
                    android.util.Log.d("NetworkManager", "Recipe loaded: " + recipe.getName());
                    
                    // Log all time-related fields for debugging
                    android.util.Log.d("NetworkManager", "Time fields - PrepTime: " + recipe.getPrepTime() + 
                        ", CookTime: " + recipe.getCookTime() + 
                        ", PerformTime: " + recipe.getPerformTime() + 
                        ", TotalTime: " + recipe.getTotalTime());
                    
                    callback.onSuccess(recipe);
                } else {
                    String errorMsg = "Erreur lors du chargement: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.model.Recipe> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    // Alias pour getRecipe - pour plus de clarté
    public void getRecipeById(String recipeId, RecipeCallback callback) {
        getRecipe(recipeId, callback);
    }
    
    /**
     * Récupère la liste des outils disponibles
     */
    public void getTools(ToolsCallback callback) {
        if (apiService == null) {
            callback.onError("Non authentifié");
            return;
        }
        
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        android.util.Log.d("NetworkManager", "Récupération des outils...");
        retrofit2.Call<java.util.List<fr.didictateur.inanutshell.data.model.Tool>> call = 
            apiService.getTools("Bearer " + token);
            
        call.enqueue(new retrofit2.Callback<java.util.List<fr.didictateur.inanutshell.data.model.Tool>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<fr.didictateur.inanutshell.data.model.Tool>> call, 
                                 retrofit2.Response<java.util.List<fr.didictateur.inanutshell.data.model.Tool>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<fr.didictateur.inanutshell.data.model.Tool> tools = response.body();
                    android.util.Log.d("NetworkManager", "Outils récupérés: " + tools.size());
                    callback.onSuccess(tools);
                } else {
                    String errorMsg = "Erreur lors du chargement des outils: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<java.util.List<fr.didictateur.inanutshell.data.model.Tool>> call, Throwable t) {
                String errorMsg = "Erreur réseau: " + t.getMessage();
                android.util.Log.e("NetworkManager", errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Gère les erreurs avec ErrorHandler
     */
    private String handleError(Exception error, String defaultMessage) {
        if (errorHandler != null) {
            try {
                // Utiliser ErrorHandler si disponible
                errorHandler.handleError(error, new ErrorHandler.ErrorCallback() {
                    @Override
                    public void onError(ErrorHandler.ErrorInfo errorInfo) {
                        // Callback géré automatiquement
                    }
                });
                return error.getMessage() != null ? error.getMessage() : defaultMessage;
            } catch (Exception e) {
                // Fallback si ErrorHandler échoue
                android.util.Log.w("NetworkManager", "ErrorHandler failed: " + e.getMessage());
            }
        }
        
        // Fallback logging standard
        if (logger != null) {
            logger.logError("NetworkManager", error.getMessage() != null ? error.getMessage() : defaultMessage, error);
        } else {
            android.util.Log.e("NetworkManager", defaultMessage, error);
        }
        return error.getMessage() != null ? error.getMessage() : defaultMessage;
    }
    
    /**
     * Gère les erreurs réseau et multi-serveurs avec auto-failover
     */
    private String handleNetworkError(Throwable error, String operation) {
        String errorMsg = handleError(new Exception(error), "Erreur lors de: " + operation);
        
        // Si on a MultiServerManager, tenter un failover automatique
        if (multiServerManager != null) {
            try {
                multiServerManager.switchToNextAvailableServer();
                ServerConfig currentServer = multiServerManager.getCurrentServer();
                if (currentServer != null && logger != null) {
                    logger.logInfo("NetworkManager", "Tentative de failover vers: " + currentServer.getBaseUrl());
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.logError("NetworkManager", "Erreur during failover: " + e.getMessage(), e);
                }
            }
        }
        
        return errorMsg;
    }
    
    // ===== MEAL PLANS API METHODS =====
    
    /**
     * Interface pour les callbacks de meal plans
     */
    public interface MealPlansCallback {
        void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> mealPlans);
        void onError(String error);
    }
    
    public interface MealPlanCallback {
        void onSuccess(fr.didictateur.inanutshell.data.model.MealieMealPlan mealPlan);
        void onError(String error);
    }
    
    /**
     * Récupère les meal plans dans une plage de dates avec infrastructure technique
     */
    public void getMealPlans(String startDate, String endDate, MealPlansCallback callback) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Getting meal plans from " + startDate + " to " + endDate);
        }
        
        if (performanceManager != null) {
            String cacheKey = "meal_plans_" + startDate + "_" + endDate;
            performanceManager.executeWithCache(cacheKey, new PerformanceManager.PerformanceTask<java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan>>() {
                @Override
                public java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> execute() throws Exception {
                    return getMealPlansSynchronous(startDate, endDate);
                }
                
                @Override
                public PerformanceManager.TaskType getType() {
                    return PerformanceManager.TaskType.IO;
                }
                
                @Override
                public boolean isCacheable() {
                    return true;
                }
            }, new PerformanceManager.PerformanceCallback<java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan>>() {
                @Override
                public void onSuccess(java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> mealPlans) {
                    callback.onSuccess(mealPlans);
                }
                
                @Override
                public void onError(Exception error) {
                    String errorMsg = handleError(error, "Erreur lors du chargement des meal plans");
                    callback.onError(errorMsg);
                }
            });
        } else {
            getMealPlansLegacy(startDate, endDate, callback);
        }
    }
    
    private java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> getMealPlansSynchronous(String startDate, String endDate) throws Exception {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            throw new Exception("Non authentifié");
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.response.MealPlanListResponse> call = 
            apiService.getMealPlans(authHeader, 1, 1000, startDate, endDate);
        
        retrofit2.Response<fr.didictateur.inanutshell.data.response.MealPlanListResponse> response = call.execute();
        
        if (response.isSuccessful() && response.body() != null) {
            java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> mealPlans = response.body().getItems();
            if (mealPlans == null) {
                mealPlans = new java.util.ArrayList<>();
            }
            if (logger != null) {
                logger.logInfo("NetworkManager", "Loaded " + mealPlans.size() + " meal plans");
            }
            return mealPlans;
        } else {
            throw new Exception("Erreur HTTP: " + response.code() + " " + response.message());
        }
    }
    
    private void getMealPlansLegacy(String startDate, String endDate, MealPlansCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.response.MealPlanListResponse> call = 
            apiService.getMealPlans(authHeader, 1, 1000, startDate, endDate);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.response.MealPlanListResponse>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.response.MealPlanListResponse> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.response.MealPlanListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<fr.didictateur.inanutshell.data.model.MealieMealPlan> mealPlans = response.body().getItems();
                    if (mealPlans == null) {
                        mealPlans = new java.util.ArrayList<>();
                    }
                    android.util.Log.d("NetworkManager", "Loaded " + mealPlans.size() + " meal plans");
                    callback.onSuccess(mealPlans);
                } else {
                    String errorMsg = "Erreur lors du chargement des meal plans: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.response.MealPlanListResponse> call, Throwable t) {
                String errorMsg = handleNetworkError(t, "chargement meal plans");
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Crée un meal plan avec infrastructure technique
     */
    public void createMealPlan(fr.didictateur.inanutshell.data.model.MealieMealPlan mealPlan, MealPlanCallback callback) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Creating meal plan: " + mealPlan.getTitle());
        }
        
        if (performanceManager != null) {
            performanceManager.executeWithCache("create_meal_plan", new PerformanceManager.PerformanceTask<fr.didictateur.inanutshell.data.model.MealieMealPlan>() {
                @Override
                public fr.didictateur.inanutshell.data.model.MealieMealPlan execute() throws Exception {
                    return createMealPlanSynchronous(mealPlan);
                }
                
                @Override
                public PerformanceManager.TaskType getType() {
                    return PerformanceManager.TaskType.IO;
                }
                
                @Override
                public boolean isCacheable() {
                    return false; // Création n'est pas cacheable
                }
            }, new PerformanceManager.PerformanceCallback<fr.didictateur.inanutshell.data.model.MealieMealPlan>() {
                @Override
                public void onSuccess(fr.didictateur.inanutshell.data.model.MealieMealPlan createdPlan) {
                    if (logger != null) {
                        logger.logInfo("NetworkManager", "Meal plan created successfully: " + createdPlan.getId());
                    }
                    callback.onSuccess(createdPlan);
                }
                
                @Override
                public void onError(Exception error) {
                    String errorMsg = handleError(error, "Erreur lors de la création du meal plan");
                    callback.onError(errorMsg);
                }
            });
        } else {
            createMealPlanLegacy(mealPlan, callback);
        }
    }
    
    private fr.didictateur.inanutshell.data.model.MealieMealPlan createMealPlanSynchronous(fr.didictateur.inanutshell.data.model.MealieMealPlan mealPlan) throws Exception {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            throw new Exception("Non authentifié");
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call = 
            apiService.createMealPlan(authHeader, mealPlan);
        
        retrofit2.Response<fr.didictateur.inanutshell.data.model.MealieMealPlan> response = call.execute();
        
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            throw new Exception("Erreur HTTP: " + response.code() + " " + response.message());
        }
    }
    
    private void createMealPlanLegacy(fr.didictateur.inanutshell.data.model.MealieMealPlan mealPlan, MealPlanCallback callback) {
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call = 
            apiService.createMealPlan(authHeader, mealPlan);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.model.MealieMealPlan>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.model.MealieMealPlan> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Erreur lors de la création du meal plan: " + response.code();
                    android.util.Log.e("NetworkManager", errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call, Throwable t) {
                String errorMsg = handleNetworkError(t, "création meal plan");
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Met à jour un meal plan
     */
    public void updateMealPlan(String planId, fr.didictateur.inanutshell.data.model.MealieMealPlan mealPlan, MealPlanCallback callback) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Updating meal plan: " + planId);
        }
        
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call = 
            apiService.updateMealPlan(authHeader, planId, mealPlan);
        
        call.enqueue(new retrofit2.Callback<fr.didictateur.inanutshell.data.model.MealieMealPlan>() {
            @Override
            public void onResponse(retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call, 
                                 retrofit2.Response<fr.didictateur.inanutshell.data.model.MealieMealPlan> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = handleError(new Exception("HTTP " + response.code()), "Erreur mise à jour meal plan");
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<fr.didictateur.inanutshell.data.model.MealieMealPlan> call, Throwable t) {
                String errorMsg = handleNetworkError(t, "mise à jour meal plan");
                callback.onError(errorMsg);
            }
        });
    }
    
    /**
     * Supprime un meal plan
     */
    public void deleteMealPlan(String planId, SimpleCallback callback) {
        if (logger != null) {
            logger.logInfo("NetworkManager", "Deleting meal plan: " + planId);
        }
        
        String authHeader = getAuthHeader();
        if (authHeader.isEmpty()) {
            callback.onError("Non authentifié");
            return;
        }
        
        retrofit2.Call<Void> call = apiService.deleteMealPlan(authHeader, planId);
        
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Meal plan supprimé");
                } else {
                    String errorMsg = handleError(new Exception("HTTP " + response.code()), "Erreur suppression meal plan");
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                String errorMsg = handleNetworkError(t, "suppression meal plan");
                callback.onError(errorMsg);
            }
        });
    }
    
    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
