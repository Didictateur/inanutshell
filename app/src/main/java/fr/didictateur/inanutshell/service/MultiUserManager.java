package fr.didictateur.inanutshell.service;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.didictateur.inanutshell.User;
import fr.didictateur.inanutshell.data.model.Recipe;

public class MultiUserManager {
    private static MultiUserManager instance;
    private Context context;
    private ExecutorService executorService;
    
    // Utilisateur actuellement connecté
    private MutableLiveData<User> currentUserLiveData = new MutableLiveData<>();
    private MutableLiveData<List<User>> followersLiveData = new MutableLiveData<>();
    private MutableLiveData<List<User>> followingLiveData = new MutableLiveData<>();
    
    // Cache des utilisateurs
    private Map<String, User> usersCache = new HashMap<>();
    private Map<String, List<String>> userFollowers = new HashMap<>();
    private Map<String, List<String>> userFollowing = new HashMap<>();
    
    public enum FollowAction {
        FOLLOW,
        UNFOLLOW,
        BLOCK,
        UNBLOCK
    }
    
    public static class UserRelation {
        public String userId;
        public String targetUserId;
        public RelationType type;
        public Date createdAt;
        
        public enum RelationType {
            FOLLOWING,
            BLOCKED,
            MUTED
        }
    }
    
    public static class CollaborativeRecipe {
        public String recipeId;
        public String ownerId;
        public List<String> collaboratorIds;
        public Map<String, CollaboratorRole> collaboratorRoles;
        public boolean publicCollaboration;
        public Date createdAt;
        public Date updatedAt;
        
        public enum CollaboratorRole {
            EDITOR,      // Peut modifier la recette
            REVIEWER,    // Peut commenter et suggérer
            VIEWER       // Peut seulement voir
        }
    }
    
