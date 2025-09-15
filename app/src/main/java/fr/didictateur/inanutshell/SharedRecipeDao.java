package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.Date;
import java.util.List;

/**
 * DAO pour les opérations sur les recettes partagées
 */
@Dao
public interface SharedRecipeDao {

    // ===================== Opérations CRUD de base =====================

    @Insert
    long insert(SharedRecipe sharedRecipe);

    @Update
    void update(SharedRecipe sharedRecipe);

    @Delete
    void delete(SharedRecipe sharedRecipe);

    @Query("SELECT * FROM shared_recipes WHERE shareId = :shareId")
    SharedRecipe getShareById(int shareId);

    @Query("SELECT * FROM shared_recipes WHERE shareId = :shareId")
    LiveData<SharedRecipe> getShareByIdLive(int shareId);

    // ===================== Recettes partagées par utilisateur =====================

    @Query("SELECT * FROM shared_recipes WHERE sharedBy = :userId AND status = 'ACCEPTED'")
    List<SharedRecipe> getRecipesSharedBy(int userId);

    @Query("SELECT * FROM shared_recipes WHERE sharedBy = :userId AND status = 'ACCEPTED'")
    LiveData<List<SharedRecipe>> getRecipesSharedByLive(int userId);

    @Query("SELECT * FROM shared_recipes WHERE sharedWith = :userId AND status = 'ACCEPTED'")
    List<SharedRecipe> getRecipesSharedWith(int userId);

    @Query("SELECT * FROM shared_recipes WHERE sharedWith = :userId AND status = 'ACCEPTED'")
    LiveData<List<SharedRecipe>> getRecipesSharedWithLive(int userId);

    // ===================== Recettes partagées dans les groupes =====================

    @Query("SELECT * FROM shared_recipes WHERE groupId = :groupId AND status = 'ACCEPTED'")
    List<SharedRecipe> getGroupSharedRecipes(int groupId);

