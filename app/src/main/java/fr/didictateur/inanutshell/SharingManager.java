package fr.didictateur.inanutshell;

import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager pour la gestion du partage de recettes et de la collaboration
 */
public class SharingManager {
    
    private final SharedRecipeDao sharedRecipeDao;
    private final UserDao userDao;
    private final GroupDao groupDao;
    private final GroupMembershipDao membershipDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public SharingManager(AppDatabase database) {
        this.sharedRecipeDao = database.sharedRecipeDao();
        this.userDao = database.userDao();
        this.groupDao = database.groupDao();
        this.membershipDao = database.groupMembershipDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // ===================== Interfaces de callback =====================
    
    public interface ShareCallback {
        void onSuccess(SharedRecipe share);
        void onError(String message);
    }
    
    public interface ShareListCallback {
        void onSuccess(List<SharedRecipe> shares);
        void onError(String message);
    }
    
    public interface BooleanCallback {
        void onResult(boolean result);
    }
    
    // ===================== Partage individuel =====================
    
    /**
     * Partage une recette avec un utilisateur spécifique
     */
    public void shareRecipeWithUser(String recipeId, String recipeName, int sharedBy, 
                                   int sharedWith, SharedRecipe.SharePermission permission, 
                                   String message, ShareCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifications préliminaires
                User sharer = userDao.getUserById(sharedBy);
                User recipient = userDao.getUserById(sharedWith);
                
                if (sharer == null || recipient == null) {
                    mainHandler.post(() -> callback.onError("Utilisateur introuvable"));
                    return;
                }
                
                if (!sharer.hasPermission("SHARE_RECIPE")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes pour partager"));
                    return;
                }
                
                // Vérifier que la recette n'est pas déjà partagée avec cet utilisateur
                if (sharedRecipeDao.isRecipeSharedWithUser(recipeId, sharedWith)) {
                    mainHandler.post(() -> callback.onError("Recette déjà partagée avec cet utilisateur"));
                    return;
                }
                
                // Créer le partage
                SharedRecipe share = new SharedRecipe(recipeId, recipeName, sharedBy, 
                                                    SharedRecipe.ShareType.INDIVIDUAL, permission);
                share.sharedWith = sharedWith;
                share.message = message;
                
                // Si le destinataire accepte les invitations automatiquement, activer directement
                if (recipient.acceptCollaborationInvites) {
                    share.status = SharedRecipe.ShareStatus.ACCEPTED;
                }
                
                long shareId = sharedRecipeDao.insert(share);
                share.shareId = (int) shareId;
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du partage: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Partage une recette avec un groupe
     */
    public void shareRecipeWithGroup(String recipeId, String recipeName, int sharedBy, 
                                    int groupId, SharedRecipe.SharePermission permission, 
                                    String message, ShareCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifications
                User sharer = userDao.getUserById(sharedBy);
                Group group = groupDao.getGroupById(groupId);
                
                if (sharer == null || group == null) {
                    mainHandler.post(() -> callback.onError("Utilisateur ou groupe introuvable"));
                    return;
                }
                
                // Vérifier que l'utilisateur fait partie du groupe
                GroupMembership membership = membershipDao.getMembership(sharedBy, groupId);
                if (membership == null || !membership.hasAccess()) {
                    mainHandler.post(() -> callback.onError("Vous devez être membre du groupe pour y partager"));
                    return;
                }
                
                // Vérifier que le partage de recettes est activé pour le groupe
                if (!group.shareRecipes) {
                    mainHandler.post(() -> callback.onError("Le partage de recettes n'est pas activé pour ce groupe"));
                    return;
                }
                
                // Créer le partage
                SharedRecipe share = new SharedRecipe(recipeId, recipeName, sharedBy, 
                                                    SharedRecipe.ShareType.GROUP, permission);
                share.groupId = groupId;
                share.message = message;
                share.status = SharedRecipe.ShareStatus.ACCEPTED; // Automatiquement accepté pour les groupes
                
                long shareId = sharedRecipeDao.insert(share);
                share.shareId = (int) shareId;
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du partage: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Rend une recette publique
     */
    public void makeRecipePublic(String recipeId, String recipeName, int sharedBy, 
                                SharedRecipe.SharePermission permission, String message, 
                                ShareCallback callback) {
        executor.execute(() -> {
            try {
                User sharer = userDao.getUserById(sharedBy);
                if (sharer == null || !sharer.hasPermission("SHARE_RECIPE")) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                if (!sharer.shareRecipesPublically) {
                    mainHandler.post(() -> callback.onError("Partage public désactivé dans vos paramètres"));
                    return;
                }
                
                // Créer le partage public
                SharedRecipe share = new SharedRecipe(recipeId, recipeName, sharedBy, 
                                                    SharedRecipe.ShareType.PUBLIC, permission);
                share.message = message;
                share.status = SharedRecipe.ShareStatus.ACCEPTED;
                
                long shareId = sharedRecipeDao.insert(share);
                share.shareId = (int) shareId;
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du partage public: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des invitations =====================
    
    /**
     * Accepte une invitation de partage
     */
    public void acceptShareInvitation(int shareId, ShareCallback callback) {
        executor.execute(() -> {
            try {
                SharedRecipe share = sharedRecipeDao.getShareById(shareId);
                if (share == null) {
                    mainHandler.post(() -> callback.onError("Invitation introuvable"));
                    return;
                }
                
                if (share.status != SharedRecipe.ShareStatus.PENDING) {
                    mainHandler.post(() -> callback.onError("Cette invitation n'est plus valide"));
                    return;
                }
                
                share.acceptShare();
                sharedRecipeDao.update(share);
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de l'acceptation: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Refuse une invitation de partage
     */
    public void declineShareInvitation(int shareId, ShareCallback callback) {
        executor.execute(() -> {
            try {
                SharedRecipe share = sharedRecipeDao.getShareById(shareId);
                if (share == null) {
                    mainHandler.post(() -> callback.onError("Invitation introuvable"));
                    return;
                }
                
                share.declineShare();
                sharedRecipeDao.update(share);
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors du refus: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Révoque un partage existant
     */
    public void revokeShare(int shareId, int requestingUserId, ShareCallback callback) {
        executor.execute(() -> {
            try {
                SharedRecipe share = sharedRecipeDao.getShareById(shareId);
                if (share == null) {
                    mainHandler.post(() -> callback.onError("Partage introuvable"));
                    return;
                }
                
                // Seul le propriétaire peut révoquer
                if (share.sharedBy != requestingUserId) {
                    mainHandler.post(() -> callback.onError("Seul le propriétaire peut révoquer ce partage"));
                    return;
                }
                
                share.revokeShare();
                sharedRecipeDao.update(share);
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la révocation: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Gestion des permissions =====================
    
    /**
     * Met à jour les permissions d'un partage
     */
    public void updateSharePermissions(int shareId, SharedRecipe.SharePermission newPermission, 
                                     boolean allowComments, boolean allowRating, 
                                     boolean allowModifications, int requestingUserId, 
                                     ShareCallback callback) {
        executor.execute(() -> {
            try {
                SharedRecipe share = sharedRecipeDao.getShareById(shareId);
                if (share == null) {
                    mainHandler.post(() -> callback.onError("Partage introuvable"));
                    return;
                }
                
                if (share.sharedBy != requestingUserId) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                share.permission = newPermission;
                share.updateCollaborationSettings(allowComments, allowRating, allowModifications);
                sharedRecipeDao.update(share);
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la mise à jour: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Vérifie si un utilisateur peut effectuer une action sur une recette partagée
     */
    public void checkUserPermission(String recipeId, int userId, String action, BooleanCallback callback) {
        executor.execute(() -> {
            try {
                // Vérifier les partages individuels
                List<SharedRecipe> directShares = sharedRecipeDao.getEditableSharesForUser(recipeId, userId);
                if (!directShares.isEmpty()) {
                    for (SharedRecipe share : directShares) {
                        if (share.canUserPerformAction(action)) {
                            mainHandler.post(() -> callback.onResult(true));
                            return;
                        }
                    }
                }
                
                // Vérifier les partages de groupe
                List<SharedRecipe> groupShares = sharedRecipeDao.getEditableGroupSharesForUser(recipeId, userId);
                for (SharedRecipe share : groupShares) {
                    if (share.canUserPerformAction(action)) {
                        mainHandler.post(() -> callback.onResult(true));
                        return;
                    }
                }
                
                mainHandler.post(() -> callback.onResult(false));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }
    
    // ===================== Récupération des recettes partagées =====================
    
    /**
     * Obtient toutes les recettes partagées avec un utilisateur
     */
    public void getSharedRecipesForUser(int userId, ShareListCallback callback) {
        executor.execute(() -> {
            try {
                List<SharedRecipe> shares = sharedRecipeDao.getAllAccessibleShares(userId);
                mainHandler.post(() -> callback.onSuccess(shares));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Obtient les recettes publiques
     */
    public void getPublicRecipes(ShareListCallback callback) {
        executor.execute(() -> {
            try {
                List<SharedRecipe> shares = sharedRecipeDao.getPublicShares();
                mainHandler.post(() -> callback.onSuccess(shares));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la récupération: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Recherche dans les recettes partagées
     */
    public void searchSharedRecipes(int userId, String query, ShareListCallback callback) {
        executor.execute(() -> {
            try {
                List<SharedRecipe> userShares = sharedRecipeDao.searchUserSharedRecipes(userId, query);
                List<SharedRecipe> groupShares = sharedRecipeDao.searchGroupSharedRecipes(userId, query);
                
                userShares.addAll(groupShares);
                mainHandler.post(() -> callback.onSuccess(userShares));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la recherche: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Collaboration =====================
    
    /**
     * Crée un partage collaboratif avec permissions étendues
     */
    public void createCollaborativeShare(String recipeId, String recipeName, int sharedBy, 
                                       int sharedWith, String message, ShareCallback callback) {
        SharedRecipe.SharePermission collaborativePermission = SharedRecipe.SharePermission.EDIT;
        
        executor.execute(() -> {
            try {
                SharedRecipe share = new SharedRecipe(recipeId, recipeName, sharedBy, 
                                                    SharedRecipe.ShareType.COLLABORATIVE, collaborativePermission);
                share.sharedWith = sharedWith;
                share.message = message;
                share.updateCollaborationSettings(true, true, true); // Tout autorisé
                
                long shareId = sharedRecipeDao.insert(share);
                share.shareId = (int) shareId;
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la création collaborative: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Définit une expiration pour un partage
     */
    public void setShareExpiry(int shareId, int days, int requestingUserId, ShareCallback callback) {
        executor.execute(() -> {
            try {
                SharedRecipe share = sharedRecipeDao.getShareById(shareId);
                if (share == null) {
                    mainHandler.post(() -> callback.onError("Partage introuvable"));
                    return;
                }
                
                if (share.sharedBy != requestingUserId) {
                    mainHandler.post(() -> callback.onError("Permissions insuffisantes"));
                    return;
                }
                
                share.setExpiryDays(days);
                sharedRecipeDao.update(share);
                
                mainHandler.post(() -> callback.onSuccess(share));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Erreur lors de la définition de l'expiration: " + e.getMessage()));
            }
        });
    }
    
    // ===================== Maintenance =====================
    
    /**
     * Expire automatiquement les anciens partages
     */
    public void expireOldShares() {
        executor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                int expiredCount = sharedRecipeDao.expireOldShares(currentTime);
                // Log le nombre de partages expirés
            } catch (Exception e) {
                // Log l'erreur
            }
        });
    }
    
    /**
     * Nettoie les anciens partages
     */
    public void cleanupOldShares(int daysOld) {
        executor.execute(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
                int cleanedCount = sharedRecipeDao.cleanupOldShares(cutoffTime);
                // Log le nombre de partages nettoyés
            } catch (Exception e) {
                // Log l'erreur
            }
        });
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        executor.shutdown();
    }
}