    private MultiUserManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        loadCurrentUser();
    }
    
    public static synchronized MultiUserManager getInstance(Context context) {
        if (instance == null) {
            instance = new MultiUserManager(context);
        }
        return instance;
    }
    
    // Gestion des utilisateurs
    public void switchUser(String userId, UserCallback callback) {
        executorService.execute(() -> {
            try {
                User user = getUserById(userId);
                if (user != null && user.canInteract()) {
                    user.updateLastLogin();
                    currentUserLiveData.postValue(user);
                    
                    // Charger les données associées
                    loadUserRelations(userId);
                    
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Utilisateur introuvable ou inactif");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors du changement d'utilisateur: " + e.getMessage());
                }
            }
        });
    }
    
    public void updateUserProfile(String userId, User updatedUser, UserCallback callback) {
        executorService.execute(() -> {
            try {
                User currentUser = getCurrentUser();
                if (currentUser == null || (!String.valueOf(currentUser.getId()).equals(userId) && !currentUser.canManageUsers())) {
                    if (callback != null) {
                        callback.onError("Permission refusée");
                    }
                    return;
                }
                
                // Mise à jour de l'utilisateur
                updateUserInDatabase(updatedUser);
                usersCache.put(userId, updatedUser);
                
                // Notifier si c'est l'utilisateur actuel
                if (String.valueOf(currentUser.getId()).equals(userId)) {
                    currentUserLiveData.postValue(updatedUser);
                }
                
                if (callback != null) {
                    callback.onSuccess(updatedUser);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de la mise à jour: " + e.getMessage());
                }
            }
        });
    }
    
    // Gestion des relations utilisateur
    public void followUser(String targetUserId, UserCallback callback) {
        performFollowAction(targetUserId, FollowAction.FOLLOW, callback);
    }
    
    public void unfollowUser(String targetUserId, UserCallback callback) {
        performFollowAction(targetUserId, FollowAction.UNFOLLOW, callback);
    }
    
    public void blockUser(String targetUserId, UserCallback callback) {
        performFollowAction(targetUserId, FollowAction.BLOCK, callback);
    }
    
    public void unblockUser(String targetUserId, UserCallback callback) {
        performFollowAction(targetUserId, FollowAction.UNBLOCK, callback);
    }
    
    private void performFollowAction(String targetUserId, FollowAction action, UserCallback callback) {
        executorService.execute(() -> {
            try {
                User currentUser = getCurrentUser();
                if (currentUser == null) {
                    if (callback != null) {
                        callback.onError("Utilisateur non connecté");
                    }
                    return;
                }
                
                User targetUser = getUserById(targetUserId);
                if (targetUser == null) {
                    if (callback != null) {
                        callback.onError("Utilisateur cible introuvable");
                    }
                    return;
                }
                
                switch (action) {
                    case FOLLOW:
                        if (canFollowUser(currentUser, targetUser)) {
                            addFollowRelation(String.valueOf(currentUser.getId()), targetUserId);
                            targetUser.incrementFollowerCount();
                            currentUser.incrementFollowingCount();
                        }
                        break;
                        
                    case UNFOLLOW:
                        removeFollowRelation(String.valueOf(currentUser.getId()), targetUserId);
                        targetUser.decrementFollowerCount();
                        currentUser.decrementFollowingCount();
                        break;
                        
                    case BLOCK:
                        addBlockRelation(String.valueOf(currentUser.getId()), targetUserId);
                        // Supprimer le suivi mutuel si existant
                        removeFollowRelation(String.valueOf(currentUser.getId()), targetUserId);
                        removeFollowRelation(targetUserId, String.valueOf(currentUser.getId()));
                        break;
                        
                    case UNBLOCK:
                        removeBlockRelation(String.valueOf(currentUser.getId()), targetUserId);
                        break;
                }
                
                // Mettre à jour les caches
                updateUserInDatabase(currentUser);
                updateUserInDatabase(targetUser);
                usersCache.put(String.valueOf(currentUser.getId()), currentUser);
                usersCache.put(targetUserId, targetUser);
                
                // Recharger les relations
                loadUserRelations(String.valueOf(currentUser.getId()));
                
                if (callback != null) {
                    callback.onSuccess(targetUser);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de l'action: " + e.getMessage());
                }
            }
        });
    }
    
    // Gestion collaborative des recettes
    public void shareRecipeWithUsers(String recipeId, List<String> userIds, 
                                   CollaborativeRecipe.CollaboratorRole role, 
                                   CollaborationCallback callback) {
        executorService.execute(() -> {
            try {
                User currentUser = getCurrentUser();
                if (currentUser == null) {
                    if (callback != null) {
                        callback.onError("Utilisateur non connecté");
                    }
                    return;
                }
                
                // Vérifier que l'utilisateur possède la recette
                Recipe recipe = getRecipeById(recipeId);
                if (recipe == null || !recipe.getUserId().equals(currentUser.getId())) {
                    if (callback != null) {
                        callback.onError("Permission refusée ou recette introuvable");
                    }
                    return;
                }
                
                // Créer la collaboration
                CollaborativeRecipe collab = new CollaborativeRecipe();
                collab.recipeId = recipeId;
                collab.ownerId = String.valueOf(currentUser.getId());
                collab.collaboratorIds = new ArrayList<>(userIds);
                collab.collaboratorRoles = new HashMap<>();
                collab.createdAt = new Date();
                collab.updatedAt = new Date();
                
                for (String userId : userIds) {
                    collab.collaboratorRoles.put(userId, role);
                }
                
                // Sauvegarder la collaboration
                saveCollaborativeRecipe(collab);
                
                // Notifier les collaborateurs
                notifyCollaborators(collab, "Vous avez été ajouté comme collaborateur à la recette: " + recipe.getName());
                
                if (callback != null) {
                    callback.onSuccess(collab);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors du partage: " + e.getMessage());
                }
            }
        });
    }
    
    public void updateCollaboratorRole(String recipeId, String userId, 
                                     CollaborativeRecipe.CollaboratorRole newRole,
                                     CollaborationCallback callback) {
        executorService.execute(() -> {
            try {
                User currentUser = getCurrentUser();
                CollaborativeRecipe collab = getCollaborativeRecipe(recipeId);
                
                if (collab == null || !collab.ownerId.equals(currentUser.getId())) {
                    if (callback != null) {
                        callback.onError("Permission refusée");
                    }
                    return;
                }
                
                collab.collaboratorRoles.put(userId, newRole);
                collab.updatedAt = new Date();
                
                updateCollaborativeRecipe(collab);
                
                if (callback != null) {
                    callback.onSuccess(collab);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de la mise à jour: " + e.getMessage());
                }
            }
        });
    }
    
    public void removeCollaborator(String recipeId, String userId, CollaborationCallback callback) {
        executorService.execute(() -> {
            try {
                User currentUser = getCurrentUser();
                CollaborativeRecipe collab = getCollaborativeRecipe(recipeId);
                
                if (collab == null || !collab.ownerId.equals(currentUser.getId())) {
                    if (callback != null) {
                        callback.onError("Permission refusée");
                    }
                    return;
                }
                
                collab.collaboratorIds.remove(userId);
                collab.collaboratorRoles.remove(userId);
                collab.updatedAt = new Date();
                
                updateCollaborativeRecipe(collab);
                
                if (callback != null) {
                    callback.onSuccess(collab);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }
    
    // Getters LiveData
    public LiveData<User> getCurrentUserLiveData() {
        return currentUserLiveData;
    }
    
    public LiveData<List<User>> getFollowersLiveData() {
        return followersLiveData;
    }
    
    public LiveData<List<User>> getFollowingLiveData() {
        return followingLiveData;
    }
    
    public User getCurrentUser() {
        return currentUserLiveData.getValue();
    }
    
    public boolean isFollowing(String targetUserId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        
        List<String> following = userFollowing.get(currentUser.getId());
        return following != null && following.contains(targetUserId);
    }
    
    public boolean canEditRecipe(String recipeId, String userId) {
        // Le propriétaire peut toujours modifier
        Recipe recipe = getRecipeById(recipeId);
        if (recipe != null && recipe.getUserId().equals(userId)) {
            return true;
        }
        
        // Vérifier les permissions de collaboration
        CollaborativeRecipe collab = getCollaborativeRecipe(recipeId);
        if (collab != null && collab.collaboratorIds.contains(userId)) {
            CollaborativeRecipe.CollaboratorRole role = collab.collaboratorRoles.get(userId);
            return role == CollaborativeRecipe.CollaboratorRole.EDITOR;
        }
        
        return false;
    }
    
    // Méthodes privées d'aide
    private void loadCurrentUser() {
        // TODO: Charger l'utilisateur depuis les préférences ou la base
        // Pour l'instant, utilisateur par défaut
        User defaultUser = createDefaultUser();
        currentUserLiveData.setValue(defaultUser);
    }
    
    private User createDefaultUser() {
        User user = new User();
        user.username = "Utilisateur";
        user.email = "user@example.com";
        return user;
    }
    
    private boolean canFollowUser(User currentUser, User targetUser) {
        // Vérifier les paramètres de confidentialité
        return targetUser.getPrivacySettings().allowFollows && 
               !isBlocked(String.valueOf(currentUser.getId()), String.valueOf(targetUser.getId()));
    }
    
    private boolean isBlocked(String userId, String targetUserId) {
        // TODO: Vérifier si l'utilisateur est bloqué
        return false;
    }
    
    private void loadUserRelations(String userId) {
        // TODO: Charger les relations depuis la base de données
        followersLiveData.postValue(new ArrayList<>());
        followingLiveData.postValue(new ArrayList<>());
    }
    
    // Méthodes de persistance (à implémenter)
    private User getUserById(String userId) {
        return usersCache.get(userId);
    }
    
    private Recipe getRecipeById(String recipeId) {
        // TODO: Récupérer la recette depuis la base
        return null;
    }
    
    private void updateUserInDatabase(User user) {
        // TODO: Mettre à jour en base
    }
    
    private void addFollowRelation(String userId, String targetUserId) {
        List<String> following = userFollowing.computeIfAbsent(userId, k -> new ArrayList<>());
        if (!following.contains(targetUserId)) {
            following.add(targetUserId);
        }
        
        List<String> followers = userFollowers.computeIfAbsent(targetUserId, k -> new ArrayList<>());
        if (!followers.contains(userId)) {
            followers.add(userId);
        }
    }
    
    private void removeFollowRelation(String userId, String targetUserId) {
        List<String> following = userFollowing.get(userId);
        if (following != null) {
            following.remove(targetUserId);
        }
        
        List<String> followers = userFollowers.get(targetUserId);
        if (followers != null) {
            followers.remove(userId);
        }
    }
    
    private void addBlockRelation(String userId, String targetUserId) {
        // TODO: Implémenter le blocage
    }
    
    private void removeBlockRelation(String userId, String targetUserId) {
        // TODO: Implémenter le déblocage
    }
    
    private void saveCollaborativeRecipe(CollaborativeRecipe collab) {
        // TODO: Sauvegarder en base
    }
    
    private void updateCollaborativeRecipe(CollaborativeRecipe collab) {
        // TODO: Mettre à jour en base
    }
    
    private CollaborativeRecipe getCollaborativeRecipe(String recipeId) {
        // TODO: Récupérer depuis la base
        return null;
    }
    
    private void notifyCollaborators(CollaborativeRecipe collab, String message) {
        // TODO: Envoyer des notifications
    }
    
    // Interfaces pour les callbacks
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    
    public interface CollaborationCallback {
        void onSuccess(CollaborativeRecipe collaboration);
        void onError(String message);
    }
}
