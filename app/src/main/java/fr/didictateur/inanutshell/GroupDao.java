package fr.didictateur.inanutshell;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * DAO pour les opérations sur les groupes et la gestion collaborative
 */
@Dao
public interface GroupDao {

    // ===================== Opérations CRUD de base =====================

    @Insert
    long insert(Group group);

    @Update
    void update(Group group);

    @Delete
    void delete(Group group);

    @Query("SELECT * FROM groups WHERE groupId = :groupId")
    Group getGroupById(int groupId);

    @Query("SELECT * FROM groups WHERE groupId = :groupId")
    LiveData<Group> getGroupByIdLive(int groupId);

    @Query("SELECT * FROM groups WHERE isActive = 1")
    List<Group> getAllActiveGroups();

    @Query("SELECT * FROM groups WHERE isActive = 1")
    LiveData<List<Group>> getAllActiveGroupsLive();

    // ===================== Gestion des propriétaires =====================

    @Query("SELECT * FROM groups WHERE ownerId = :ownerId AND isActive = 1")
    List<Group> getGroupsByOwner(int ownerId);

    @Query("SELECT * FROM groups WHERE ownerId = :ownerId AND isActive = 1")
    LiveData<List<Group>> getGroupsByOwnerLive(int ownerId);

    @Query("UPDATE groups SET ownerId = :newOwnerId WHERE groupId = :groupId")
    void transferOwnership(int groupId, int newOwnerId);

    // ===================== Gestion des codes d'invitation =====================

    @Query("SELECT * FROM groups WHERE inviteCode = :inviteCode AND isActive = 1")
    Group getGroupByInviteCode(String inviteCode);

    @Query("UPDATE groups SET inviteCode = :newCode, inviteCodeExpiry = :expiry WHERE groupId = :groupId")
    void updateInviteCode(int groupId, String newCode, long expiry);

    @Query("SELECT COUNT(*) > 0 FROM groups WHERE inviteCode = :code AND isActive = 1 AND inviteCodeExpiry > :currentTime")
    boolean isInviteCodeValid(String code, long currentTime);

    // ===================== Gestion des types de groupes =====================

    @Query("SELECT * FROM groups WHERE groupType = :type AND isActive = 1")
    List<Group> getGroupsByType(Group.GroupType type);

    @Query("SELECT * FROM groups WHERE groupType = :type AND isActive = 1")
    LiveData<List<Group>> getGroupsByTypeLive(Group.GroupType type);

    // ===================== Gestion de l'accès public =====================

    @Query("SELECT * FROM groups WHERE allowPublicJoin = 1 AND isActive = 1")
    List<Group> getPublicGroups();

    @Query("SELECT * FROM groups WHERE allowPublicJoin = 1 AND isActive = 1")
    LiveData<List<Group>> getPublicGroupsLive();

    @Query("UPDATE groups SET allowPublicJoin = :allowPublic WHERE groupId = :groupId")
    void updatePublicJoinSetting(int groupId, boolean allowPublic);

    // ===================== Paramètres de partage =====================

    @Query("UPDATE groups SET shareRecipes = :share WHERE groupId = :groupId")
    void updateRecipeSharingSetting(int groupId, boolean share);

    @Query("UPDATE groups SET shareMealPlanning = :share WHERE groupId = :groupId")
    void updateMealPlanningSharingSetting(int groupId, boolean share);

    @Query("UPDATE groups SET shareShoppingLists = :share WHERE groupId = :groupId")
    void updateShoppingListSharingSetting(int groupId, boolean share);

    @Query("UPDATE groups SET shareTimers = :share WHERE groupId = :groupId")
    void updateTimerSharingSetting(int groupId, boolean share);

    // ===================== Recherche et filtrage =====================

    @Query("SELECT * FROM groups WHERE " +
           "(groupName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
           "AND isActive = 1")
    List<Group> searchGroups(String query);

    @Query("SELECT * FROM groups WHERE " +
           "(groupName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
           "AND isActive = 1")
    LiveData<List<Group>> searchGroupsLive(String query);

    @Query("SELECT * FROM groups WHERE " +
           "allowPublicJoin = 1 AND isActive = 1 " +
           "AND (groupName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')")
    List<Group> searchPublicGroups(String query);

    // ===================== Statistiques =====================

    @Query("SELECT COUNT(*) FROM groups WHERE isActive = 1")
    int getActiveGroupCount();

    @Query("SELECT COUNT(*) FROM groups WHERE ownerId = :ownerId AND isActive = 1")
    int getGroupCountByOwner(int ownerId);

    @Query("SELECT COUNT(*) FROM groups WHERE groupType = :type AND isActive = 1")
    int getGroupCountByType(Group.GroupType type);

    @Query("SELECT COUNT(*) FROM groups WHERE allowPublicJoin = 1 AND isActive = 1")
    int getPublicGroupCount();

    // ===================== Validation et contraintes =====================

    @Query("SELECT COUNT(*) > 0 FROM groups WHERE groupName = :name AND ownerId = :ownerId AND isActive = 1")
    boolean groupNameExistsForOwner(String name, int ownerId);

    @Query("SELECT COUNT(*) FROM groups WHERE ownerId = :ownerId AND isActive = 1")
    int getOwnedGroupCount(int ownerId);

    // ===================== Fonctionnalités partagées =====================

    @Query("SELECT * FROM groups WHERE shareRecipes = 1 AND isActive = 1")
    List<Group> getGroupsWithRecipeSharing();

    @Query("SELECT * FROM groups WHERE shareMealPlanning = 1 AND isActive = 1")
    List<Group> getGroupsWithMealPlanningSharing();

    @Query("SELECT * FROM groups WHERE shareShoppingLists = 1 AND isActive = 1")
    List<Group> getGroupsWithShoppingListSharing();

    @Query("SELECT * FROM groups WHERE shareTimers = 1 AND isActive = 1")
    List<Group> getGroupsWithTimerSharing();

    // ===================== Gestion de l'état =====================

    @Query("UPDATE groups SET isActive = :isActive WHERE groupId = :groupId")
    void setGroupActiveStatus(int groupId, boolean isActive);

    @Query("SELECT * FROM groups WHERE isActive = :isActive")
    List<Group> getGroupsByActiveStatus(boolean isActive);

    // ===================== Nettoyage =====================

    @Query("DELETE FROM groups WHERE isActive = 0 AND groupId NOT IN " +
           "(SELECT DISTINCT defaultGroupId FROM users WHERE defaultGroupId IS NOT NULL)")
    int cleanupInactiveGroups();

    @Query("UPDATE groups SET isActive = 0 WHERE inviteCodeExpiry < :currentTime AND allowPublicJoin = 0")
    int deactivateExpiredPrivateGroups(long currentTime);

    // ===================== Gestion des limites =====================

    @Query("UPDATE groups SET maxMembers = :maxMembers WHERE groupId = :groupId")
    void updateMemberLimit(int groupId, int maxMembers);

    @Query("SELECT maxMembers FROM groups WHERE groupId = :groupId")
    int getMemberLimit(int groupId);
}
