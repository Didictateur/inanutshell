package fr.didictateur.inanutshell.data.network;

import fr.didictateur.inanutshell.MealieApplication;
import fr.didictateur.inanutshell.data.api.MealieApiService;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class NetworkManager {
    
    private static NetworkManager instance;
    private MealieApiService apiService;
    private Retrofit retrofit;
    
    private NetworkManager() {
        setupRetrofit();
    }
    
    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    private void setupRetrofit() {
        // Configuration des interceptors
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Log pour déboguer
        android.util.Log.d("NetworkManager", "Configuration Retrofit...");
        
        // DNS personnalisé avec fallback vers IP pour cook.cosmoris.fr
        okhttp3.Dns customDns = hostname -> {
            android.util.Log.d("NetworkManager", "DNS lookup pour: " + hostname);
            if ("cook.cosmoris.fr".equals(hostname)) {
                try {
                    java.net.InetAddress address = java.net.InetAddress.getByName("83.228.206.105");
                    android.util.Log.d("NetworkManager", "DNS fallback vers IP: " + address.getHostAddress());
                    return java.util.Collections.singletonList(address);
                } catch (java.net.UnknownHostException e) {
                    android.util.Log.e("NetworkManager", "Fallback IP failed: " + e.getMessage());
                }
            }
            return okhttp3.Dns.SYSTEM.lookup(hostname);
        };

        // Configuration SSL permissive pour debug
        javax.net.ssl.X509TrustManager trustAllCerts = new javax.net.ssl.X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
            
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
            
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
        };

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .dns(customDns)
            .hostnameVerifier((hostname, session) -> true)
            .addInterceptor(loggingInterceptor);

        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, new javax.net.ssl.TrustManager[]{trustAllCerts}, new java.security.SecureRandom());
            httpClient.sslSocketFactory(sslContext.getSocketFactory(), trustAllCerts);
        } catch (Exception e) {
            android.util.Log.e("NetworkManager", "Erreur SSL Config: " + e.getMessage());
        }
        
        // URL de base du serveur Mealie
        String baseUrl = MealieApplication.getInstance().getMealieServerUrl();
        if (baseUrl.isEmpty()) {
            baseUrl = "http://localhost:9000/"; // URL par défaut
        }
        
        // Assurer que l'URL se termine par un slash
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        android.util.Log.d("NetworkManager", "Base URL: " + baseUrl);
        
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
        String token = MealieApplication.getInstance().getMealieAuthToken();
        String authHeader = token.isEmpty() ? "" : "Bearer " + token;
        android.util.Log.d("NetworkManager", "Auth header: " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
        return authHeader;
    }
    
    public String getAuthToken() {
        return MealieApplication.getInstance().getMealieAuthToken();
    }
    
    public String getBaseUrl() {
        return MealieApplication.getInstance().getMealieServerUrl();
    }
    
    public boolean isConnected() {
        // Vérifier si nous avons une URL et un token
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
    
    // Get recipes method
    public void getRecipes(RecipesCallback callback) {
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
    
    // Create recipe method
    public void createRecipe(fr.didictateur.inanutshell.data.model.Recipe recipe, CreateRecipeCallback callback) {
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
}
