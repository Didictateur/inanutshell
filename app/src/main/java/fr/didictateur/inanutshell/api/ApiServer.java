package fr.didictateur.inanutshell.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.model.RecipeComment;
import fr.didictateur.inanutshell.data.model.User;
import fr.didictateur.inanutshell.data.model.NutritionFacts;

public class ApiServer {
    private static ApiServer instance;
    private Context context;
    private SharedPreferences apiPrefs;
    
    // Configuration du serveur
    private boolean serverEnabled = false;
    private int serverPort = 8080;
    private String apiVersion = "v1";
    private String baseUrl = "http://localhost:8080/api/v1";
    
    // Authentification
    private Map<String, ApiToken> activeTokens = new HashMap<>();
    private Map<String, ApiClient> registeredClients = new HashMap<>();
    
    public static class ApiToken {
        public String token;
        public String clientId;
        public String userId;
        public Date createdAt;
        public Date expiresAt;
        public List<String> permissions;
        public boolean isActive;
        
        public ApiToken(String clientId, String userId) {
            this.token = UUID.randomUUID().toString().replace("-", "");
            this.clientId = clientId;
            this.userId = userId;
            this.createdAt = new Date();
            this.expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24h
            this.permissions = new ArrayList<>();
            this.isActive = true;
        }
        
        public boolean isValid() {
            return isActive && new Date().before(expiresAt);
        }
    }
    
    public static class ApiClient {
        public String clientId;
        public String clientSecret;
        public String name;
        public String description;
        public String redirectUri;
        public List<String> allowedPermissions;
        public Date createdAt;
        public boolean isActive;
        
        public ApiClient(String name, String description) {
            this.clientId = UUID.randomUUID().toString();
            this.clientSecret = UUID.randomUUID().toString();
            this.name = name;
            this.description = description;
            this.allowedPermissions = new ArrayList<>();
            this.createdAt = new Date();
            this.isActive = true;
        }
    }
    
    public static class ApiResponse<T> {
        public boolean success;
        public String message;
        public T data;
        public Map<String, Object> meta;
        public List<String> errors;
        