    @Query("SELECT * FROM shared_recipes WHERE groupId = :groupId AND status = 'ACCEPTED'")
    LiveData<List<SharedRecipe>> getGroupSharedRecipesLive(int groupId);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "INNER JOIN group_memberships gm ON sr.groupId = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.status = 'ACTIVE' " +
           "AND sr.status = 'ACCEPTED'")
    List<SharedRecipe> getUserGroupSharedRecipes(int userId);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "INNER JOIN group_memberships gm ON sr.groupId = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.status = 'ACTIVE' " +
           "AND sr.status = 'ACCEPTED'")
    LiveData<List<SharedRecipe>> getUserGroupSharedRecipesLive(int userId);

    // ===================== Gestion des invitations =====================

    @Query("SELECT * FROM shared_recipes WHERE sharedWith = :userId AND status = 'PENDING'")
    List<SharedRecipe> getPendingInvitations(int userId);

    @Query("SELECT * FROM shared_recipes WHERE sharedWith = :userId AND status = 'PENDING'")
    LiveData<List<SharedRecipe>> getPendingInvitationsLive(int userId);

    @Query("SELECT * FROM shared_recipes WHERE sharedBy = :userId AND status = 'PENDING'")
    List<SharedRecipe> getSentInvitations(int userId);

    @Query("UPDATE shared_recipes SET status = 'ACCEPTED' WHERE shareId = :shareId")
    void acceptShare(int shareId);

    @Query("UPDATE shared_recipes SET status = 'DECLINED' WHERE shareId = :shareId")
    void declineShare(int shareId);

    @Query("UPDATE shared_recipes SET status = 'REVOKED' WHERE shareId = :shareId")
    void revokeShare(int shareId);

    // ===================== Recettes spécifiques =====================

    @Query("SELECT * FROM shared_recipes WHERE recipeId = :recipeId AND status = 'ACCEPTED'")
    List<SharedRecipe> getSharesForRecipe(String recipeId);

    @Query("SELECT * FROM shared_recipes WHERE recipeId = :recipeId AND sharedBy = :userId")
    List<SharedRecipe> getUserSharesForRecipe(String recipeId, int userId);

    @Query("SELECT COUNT(*) > 0 FROM shared_recipes " +
           "WHERE recipeId = :recipeId AND sharedWith = :userId AND status = 'ACCEPTED'")
    boolean isRecipeSharedWithUser(String recipeId, int userId);

    @Query("SELECT COUNT(*) > 0 FROM shared_recipes sr " +
           "INNER JOIN group_memberships gm ON sr.groupId = gm.groupId " +
           "WHERE sr.recipeId = :recipeId AND gm.userId = :userId " +
           "AND gm.status = 'ACTIVE' AND sr.status = 'ACCEPTED'")
    boolean isRecipeSharedInUserGroups(String recipeId, int userId);

    // ===================== Gestion des permissions =====================

    @Query("SELECT * FROM shared_recipes " +
           "WHERE recipeId = :recipeId AND sharedWith = :userId " +
           "AND status = 'ACCEPTED' AND permission IN ('EDIT', 'FULL_ACCESS')")
    List<SharedRecipe> getEditableSharesForUser(String recipeId, int userId);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "INNER JOIN group_memberships gm ON sr.groupId = gm.groupId " +
           "WHERE sr.recipeId = :recipeId AND gm.userId = :userId " +
           "AND gm.status = 'ACTIVE' AND sr.status = 'ACCEPTED' " +
           "AND sr.permission IN ('EDIT', 'FULL_ACCESS')")
    List<SharedRecipe> getEditableGroupSharesForUser(String recipeId, int userId);

    @Query("UPDATE shared_recipes SET permission = :permission WHERE shareId = :shareId")
    void updateSharePermission(int shareId, SharedRecipe.SharePermission permission);

    // ===================== Gestion des types de partage =====================

    @Query("SELECT * FROM shared_recipes WHERE shareType = :shareType AND status = 'ACCEPTED'")
    List<SharedRecipe> getSharesByType(SharedRecipe.ShareType shareType);

    @Query("SELECT * FROM shared_recipes WHERE shareType = 'PUBLIC' AND status = 'ACCEPTED'")
    List<SharedRecipe> getPublicShares();

    @Query("SELECT * FROM shared_recipes WHERE shareType = 'PUBLIC' AND status = 'ACCEPTED'")
    LiveData<List<SharedRecipe>> getPublicSharesLive();

    // ===================== Recherche et filtrage =====================

    @Query("SELECT * FROM shared_recipes " +
           "WHERE (recipeName LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%') " +
           "AND status = 'ACCEPTED'")
    List<SharedRecipe> searchSharedRecipes(String query);

    @Query("SELECT * FROM shared_recipes " +
           "WHERE sharedWith = :userId AND status = 'ACCEPTED' " +
           "AND (recipeName LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%')")
    List<SharedRecipe> searchUserSharedRecipes(int userId, String query);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "INNER JOIN group_memberships gm ON sr.groupId = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.status = 'ACTIVE' " +
           "AND sr.status = 'ACCEPTED' " +
           "AND (sr.recipeName LIKE '%' || :query || '%' OR sr.message LIKE '%' || :query || '%')")
    List<SharedRecipe> searchGroupSharedRecipes(int userId, String query);

    // ===================== Gestion des expirations =====================

    @Query("UPDATE shared_recipes SET status = 'EXPIRED' " +
           "WHERE expiryDate < :currentDate AND status = 'ACCEPTED'")
    int expireOldShares(long currentDate);

    @Query("SELECT * FROM shared_recipes " +
           "WHERE expiryDate IS NOT NULL AND expiryDate > :currentDate " +
           "AND status = 'ACCEPTED'")
    List<SharedRecipe> getExpiringShares(long currentDate);

    @Query("SELECT * FROM shared_recipes " +
           "WHERE sharedWith = :userId AND expiryDate BETWEEN :currentDate AND :futureDate " +
           "AND status = 'ACCEPTED'")
    List<SharedRecipe> getUserExpiringShares(int userId, long currentDate, long futureDate);

    // ===================== Statistiques =====================

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE sharedBy = :userId")
    int getShareCountByUser(int userId);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE sharedWith = :userId AND status = 'ACCEPTED'")
    int getReceivedShareCount(int userId);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE groupId = :groupId AND status = 'ACCEPTED'")
    int getGroupShareCount(int groupId);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE status = :status")
    int getShareCountByStatus(SharedRecipe.ShareStatus status);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE shareType = :shareType AND status = 'ACCEPTED'")
    int getShareCountByType(SharedRecipe.ShareType shareType);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE recipeId = :recipeId AND status = 'ACCEPTED'")
    int getRecipeShareCount(String recipeId);

    // ===================== Collaboration =====================

    @Query("UPDATE shared_recipes SET allowComments = :allow WHERE shareId = :shareId")
    void updateCommentsAllowed(int shareId, boolean allow);

    @Query("UPDATE shared_recipes SET allowRating = :allow WHERE shareId = :shareId")
    void updateRatingAllowed(int shareId, boolean allow);

    @Query("UPDATE shared_recipes SET allowModifications = :allow WHERE shareId = :shareId")
    void updateModificationsAllowed(int shareId, boolean allow);

    @Query("UPDATE shared_recipes SET collaborationNotes = :notes WHERE shareId = :shareId")
    void updateCollaborationNotes(int shareId, String notes);

    @Query("SELECT * FROM shared_recipes " +
           "WHERE allowModifications = 1 AND status = 'ACCEPTED' " +
           "AND permission IN ('SUGGEST', 'EDIT', 'FULL_ACCESS')")
    List<SharedRecipe> getCollaborativeShares();

    // ===================== Nettoyage et maintenance =====================

    @Query("DELETE FROM shared_recipes " +
           "WHERE status IN ('DECLINED', 'REVOKED', 'EXPIRED') " +
           "AND dateShared < :cutoffDate")
    int cleanupOldShares(long cutoffDate);

    @Query("SELECT COUNT(*) FROM shared_recipes WHERE status = 'PENDING' AND dateShared < :cutoffDate")
    int getPendingSharesOlderThan(long cutoffDate);

    // ===================== Requêtes avancées =====================

    @Query("SELECT DISTINCT recipeId FROM shared_recipes " +
           "WHERE (sharedWith = :userId OR " +
           "groupId IN (SELECT groupId FROM group_memberships WHERE userId = :userId AND status = 'ACTIVE')) " +
           "AND status = 'ACCEPTED'")
    List<String> getAccessibleRecipeIds(int userId);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "WHERE (sr.sharedWith = :userId OR " +
           "sr.groupId IN (SELECT gm.groupId FROM group_memberships gm WHERE gm.userId = :userId AND gm.status = 'ACTIVE')) " +
           "AND sr.status = 'ACCEPTED' " +
           "ORDER BY sr.dateShared DESC")
    List<SharedRecipe> getAllAccessibleShares(int userId);

    @Query("SELECT sr.* FROM shared_recipes sr " +
           "WHERE (sr.sharedWith = :userId OR " +
           "sr.groupId IN (SELECT gm.groupId FROM group_memberships gm WHERE gm.userId = :userId AND gm.status = 'ACTIVE')) " +
           "AND sr.status = 'ACCEPTED' " +
           "ORDER BY sr.dateShared DESC")
    LiveData<List<SharedRecipe>> getAllAccessibleSharesLive(int userId);
}
