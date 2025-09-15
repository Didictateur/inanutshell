package fr.didictateur.inanutshell.data.manager;

import android.util.Log;
import fr.didictateur.inanutshell.data.model.Category;
import fr.didictateur.inanutshell.data.model.Tag;
import fr.didictateur.inanutshell.data.network.NetworkManager;
import java.util.List;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryTagManager {
    private static final String TAG = "CategoryTagManager";
    
    private static CategoryTagManager instance;
    private NetworkManager networkManager;
    
    // Cache local des catégories et tags
    private List<Category> allCategories = new ArrayList<>();
    private List<Tag> allTags = new ArrayList<>();
    
    private boolean categoriesLoaded = false;
    private boolean tagsLoaded = false;
    
    private CategoryTagManager() {
        networkManager = NetworkManager.getInstance();
    }
    
    public static synchronized CategoryTagManager getInstance() {
        if (instance == null) {
            instance = new CategoryTagManager();
        }
        return instance;
    }
    
    // Méthodes publiques pour récupérer les données
    public void getCategories(CategoriesCallback callback) {
        loadCategories(callback);
    }
    
    public void getTags(TagsCallback callback) {
        loadTags(callback);
    }
    
    // Interfaces de callback
    public interface CategoriesCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }
    
    public interface TagsCallback {
        void onSuccess(List<Tag> tags);
        void onError(String error);
    }
    
    public interface CategoryCallback {
        void onSuccess(Category category);
        void onError(String error);
    }
    
    public interface TagCallback {
        void onSuccess(Tag tag);
        void onError(String error);
    }
    
    // Récupérer toutes les catégories
    public void loadCategories(final CategoriesCallback callback) {
        Call<List<Category>> call = NetworkManager.getInstance().getApiService().getCategories(
            NetworkManager.getInstance().getAuthHeader()
        );
        
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();
                    callback.onSuccess(allCategories);
                } else if (response.code() == 404) {
                    // Aucune catégorie n'existe encore, retourner une liste vide
                    Log.d(TAG, "Aucune catégorie trouvée (404), initialisation avec liste vide");
                    allCategories = new ArrayList<>();
                    callback.onSuccess(allCategories);
                } else {
                    Log.e(TAG, "Erreur lors du chargement des catégories: " + response.code());
                    callback.onError("Erreur lors du chargement des catégories: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e(TAG, "Échec du chargement des catégories", t);
                callback.onError("Échec du chargement des catégories: " + t.getMessage());
            }
        });
    }
    
    // Récupérer tous les tags
    public void loadTags(final TagsCallback callback) {
        Call<List<Tag>> call = NetworkManager.getInstance().getApiService().getTags(
            NetworkManager.getInstance().getAuthHeader()
        );
        
        call.enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTags = response.body();
                    callback.onSuccess(allTags);
                } else if (response.code() == 404) {
                    // Aucun tag n'existe encore, retourner une liste vide
                    Log.d(TAG, "Aucun tag trouvé (404), initialisation avec liste vide");
                    allTags = new ArrayList<>();
                    callback.onSuccess(allTags);
                } else {
                    Log.e(TAG, "Erreur lors du chargement des tags: " + response.code());
                    callback.onError("Erreur lors du chargement des tags: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Tag>> call, Throwable t) {
                Log.e(TAG, "Échec du chargement des tags", t);
                callback.onError("Échec du chargement des tags: " + t.getMessage());
            }
        });
    }
    
    // Méthodes utilitaires pour le cache
    public List<Category> getCachedCategories() {
        return new ArrayList<>(allCategories);
    }
    
    public List<Tag> getCachedTags() {
        return new ArrayList<>(allTags);
    }
    
    public Category getCategoryById(String id) {
        for (Category category : allCategories) {
            if (category.getId().equals(id)) {
                return category;
            }
        }
        return null;
    }
    
    public Tag getTagById(String id) {
        for (Tag tag : allTags) {
            if (tag.getId().equals(id)) {
                return tag;
            }
        }
        return null;
    }
    
    public Category getCategoryByName(String name) {
        for (Category category : allCategories) {
            if (category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
    
    public Tag getTagByName(String name) {
        for (Tag tag : allTags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        return null;
    }
    
    // Créer un nouveau tag
    public void createTag(String name, final TagCreateCallback callback) {
        Tag newTag = new Tag();
        newTag.setName(name);
        
        Call<Tag> call = NetworkManager.getInstance().getApiService().createTag(
            NetworkManager.getInstance().getAuthHeader(), newTag
        );
        
        call.enqueue(new Callback<Tag>() {
            @Override
            public void onResponse(Call<Tag> call, Response<Tag> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Tag createdTag = response.body();
                    // Ajouter à la liste locale
                    if (allTags != null) {
                        allTags.add(createdTag);
                    }
                    Log.d(TAG, "Tag créé avec succès: " + createdTag.getName());
                    callback.onSuccess(createdTag);
                } else {
                    Log.e(TAG, "Erreur lors de la création du tag: " + response.code());
                    callback.onError("Erreur lors de la création du tag: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Tag> call, Throwable t) {
                Log.e(TAG, "Échec de la création du tag", t);
                callback.onError("Échec de la création du tag: " + t.getMessage());
            }
        });
    }
    
    // Créer une nouvelle catégorie
    public void createCategory(String name, final CategoryCreateCallback callback) {
        Category newCategory = new Category();
        newCategory.setName(name);
        
        Call<Category> call = NetworkManager.getInstance().getApiService().createCategory(
            NetworkManager.getInstance().getAuthHeader(), newCategory
        );
        
        call.enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Category createdCategory = response.body();
                    // Ajouter à la liste locale
                    if (allCategories != null) {
                        allCategories.add(createdCategory);
                    }
                    Log.d(TAG, "Catégorie créée avec succès: " + createdCategory.getName());
                    callback.onSuccess(createdCategory);
                } else {
                    Log.e(TAG, "Erreur lors de la création de la catégorie: " + response.code());
                    callback.onError("Erreur lors de la création de la catégorie: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                Log.e(TAG, "Échec de la création de la catégorie", t);
                callback.onError("Échec de la création de la catégorie: " + t.getMessage());
            }
        });
    }
    
    // Forcer le rechargement du cache
    public void refreshCache() {
        categoriesLoaded = false;
        tagsLoaded = false;
        allCategories.clear();
        allTags.clear();
    }
    
    // Vérifier si les données sont chargées
    public boolean areCategoriesLoaded() {
        return categoriesLoaded;
    }
    
    public boolean areTagsLoaded() {
        return tagsLoaded;
    }
    
    // Callbacks pour la création
    public interface TagCreateCallback {
        void onSuccess(Tag tag);
        void onError(String error);
    }
    
    public interface CategoryCreateCallback {
        void onSuccess(Category category);
        void onError(String error);
    }
}