        public ApiResponse() {
            this.meta = new HashMap<>();
            this.errors = new ArrayList<>();
        }
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = true;
            response.data = data;
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = false;
            response.message = message;
            return response;
        }
    }
    
    public static class PaginatedResponse<T> {
        public List<T> items;
        public int page;
        public int limit;
        public int totalPages;
        public long totalItems;
        public boolean hasNext;
        public boolean hasPrevious;
        
        public PaginatedResponse(List<T> items, int page, int limit, long totalItems) {
            this.items = items;
            this.page = page;
            this.limit = limit;
            this.totalItems = totalItems;
            this.totalPages = (int) Math.ceil((double) totalItems / limit);
            this.hasNext = page < totalPages;
            this.hasPrevious = page > 1;
        }
    }
    
    private ApiServer(Context context) {
        this.context = context.getApplicationContext();
        this.apiPrefs = context.getSharedPreferences("api_server", Context.MODE_PRIVATE);
        loadConfiguration();
    }
    
    public static synchronized ApiServer getInstance(Context context) {
        if (instance == null) {
            instance = new ApiServer(context);
        }
        return instance;
    }
    
    // Configuration du serveur
    public void startServer() {
        if (serverEnabled) {
            return; // Déjà démarré
        }
        
        try {
            // TODO: Démarrer le serveur HTTP embarqué
            // En pratique, on utiliserait une bibliothèque comme NanoHTTPD
            
            serverEnabled = true;
            apiPrefs.edit().putBoolean("server_enabled", true).apply();
            
            registerDefaultRoutes();
            
        } catch (Exception e) {
            e.printStackTrace();
            serverEnabled = false;
        }
    }
    
    public void stopServer() {
        if (!serverEnabled) {
            return;
        }
        
        try {
            // TODO: Arrêter le serveur HTTP
            
            serverEnabled = false;
            apiPrefs.edit().putBoolean("server_enabled", false).apply();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean isServerRunning() {
        return serverEnabled;
    }
    
    // Gestion des clients API
    public ApiClient registerClient(String name, String description, List<String> permissions) {
        ApiClient client = new ApiClient(name, description);
        client.allowedPermissions.addAll(permissions);
        
        registeredClients.put(client.clientId, client);
        saveClientToStorage(client);
        
        return client;
    }
    
    public void revokeClient(String clientId) {
        ApiClient client = registeredClients.get(clientId);
        if (client != null) {
            client.isActive = false;
            
            // Révoquer tous les tokens de ce client
            activeTokens.entrySet().removeIf(entry -> 
                entry.getValue().clientId.equals(clientId));
            
            saveClientToStorage(client);
        }
    }
    
    // Authentification
    public ApiToken authenticateClient(String clientId, String clientSecret, String userId) {
        ApiClient client = registeredClients.get(clientId);
        if (client == null || !client.isActive || !client.clientSecret.equals(clientSecret)) {
            return null;
        }
        
        // Révoquer les anciens tokens de ce client/utilisateur
        activeTokens.entrySet().removeIf(entry -> {
            ApiToken token = entry.getValue();
            return token.clientId.equals(clientId) && token.userId.equals(userId);
        });
        
        // Créer un nouveau token
        ApiToken token = new ApiToken(clientId, userId);
        token.permissions.addAll(client.allowedPermissions);
        
        activeTokens.put(token.token, token);
        
        return token;
    }
    
    public ApiToken validateToken(String tokenString) {
        ApiToken token = activeTokens.get(tokenString);
        if (token != null && token.isValid()) {
            return token;
        }
        
        // Supprimer les tokens expirés
        if (token != null) {
            activeTokens.remove(tokenString);
        }
        
        return null;
    }
    
    public void revokeToken(String tokenString) {
        activeTokens.remove(tokenString);
    }
    
    // Endpoints API
    private void registerDefaultRoutes() {
        // Routes définies conceptuellement - en pratique il faudrait 
        // un framework HTTP pour les implémenter
        
        // GET /api/v1/recipes - Lister les recettes
        // GET /api/v1/recipes/{id} - Détails d'une recette
        // POST /api/v1/recipes - Créer une recette
        // PUT /api/v1/recipes/{id} - Modifier une recette
        // DELETE /api/v1/recipes/{id} - Supprimer une recette
        
        // GET /api/v1/recipes/{id}/comments - Commentaires d'une recette
        // POST /api/v1/recipes/{id}/comments - Ajouter un commentaire
        
        // GET /api/v1/recipes/{id}/nutrition - Infos nutritionnelles
        // POST /api/v1/recipes/{id}/nutrition - Calculer/mettre à jour nutrition
        
        // GET /api/v1/users/{id} - Profil utilisateur
        // PUT /api/v1/users/{id} - Modifier profil
        
        // GET /api/v1/search - Rechercher des recettes
    }
    
    // Implémentation des endpoints (simulée)
    public ApiResponse<PaginatedResponse<Recipe>> getRecipes(int page, int limit, String query, 
                                                           String category, String sortBy) {
        try {
            // TODO: Récupérer les recettes depuis la base de données
            List<Recipe> allRecipes = getAllRecipesFromDatabase();
            
            // Filtrer par requête de recherche
            if (query != null && !query.isEmpty()) {
                allRecipes = filterRecipesByQuery(allRecipes, query);
            }
            
            // Filtrer par catégorie
            if (category != null && !category.isEmpty()) {
                allRecipes = filterRecipesByCategory(allRecipes, category);
            }
            
            // Trier
            if (sortBy != null) {
                allRecipes = sortRecipes(allRecipes, sortBy);
            }
            
            // Paginer
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, allRecipes.size());
            
            List<Recipe> paginatedRecipes = allRecipes.subList(startIndex, endIndex);
            PaginatedResponse<Recipe> paginatedResponse = new PaginatedResponse<>(
                paginatedRecipes, page, limit, allRecipes.size());
            
            return ApiResponse.success(paginatedResponse);
            
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la récupération des recettes: " + e.getMessage());
        }
    }
    
    public ApiResponse<Recipe> getRecipe(String recipeId) {
        try {
            Recipe recipe = getRecipeFromDatabase(recipeId);
            if (recipe != null) {
                return ApiResponse.success(recipe);
            } else {
                return ApiResponse.error("Recette introuvable");
            }
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la récupération de la recette: " + e.getMessage());
        }
    }
    
    public ApiResponse<Recipe> createRecipe(Recipe recipe, String userId) {
        try {
            // Valider la recette
            if (recipe.getName() == null || recipe.getName().isEmpty()) {
                return ApiResponse.error("Le nom de la recette est requis");
            }
            
            // Assigner l'ID utilisateur
            recipe.setUserId(userId);
            recipe.setId(UUID.randomUUID().toString());
            
            // Sauvegarder en base
            saveRecipeToDatabase(recipe);
            
            return ApiResponse.success(recipe);
            
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la création de la recette: " + e.getMessage());
        }
    }
    
    public ApiResponse<Recipe> updateRecipe(String recipeId, Recipe updatedRecipe, String userId) {
        try {
            Recipe existingRecipe = getRecipeFromDatabase(recipeId);
            if (existingRecipe == null) {
                return ApiResponse.error("Recette introuvable");
            }
            
            // Vérifier les permissions
            if (!existingRecipe.getUserId().equals(userId)) {
                return ApiResponse.error("Permission refusée");
            }
            
            // Mettre à jour
            updatedRecipe.setId(recipeId);
            updatedRecipe.setUserId(userId);
            updateRecipeInDatabase(updatedRecipe);
            
            return ApiResponse.success(updatedRecipe);
            
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }
    
    public ApiResponse<String> deleteRecipe(String recipeId, String userId) {
        try {
            Recipe recipe = getRecipeFromDatabase(recipeId);
            if (recipe == null) {
                return ApiResponse.error("Recette introuvable");
            }
            
            if (!recipe.getUserId().equals(userId)) {
                return ApiResponse.error("Permission refusée");
            }
            
            deleteRecipeFromDatabase(recipeId);
            
            return ApiResponse.success("Recette supprimée avec succès");
            
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage());
        }
    }
    
    public ApiResponse<PaginatedResponse<RecipeComment>> getRecipeComments(String recipeId, int page, int limit) {
        try {
            List<RecipeComment> allComments = getCommentsFromDatabase(recipeId);
            
            // Paginer
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, allComments.size());
            
            List<RecipeComment> paginatedComments = allComments.subList(startIndex, endIndex);
            PaginatedResponse<RecipeComment> paginatedResponse = new PaginatedResponse<>(
                paginatedComments, page, limit, allComments.size());
            
            return ApiResponse.success(paginatedResponse);
            
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la récupération des commentaires: " + e.getMessage());
        }
    }
    
    public ApiResponse<NutritionFacts> getRecipeNutrition(String recipeId) {
        try {
            NutritionFacts nutrition = getNutritionFromDatabase(recipeId);
            if (nutrition != null) {
                return ApiResponse.success(nutrition);
            } else {
                return ApiResponse.error("Informations nutritionnelles non disponibles");
            }
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la récupération des infos nutritionnelles: " + e.getMessage());
        }
    }
    
    // Documentation API
    public String generateApiDocumentation() {
        StringBuilder doc = new StringBuilder();
        doc.append("# In a Nutshell API Documentation\n\n");
        doc.append("Version: ").append(apiVersion).append("\n");
        doc.append("Base URL: ").append(baseUrl).append("\n\n");
        
        doc.append("## Authentification\n\n");
        doc.append("L'API utilise des tokens Bearer pour l'authentification.\n");
        doc.append("Incluez le token dans l'en-tête Authorization:\n");
        doc.append("```\nAuthorization: Bearer YOUR_TOKEN\n```\n\n");
        
        doc.append("## Endpoints\n\n");
        
        doc.append("### Recettes\n\n");
        doc.append("- `GET /recipes` - Lister les recettes\n");
        doc.append("- `GET /recipes/{id}` - Détails d'une recette\n");
        doc.append("- `POST /recipes` - Créer une recette\n");
        doc.append("- `PUT /recipes/{id}` - Modifier une recette\n");
        doc.append("- `DELETE /recipes/{id}` - Supprimer une recette\n\n");
        
        doc.append("### Commentaires\n\n");
        doc.append("- `GET /recipes/{id}/comments` - Commentaires d'une recette\n");
        doc.append("- `POST /recipes/{id}/comments` - Ajouter un commentaire\n\n");
        
        doc.append("### Nutrition\n\n");
        doc.append("- `GET /recipes/{id}/nutrition` - Infos nutritionnelles\n");
        doc.append("- `POST /recipes/{id}/nutrition` - Calculer nutrition\n\n");
        
        doc.append("### Utilisateurs\n\n");
        doc.append("- `GET /users/{id}` - Profil utilisateur\n");
        doc.append("- `PUT /users/{id}` - Modifier profil\n\n");
        
        doc.append("### Recherche\n\n");
        doc.append("- `GET /search` - Rechercher des recettes\n\n");
        
        doc.append("## Formats de réponse\n\n");
        doc.append("Toutes les réponses suivent ce format:\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"success\": true|false,\n");
        doc.append("  \"message\": \"Message informatif\",\n");
        doc.append("  \"data\": { ... },\n");
        doc.append("  \"meta\": { ... },\n");
        doc.append("  \"errors\": [...]\n");
        doc.append("}\n");
        doc.append("```\n");
        
        return doc.toString();
    }
    
    // Méthodes utilitaires privées
    private void loadConfiguration() {
        serverEnabled = apiPrefs.getBoolean("server_enabled", false);
        serverPort = apiPrefs.getInt("server_port", 8080);
    }
    
    private void saveClientToStorage(ApiClient client) {
        // TODO: Sauvegarder en base de données ou SharedPreferences
    }
    
    // Méthodes de base de données (à implémenter)
    private List<Recipe> getAllRecipesFromDatabase() {
        // TODO: Implémenter avec Room DAO
        return new ArrayList<>();
    }
    
    private List<Recipe> filterRecipesByQuery(List<Recipe> recipes, String query) {
        // TODO: Implémenter filtrage par texte
        return recipes;
    }
    
    private List<Recipe> filterRecipesByCategory(List<Recipe> recipes, String category) {
        // TODO: Implémenter filtrage par catégorie
        return recipes;
    }
    
    private List<Recipe> sortRecipes(List<Recipe> recipes, String sortBy) {
        // TODO: Implémenter tri (nom, date, popularité, etc.)
        return recipes;
    }
    
    private Recipe getRecipeFromDatabase(String recipeId) {
        // TODO: Implémenter avec Room DAO
        return null;
    }
    
    private void saveRecipeToDatabase(Recipe recipe) {
        // TODO: Implémenter avec Room DAO
    }
    
    private void updateRecipeInDatabase(Recipe recipe) {
        // TODO: Implémenter avec Room DAO
    }
    
    private void deleteRecipeFromDatabase(String recipeId) {
        // TODO: Implémenter avec Room DAO
    }
    
    private List<RecipeComment> getCommentsFromDatabase(String recipeId) {
        // TODO: Implémenter avec Room DAO
        return new ArrayList<>();
    }
    
    private NutritionFacts getNutritionFromDatabase(String recipeId) {
        // TODO: Implémenter avec Room DAO
        return null;
    }
    
    // Getters/Setters pour la configuration
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public int getServerPort() { return serverPort; }
    public void setServerPort(int port) { 
        this.serverPort = port;
        this.baseUrl = "http://localhost:" + port + "/api/" + apiVersion;
        apiPrefs.edit().putInt("server_port", port).apply();
    }
    
    public List<ApiClient> getRegisteredClients() {
        return new ArrayList<>(registeredClients.values());
    }
    
    public List<ApiToken> getActiveTokens() {
        return new ArrayList<>(activeTokens.values());
    }
}
