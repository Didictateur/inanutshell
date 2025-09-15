package fr.didictateur.inanutshell.data.shopping;

import android.content.Context;
import androidx.lifecycle.LiveData;

import fr.didictateur.inanutshell.data.AppDatabase;
import fr.didictateur.inanutshell.data.cache.CachedRecipe;
import fr.didictateur.inanutshell.data.meal.MealPlan;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour les opérations des listes de courses
 */
public class ShoppingManager {
    
    private final AppDatabase database;
    private final ShoppingListDao shoppingListDao;
    private final ShoppingItemDao shoppingItemDao;
    private final ExecutorService executorService;
    
    public interface ShoppingCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
    
    public ShoppingManager(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.shoppingListDao = database.shoppingListDao();
        this.shoppingItemDao = database.shoppingItemDao();
        this.executorService = Executors.newFixedThreadPool(3);
    }
    
    // === Gestion des listes ===
    
    public void createShoppingList(String name, ShoppingList.GenerationSource source, 
                                 String sourceId, ShoppingCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                ShoppingList list = new ShoppingList();
                list.name = name;
                list.generationSource = source;
                list.sourceId = sourceId;
                list.createdAt = new Date();
                list.updatedAt = new Date();
                
                long id = shoppingListDao.insert(list);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void deleteShoppingList(int listId, ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                // Supprimer d'abord tous les items
                shoppingItemDao.deleteByListId(listId);
                // Puis la liste
                shoppingListDao.deleteById(listId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void completeShoppingList(int listId, ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                shoppingListDao.markAsCompleted(listId, new Date());
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void reopenShoppingList(int listId, ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                shoppingListDao.markAsActive(listId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    // === Gestion des items ===
    
    public void addItemToList(int listId, String name, ShoppingItem.Category category,
                            String quantity, String unit, String notes, 
                            ShoppingCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                ShoppingItem item = new ShoppingItem();
                item.shoppingListId = listId;
                item.name = name;
                item.category = category;
                item.quantity = quantity;
                item.unit = unit;
                item.notes = notes;
                
                long id = shoppingItemDao.insert(item);
                updateListItemCounts(listId);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void updateItemCheckedStatus(int itemId, boolean isChecked, 
                                      ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                shoppingItemDao.updateCheckedStatus(itemId, isChecked);
                
                // Mettre à jour les compteurs de la liste
                ShoppingItem item = shoppingItemDao.getById(itemId);
                if (item != null) {
                    updateListItemCounts(item.shoppingListId);
                }
                
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void updateItemQuantity(int itemId, String quantity, String unit,
                                 ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                shoppingItemDao.updateQuantity(itemId, quantity, unit);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void deleteItem(int itemId, ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                ShoppingItem item = shoppingItemDao.getById(itemId);
                if (item != null) {
                    int listId = item.shoppingListId;
                    shoppingItemDao.deleteById(itemId);
                    updateListItemCounts(listId);
                }
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void deleteCheckedItems(int listId, ShoppingCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                shoppingItemDao.deleteCheckedItems(listId);
                updateListItemCounts(listId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    // === Génération automatique ===
    
    public void generateListFromRecipe(CachedRecipe recipe, ShoppingCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                // Créer la liste
                String listName = "Courses pour: " + recipe.name;
                ShoppingList list = new ShoppingList();
                list.name = listName;
                list.generationSource = ShoppingList.GenerationSource.RECIPE;
                list.sourceId = recipe.id;
                list.createdAt = new Date();
                list.updatedAt = new Date();
                
                long listId = shoppingListDao.insert(list);
                
                // Extraire les ingrédients et créer des items
                // TODO: Parser les ingrédients JSON de la recette et créer des ShoppingItem
                // Pour l'instant, simulation basique avec le nom de la recette
                ShoppingItem item = new ShoppingItem();
                item.shoppingListId = (int) listId;
                item.name = "Ingrédients pour: " + recipe.name;
                item.category = ShoppingItem.Category.OTHER;
                item.recipeId = recipe.id;
                item.notes = "Généré depuis la recette";
                
                shoppingItemDao.insert(item);
                
                updateListItemCounts((int) listId);
                callback.onSuccess(listId);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void generateListFromMealPlan(List<MealPlan> mealPlans, String listName,
                                       ShoppingCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                // Créer la liste
                ShoppingList list = new ShoppingList();
                list.name = listName;
                list.generationSource = ShoppingList.GenerationSource.MEAL_PLAN;
                list.createdAt = new Date();
                list.updatedAt = new Date();
                
                long listId = shoppingListDao.insert(list);
                
                // Collecter les ingrédients de toutes les recettes du plan
                for (MealPlan mealPlan : mealPlans) {
                    // TODO: Récupérer la recette associée et extraire les ingrédients
                    // Pour l'instant, création d'un item générique
                    ShoppingItem item = new ShoppingItem();
                    item.shoppingListId = (int) listId;
                    item.name = "Ingrédients pour: " + mealPlan.recipeName;
                    item.category = ShoppingItem.Category.OTHER;
                    item.notes = "Repas du " + mealPlan.mealDate + " - " + mealPlan.mealType;
                    
                    shoppingItemDao.insert(item);
                }
                
                updateListItemCounts((int) listId);
                callback.onSuccess(listId);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    // === Méthodes d'accès LiveData ===
    
    public LiveData<List<ShoppingList>> getAllLists() {
        return shoppingListDao.getAllLists();
    }
    
    public LiveData<List<ShoppingList>> getActiveLists() {
        return shoppingListDao.getActiveLists();
    }
    
    public LiveData<List<ShoppingItem>> getItemsByListId(int listId) {
        return shoppingItemDao.getItemsByListId(listId);
    }
    
    public LiveData<List<ShoppingItem>> getUncheckedItemsByListId(int listId) {
        return shoppingItemDao.getUncheckedItemsByListId(listId);
    }
    
    public LiveData<List<ShoppingItem>> getItemsByCategory(int listId, ShoppingItem.Category category) {
        return shoppingItemDao.getItemsByCategory(listId, category);
    }
    
    // === Méthodes privées ===
    
    private void updateListItemCounts(int listId) {
        executorService.execute(() -> {
            try {
                int totalItems = shoppingItemDao.getTotalItemsCount(listId);
                int checkedItems = shoppingItemDao.getCheckedItemsCount(listId);
                shoppingListDao.updateItemCounts(listId, totalItems, checkedItems, new Date());
            } catch (Exception e) {
                // Log error silently
            }
        });
    }
    
    private ShoppingItem.Category categorizeIngredient(String ingredient) {
        String lower = ingredient.toLowerCase();
        
        if (lower.contains("pomme") || lower.contains("tomate") || lower.contains("salade") ||
            lower.contains("carotte") || lower.contains("légume") || lower.contains("fruit")) {
            return ShoppingItem.Category.FRUITS_VEGETABLES;
        }
        if (lower.contains("viande") || lower.contains("porc") || lower.contains("bœuf") ||
            lower.contains("poisson") || lower.contains("saumon") || lower.contains("poulet")) {
            return ShoppingItem.Category.MEAT_FISH;
        }
        if (lower.contains("lait") || lower.contains("fromage") || lower.contains("yaourt") ||
            lower.contains("beurre") || lower.contains("crème")) {
            return ShoppingItem.Category.DAIRY;
        }
        if (lower.contains("pain") || lower.contains("baguette") || lower.contains("croissant")) {
            return ShoppingItem.Category.BAKERY;
        }
        if (lower.contains("farine") || lower.contains("sucre") || lower.contains("sel") ||
            lower.contains("huile") || lower.contains("vinaigre") || lower.contains("épice")) {
            return ShoppingItem.Category.PANTRY;
        }
        
        return ShoppingItem.Category.OTHER;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
